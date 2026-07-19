package com.aionemu.gameserver.model.autogroup;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.aionemu.gameserver.model.PlayerClass;
import com.aionemu.gameserver.model.Race;

/** 真端自动匹配的纯内存选人器。 */
public final class RetailMatchPlanner {
	private RetailMatchPlanner() {
	}

	public static Plan draft(MatchDefinition definition, List<Party> queue, List<Assignment> existing,
			long now, boolean requireReady) {
		if (queue.isEmpty()) {
			return Plan.EMPTY;
		}
		List<Party> ordered = queue.stream().sorted(Comparator.comparingLong(Party::sequence)).toList();
		int poolSize = candidatePoolSize(definition, ordered, now);
		List<Party> candidates = new ArrayList<>(ordered.subList(0, poolSize));
		candidates.sort(Comparator.comparingInt((Party party) -> priority(definition, party))
				.thenComparingLong(Party::sequence));

		List<AGPlayer> composition = new ArrayList<>();
		for (Assignment assignment : existing) {
			composition.add(assignment.member().asAGPlayer(assignment.side()));
		}
		List<Assignment> assignments = new ArrayList<>();
		List<Party> selected = new ArrayList<>();
		for (Party party : candidates) {
			byte side = selectSide(definition, composition, party);
			if (side < 0) {
				continue;
			}
			for (Member member : party.members()) {
				composition.add(member.asAGPlayer(side));
				assignments.add(new Assignment(member, side));
			}
			selected.add(party);
			if (requireReady && definition.isCompositionReady(composition, now - ordered.getFirst().registeredAt())) {
				return new Plan(selected, assignments);
			}
		}
		return requireReady ? Plan.EMPTY : new Plan(selected, assignments);
	}

	private static int candidatePoolSize(MatchDefinition definition, List<Party> queue, long now) {
		long interval = definition.getShuffleIntervalMillis();
		int limit = definition.getShuffleLimitSize();
		if (interval <= 0 || limit <= 0) {
			return queue.size();
		}
		long elapsed = Math.max(0, now - queue.getFirst().registeredAt());
		long expansions = elapsed / interval;
		int base = Math.max(definition.getPlayerSize(), definition.getShuffleMinimum());
		long expanded = base + expansions * Math.max(1, definition.getShuffleMinimum());
		return Math.min(queue.size(), (int) Math.min(limit, expanded));
	}

	private static int priority(MatchDefinition definition, Party party) {
		if (party.requestType().isGroupEntry() && party.members().size() >= definition.getPlayersPerSide()) {
			return 0;
		}
		if (party.requestType().isGroupEntry()) {
			return 1;
		}
		return party.requestType().isFastGroupEntry() ? 3 : 2;
	}

	private static byte selectSide(MatchDefinition definition, List<AGPlayer> composition, Party party) {
		if (definition.getMatchSides() == 1) {
			return fits(definition, composition, party, (byte) 0) ? (byte) 0 : -1;
		}
		if (!definition.isRaceFree() && definition.getMatchSides() == 2) {
			Race race = party.members().getFirst().race();
			if ((race != Race.ELYOS && race != Race.ASMODIANS)
					|| party.members().stream().anyMatch(member -> member.race() != race)) {
				return -1;
			}
			byte side = (byte) race.getRaceId();
			return fits(definition, composition, party, side) ? side : -1;
		}
		Map<Byte, Integer> sizes = new HashMap<>();
		for (byte side = 0; side < definition.getMatchSides(); side++) {
			byte candidate = side;
			sizes.put(side, (int) composition.stream().filter(player -> player.getMatchSide() == candidate).count());
		}
		return sizes.entrySet().stream().sorted(Map.Entry.<Byte, Integer>comparingByValue()
				.thenComparing(Map.Entry.comparingByKey())).map(Map.Entry::getKey)
				.filter(side -> fits(definition, composition, party, side)).findFirst().orElse((byte) -1);
	}

	private static boolean fits(MatchDefinition definition, List<AGPlayer> composition, Party party, byte side) {
		List<AGPlayer> sidePlayers = composition.stream().filter(player -> player.getMatchSide() == side)
				.collect(java.util.stream.Collectors.toCollection(ArrayList::new));
		if (sidePlayers.size() + party.members().size() > definition.getPlayersPerSide()) {
			return false;
		}
		for (Member member : party.members()) {
			if (!definition.canAdd(member.playerClass(), sidePlayers, 1)) {
				return false;
			}
			sidePlayers.add(member.asAGPlayer(side));
		}
		return true;
	}

	public record Member(int playerId, String name, PlayerClass playerClass, Race race) {
		private AGPlayer asAGPlayer(byte side) {
			return new AGPlayer(playerId, race, playerClass, name, side, false, true, false);
		}
	}

	public record Party(long sequence, long registeredAt, EntryRequestType requestType, int teamId,
			List<Member> members) {
		public Party {
			members = List.copyOf(members);
		}
	}

	public record Assignment(Member member, byte side) {
	}

	public record Plan(List<Party> parties, List<Assignment> assignments) {
		private static final Plan EMPTY = new Plan(List.of(), List.of());

		public Plan {
			parties = List.copyOf(parties);
			assignments = List.copyOf(assignments);
		}

		public boolean isEmpty() {
			return assignments.isEmpty();
		}
	}
}
