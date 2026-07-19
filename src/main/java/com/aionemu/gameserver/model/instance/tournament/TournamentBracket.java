package com.aionemu.gameserver.model.instance.tournament;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

public final class TournamentBracket {
	private final int size;
	private int round;
	private int[] slots;
	private int[] winners;
	private final List<Result> results = new ArrayList<>();

	public TournamentBracket(int size, List<Integer> teamIds) {
		if (size < 2 || Integer.bitCount(size) != 1 || teamIds.size() < 2 || teamIds.size() > size
				|| teamIds.stream().anyMatch(id -> id == null || id <= 0) || teamIds.stream().distinct().count() != teamIds.size()) {
			throw new IllegalArgumentException("Invalid tournament bracket");
		}
		this.size = size;
		this.slots = new int[size];
		for (int i = 0; i < teamIds.size(); i++) {
			slots[i] = teamIds.get(i);
		}
		this.winners = new int[size / 2];
		seedByes();
	}

	private TournamentBracket(int size, int round, int[] slots, int[] winners, List<Result> results) {
		this.size = size;
		this.round = round;
		this.slots = slots;
		this.winners = winners;
		this.results.addAll(results);
	}

	public int size() {
		return size;
	}

	public int round() {
		return round;
	}

	public int roundCount() {
		return Integer.numberOfTrailingZeros(size);
	}

	public List<Match> currentMatches() {
		List<Match> matches = new ArrayList<>();
		for (int pair = 0; pair < winners.length; pair++) {
			int teamA = slots[pair * 2];
			int teamB = slots[pair * 2 + 1];
			if (teamA != 0 && teamB != 0 && winners[pair] == 0) {
				matches.add(new Match(matchId(round, pair), round, pair, teamA, teamB));
			}
		}
		return List.copyOf(matches);
	}

	public List<Result> results() {
		return List.copyOf(results);
	}

	public void recordWinner(int matchId, int teamId) {
		int matchRound = matchId >>> 16;
		int pair = (matchId & 0xffff) - 1;
		if (matchRound != round || pair < 0 || pair >= winners.length || winners[pair] != 0) {
			throw new IllegalArgumentException("Unknown or completed tournament match " + matchId);
		}
		int teamA = slots[pair * 2];
		int teamB = slots[pair * 2 + 1];
		if (teamA == 0 || teamB == 0 || teamId != teamA && teamId != teamB) {
			throw new IllegalArgumentException("Team " + teamId + " is not in match " + matchId);
		}
		winners[pair] = teamId;
		results.add(new Result(matchId, round, pair, teamA, teamB, teamId));
		advanceIfComplete();
	}

	public boolean complete() {
		return slots.length == 1;
	}

	public int champion() {
		return complete() ? slots[0] : 0;
	}

	public int placement(int teamId) {
		if (!complete() || teamId <= 0) {
			return 0;
		}
		if (slots[0] == teamId) {
			return 1;
		}
		for (Result result : results) {
			if ((result.teamA() == teamId || result.teamB() == teamId) && result.winner() != teamId) {
				return 1 + (1 << Math.max(0, roundCount() - result.round() - 1));
			}
		}
		return 0;
	}

	public String encode() {
		try {
			ByteArrayOutputStream bytes = new ByteArrayOutputStream();
			try (DataOutputStream output = new DataOutputStream(bytes)) {
				output.writeInt(size);
				output.writeInt(round);
				writeInts(output, slots);
				writeInts(output, winners);
				output.writeInt(results.size());
				for (Result result : results) {
					output.writeInt(result.matchId());
					output.writeInt(result.round());
					output.writeInt(result.pair());
					output.writeInt(result.teamA());
					output.writeInt(result.teamB());
					output.writeInt(result.winner());
				}
			}
			return Base64.getEncoder().encodeToString(bytes.toByteArray());
		} catch (IOException e) {
			throw new IllegalStateException("Failed to encode tournament bracket", e);
		}
	}

	public static TournamentBracket decode(String encoded) {
		try (DataInputStream input = new DataInputStream(new ByteArrayInputStream(Base64.getDecoder().decode(encoded)))) {
			int size = input.readInt();
			int round = input.readInt();
			int[] slots = readInts(input, -1);
			int expectedWinners = slots.length == 1 ? 0 : slots.length / 2;
			int[] winners = readInts(input, expectedWinners);
			int resultCount = input.readInt();
			if (size < 2 || Integer.bitCount(size) != 1 || round < 0 || slots.length != size >> round
					|| Integer.bitCount(slots.length) != 1 || resultCount < 0 || resultCount > size - 1) {
				throw new IOException("Invalid tournament bracket header");
			}
			List<Result> results = new ArrayList<>(resultCount);
			for (int i = 0; i < resultCount; i++) {
				results.add(new Result(input.readInt(), input.readInt(), input.readInt(), input.readInt(), input.readInt(), input.readInt()));
			}
			if (input.available() != 0) {
				throw new IOException("Trailing tournament bracket bytes");
			}
			return new TournamentBracket(size, round, slots, winners, results);
		} catch (IOException | IllegalArgumentException e) {
			throw new IllegalStateException("Failed to decode tournament bracket", e);
		}
	}

	private void seedByes() {
		for (int pair = 0; pair < winners.length; pair++) {
			int teamA = slots[pair * 2];
			int teamB = slots[pair * 2 + 1];
			if (teamA == 0 ^ teamB == 0) {
				winners[pair] = teamA == 0 ? teamB : teamA;
			}
		}
		advanceIfComplete();
	}

	private void advanceIfComplete() {
		while (slots.length > 1 && currentMatches().isEmpty()) {
			int[] next = winners;
			if (next.length == 1) {
				slots = next;
				winners = new int[0];
				round++;
				return;
			}
			slots = next;
			winners = new int[next.length / 2];
			round++;
			seedByes();
		}
	}

	private static int matchId(int round, int pair) {
		return round << 16 | pair + 1;
	}

	private static void writeInts(DataOutputStream output, int[] values) throws IOException {
		output.writeInt(values.length);
		for (int value : values) {
			output.writeInt(value);
		}
	}

	private static int[] readInts(DataInputStream input, int expectedLength) throws IOException {
		int length = input.readInt();
		if (length < 0 || length > 32 || expectedLength >= 0 && length != expectedLength) {
			throw new IOException("Invalid tournament bracket array length " + length);
		}
		int[] values = new int[length];
		for (int i = 0; i < length; i++) {
			values[i] = input.readInt();
		}
		return values;
	}

	public record Match(int id, int round, int pair, int teamA, int teamB) {
	}

	public record Result(int matchId, int round, int pair, int teamA, int teamB, int winner) {
	}
}
