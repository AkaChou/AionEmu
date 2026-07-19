package com.aionemu.gameserver.ai;

import com.aionemu.boot.i18n.I18n;
import com.aionemu.commons.utils.Rnd;
import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.dataholders.RetailAiData.GroupControlArea;
import com.aionemu.gameserver.dataholders.RetailAiData.GroupController;
import com.aionemu.gameserver.dataholders.RetailAiData.LocationAliasPoint;
import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.gameobjects.player.RequestResponseHandler;
import com.aionemu.gameserver.model.team2.TeamType;
import com.aionemu.gameserver.model.team2.alliance.PlayerAlliance;
import com.aionemu.gameserver.model.team2.alliance.PlayerAllianceService;
import com.aionemu.gameserver.model.team2.group.PlayerGroup;
import com.aionemu.gameserver.model.team2.group.PlayerGroupService;
import com.aionemu.gameserver.model.team2.league.League;
import com.aionemu.gameserver.model.team2.league.LeagueService;
import com.aionemu.gameserver.network.aion.serverpackets.SM_QUESTION_WINDOW;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.services.teleport.TeleportService2;
import com.aionemu.gameserver.utils.MathUtil;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.world.WorldMapInstance;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.Future;

/** 真端 InAreaObjCtrl 团队区域控制。 */
@Slf4j
public final class RetailGroupControlEngine {

	private static final long REFRESH_INTERVAL = 10_000;
	private static final long ASK_COOLDOWN_SECONDS = 600;
	private static final String STATE_PREFIX = "retail.group_control.";

	private RetailGroupControlEngine() {
	}

	public static boolean supports(int worldId, String prefix) {
		return DataManager.RETAIL_AI_DATA != null && prefix != null
			&& !DataManager.RETAIL_AI_DATA.findGroupControlAreas(worldId, prefix).isEmpty();
	}

	public static void initialize(WorldMapInstance instance) {
		if (DataManager.RETAIL_AI_DATA == null || DataManager.RETAIL_AI_DATA.getGroupControllers(instance.getMapId()).isEmpty()) {
			return;
		}
		instance.getOrCreateTransientState(InstanceState.class, () -> createState(instance));
	}

	public static boolean setAreaEnabled(WorldMapInstance instance, String prefix, boolean enabled) {
		if (!supports(instance.getMapId(), prefix)) {
			return false;
		}
		initialize(instance);
		InstanceState state = instance.getTransientState(InstanceState.class);
		if (state != null) {
			for (ControllerState controller : state.controllers) {
				if (matchesPrefix(controller.definition.area1(), prefix)) {
					controller.enabled = enabled;
					instance.getRuntimeState().put(enabledKey(controller.definition.id()), enabled);
				}
			}
		}
		return true;
	}

	public static void clear(WorldMapInstance instance) {
		InstanceState state = instance.removeTransientState(InstanceState.class);
		if (state != null) {
			state.close();
		}
		instance.getRuntimeState().removePrefix(STATE_PREFIX);
	}

	private static InstanceState createState(WorldMapInstance instance) {
		Map<String, GroupControlArea> areas = new HashMap<>();
		for (GroupControlArea area : DataManager.RETAIL_AI_DATA.findGroupControlAreas(instance.getMapId(), "")) {
			areas.put(area.name().toLowerCase(Locale.ROOT), area);
		}
		List<ControllerState> controllers = DataManager.RETAIL_AI_DATA.getGroupControllers(instance.getMapId()).stream()
			.map(definition -> new ControllerState(definition,
				requireArea(areas, definition.area1()), requireArea(areas, definition.area2()),
				instance.getRuntimeState().getBoolean(enabledKey(definition.id()), true)))
			.toList();
		InstanceState state = new InstanceState(instance, controllers);
		state.task = GameThreadPoolServices.threadPoolManager().scheduleAtFixedRate(
			() -> refreshSafely(state), REFRESH_INTERVAL, REFRESH_INTERVAL);
		return state;
	}

	private static GroupControlArea requireArea(Map<String, GroupControlArea> areas, String name) {
		GroupControlArea area = areas.get(name.toLowerCase(Locale.ROOT));
		if (area == null) {
			throw new IllegalStateException("Missing GROUPCTRL area: " + name);
		}
		return area;
	}

	private static void refreshSafely(InstanceState state) {
		try {
			for (ControllerState controller : state.controllers) {
				refresh(state.instance, controller);
			}
		} catch (Throwable t) {
			log.error(I18n.get("log.retail_group.refresh_failed", state.instance.getMapId(),
				state.instance.getInstanceId()), t);
		}
	}

	private static void refresh(WorldMapInstance instance, ControllerState state) {
		synchronized (state) {
			if (state.closed) {
				return;
			}
			state.pruneTeams();
			Set<Player> currentPlayers = state.members();
			Map<Integer, Player> players = new HashMap<>();
			currentPlayers.forEach(player -> players.put(player.getObjectId(), player));
			Set<Integer> current = new HashSet<>(players.keySet());
			Set<Integer> insideArea1 = new HashSet<>();
			Set<Integer> insideArea2 = new HashSet<>();
			for (Player player : instance.getPlayersInside()) {
				players.put(player.getObjectId(), player);
				if (!matchesRace(state.definition, player)) {
					continue;
				}
				if (state.area2.area().isInside3D(player.getX(), player.getY(), player.getZ())) {
					insideArea2.add(player.getObjectId());
				}
				if (state.enabled && (current.contains(player.getObjectId()) || !player.isInTeam())
						&& state.area1.area().isInside3D(player.getX(), player.getY(), player.getZ())) {
					insideArea1.add(player.getObjectId());
				}
			}
			MembershipDelta delta = categorize(insideArea1, insideArea2, current, state.pendingRemoval);
			state.pendingRemoval = delta.pendingLeaves;
			for (int objectId : delta.confirmedLeaves) {
				Player player = players.get(objectId);
				if (player != null) {
					state.remove(player, true);
				}
			}
			for (int objectId : delta.entrants) {
				Player player = players.get(objectId);
				if (player != null) {
					if (asksBeforeJoining(state.definition.type())) {
						askToJoin(instance, state, player);
					} else {
						state.add(player);
					}
				}
			}
		}
	}

	static MembershipDelta categorize(Set<Integer> area1, Set<Integer> area2, Set<Integer> current,
			Set<Integer> pending) {
		Set<Integer> entrants = new HashSet<>(area1);
		entrants.removeAll(current);
		Set<Integer> outside = new HashSet<>(current);
		outside.removeAll(area2);
		Set<Integer> confirmed = new HashSet<>(outside);
		confirmed.retainAll(pending);
		Set<Integer> nextPending = new HashSet<>(outside);
		nextPending.removeAll(confirmed);
		return new MembershipDelta(Set.copyOf(entrants), Set.copyOf(confirmed), Set.copyOf(nextPending));
	}

	private static void askToJoin(WorldMapInstance instance, ControllerState state, Player player) {
		long now = System.currentTimeMillis() / 1000;
		if (state.askedUntil.getOrDefault(player, 0L) > now) {
			return;
		}
		state.askedUntil.put(player, now + ASK_COOLDOWN_SECONDS);
		RequestResponseHandler handler = new RequestResponseHandler(player) {
			@Override
			public void acceptRequest(Creature requester, Player responder) {
				synchronized (state) {
					if (!state.closed && responder.getPosition() != null
							&& responder.getPosition().getWorldMapInstance() == instance
							&& matchesRace(state.definition, responder)) {
						state.add(responder);
					}
				}
			}

			@Override
			public void denyRequest(Creature requester, Player responder) {
			}
		};
		if (player.getResponseRequester().putRequest(
				SM_QUESTION_WINDOW.STR_ASK_INVADE_DIRECT_PORTAL_DEFENSE_FORCE, handler)) {
			PacketSendUtility.sendPacket(player, new SM_QUESTION_WINDOW(
				SM_QUESTION_WINDOW.STR_ASK_INVADE_DIRECT_PORTAL_DEFENSE_FORCE, 0, 0));
		}
	}

	private static boolean asksBeforeJoining(int type) {
		return type == 3 || type == 4 || type == 6;
	}

	private static boolean matchesRace(GroupController definition, Player player) {
		return definition.race().equalsIgnoreCase("pc_light")
			? player.getRace() == Race.ELYOS : player.getRace() == Race.ASMODIANS;
	}

	private static TeamType teamType(int controlTargetType) {
		return switch (controlTargetType) {
			case 0 -> TeamType.IN_AREA_DEFAULT;
			case 1 -> TeamType.IN_AREA_TARGET_1;
			case 2 -> TeamType.IN_AREA_TARGET_2;
			case 3 -> TeamType.IN_AREA_TARGET_3;
			case 4 -> TeamType.IN_AREA_TARGET_4;
			default -> throw new IllegalArgumentException("Unsupported GROUPCTRL target type: " + controlTargetType);
		};
	}

	private static boolean matchesPrefix(String name, String prefix) {
		return name.regionMatches(true, 0, prefix, 0, prefix.length());
	}

	private static String enabledKey(int controllerId) {
		return STATE_PREFIX + controllerId + ".enabled";
	}

	static record MembershipDelta(Set<Integer> entrants, Set<Integer> confirmedLeaves, Set<Integer> pendingLeaves) {
	}

	private static final class InstanceState {
		private final WorldMapInstance instance;
		private final List<ControllerState> controllers;
		private Future<?> task;

		private InstanceState(WorldMapInstance instance, List<ControllerState> controllers) {
			this.instance = instance;
			this.controllers = controllers;
		}

		private void close() {
			if (task != null) {
				task.cancel(false);
			}
			controllers.forEach(ControllerState::close);
		}
	}

	private static final class ControllerState {
		private final GroupController definition;
		private final GroupControlArea area1;
		private final GroupControlArea area2;
		private final List<PlayerGroup> groups = new ArrayList<>();
		private final List<PlayerAlliance> alliances = new ArrayList<>();
		private final List<League> leagues = new ArrayList<>();
		private final Map<Player, Long> askedUntil = new WeakHashMap<>();
		private volatile boolean enabled;
		private boolean closed;
		private Set<Integer> pendingRemoval = Set.of();

		private ControllerState(GroupController definition, GroupControlArea area1, GroupControlArea area2,
				boolean enabled) {
			this.definition = definition;
			this.area1 = area1;
			this.area2 = area2;
			this.enabled = enabled;
		}

		private Set<Player> members() {
			Set<Player> members = new HashSet<>();
			groups.forEach(group -> members.addAll(group.getMembers()));
			alliances.forEach(alliance -> members.addAll(alliance.getMembers()));
			return members;
		}

		private void add(Player player) {
			if (closed || player.isInTeam()) {
				return;
			}
			TeamType teamType = teamType(definition.controlTargetType());
			switch (definition.type()) {
				case 1, 3 -> {
					PlayerGroup group = groups.stream().filter(existing -> !existing.isFull()).findFirst().orElse(null);
					if (group == null) {
						groups.add(PlayerGroupService.createGroup(player, teamType));
					} else {
						PlayerGroupService.addPlayer(group, player);
					}
				}
				case 2, 4 -> {
					PlayerAlliance alliance = alliances.stream().filter(existing -> !existing.isFull()).findFirst().orElse(null);
					if (alliance == null) {
						alliances.add(PlayerAllianceService.createAlliance(player, teamType));
					} else {
						PlayerAllianceService.addPlayer(alliance, player);
					}
				}
				case 5, 6 -> addToLeague(player, teamType);
				default -> throw new IllegalStateException("Unsupported GROUPCTRL type: " + definition.type());
			}
		}

		private void addToLeague(Player player, TeamType teamType) {
			League league = leagues.stream().filter(existing -> !existing.isFull()
				|| existing.getMembers().stream().anyMatch(alliance -> !alliance.isFull())).findFirst().orElse(null);
			PlayerAlliance alliance = league == null ? null : league.getMembers().stream()
				.filter(existing -> !existing.isFull()).findFirst().orElse(null);
			if (alliance != null) {
				PlayerAllianceService.addPlayer(alliance, player);
				return;
			}
			alliance = PlayerAllianceService.createAlliance(player, teamType);
			alliances.add(alliance);
			if (league == null) {
				leagues.add(LeagueService.createLeague(player, player));
			} else {
				LeagueService.addAlliance(league, alliance);
			}
		}

		private void remove(Player player, boolean transfer) {
			int messageId = switch (definition.type()) {
				case 1 -> 1403202;
				case 2 -> 1403203;
				case 5, 6 -> 1403229;
				default -> 0;
			};
			if (transfer && messageId != 0) {
				PacketSendUtility.sendPacket(player, new SM_SYSTEM_MESSAGE(messageId));
			}
			if (player.getPlayerGroup2() != null && groups.contains(player.getPlayerGroup2())) {
				PlayerGroupService.removePlayer(player);
			} else if (player.getPlayerAlliance2() != null && alliances.contains(player.getPlayerAlliance2())) {
				PlayerAllianceService.removePlayer(player);
			}
			pruneTeams();
			if (transfer && definition.exitWorldId() > 0 && !definition.exitAlias().isBlank()) {
				List<LocationAliasPoint> points = DataManager.RETAIL_AI_DATA.findLocationAlias(
					definition.exitWorldId(), definition.exitAlias());
				if (points == null || points.isEmpty()) {
					log.error(I18n.get("log.retail_group.exit_alias_missing", definition.exitWorldId(), definition.exitAlias()));
					return;
				}
				LocationAliasPoint point = points.get(Rnd.get(points.size()));
				TeleportService2.teleportTo(player, definition.exitWorldId(), point.x(), point.y(), point.z(),
					MathUtil.convertDegreeToHeading(point.direction()));
			}
		}

		private void pruneTeams() {
			groups.removeIf(group -> group.size() == 0);
			alliances.removeIf(alliance -> alliance.size() == 0);
			leagues.removeIf(league -> league.size() == 0);
		}

		private void close() {
			synchronized (this) {
				closed = true;
				for (Player player : new ArrayList<>(members())) {
					remove(player, false);
				}
				pendingRemoval = Set.of();
				askedUntil.clear();
			}
		}
	}
}
