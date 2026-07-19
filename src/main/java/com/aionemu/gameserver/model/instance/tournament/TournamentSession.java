package com.aionemu.gameserver.model.instance.tournament;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class TournamentSession {
	private static final int FORMAT_VERSION = 1;
	private final int tournamentId;
	private final int matchmakerId;
	private final long lobbyUid;
	private final int bracketSize;
	private final Map<Integer, Team> teams = new LinkedHashMap<>();
	private final Map<Integer, Match> matches = new LinkedHashMap<>();
	private State state = State.REGISTERING;
	private long deadline;
	private int stateVersion;
	private TournamentBracket bracket;

	public TournamentSession(int tournamentId, int matchmakerId, long lobbyUid, int bracketSize, long deadline) {
		if (tournamentId <= 0 || matchmakerId <= 0 || lobbyUid <= 0 || bracketSize < 2
				|| Integer.bitCount(bracketSize) != 1 || deadline <= 0) {
			throw new IllegalArgumentException("Invalid tournament session");
		}
		this.tournamentId = tournamentId;
		this.matchmakerId = matchmakerId;
		this.lobbyUid = lobbyUid;
		this.bracketSize = bracketSize;
		this.deadline = deadline;
	}

	private TournamentSession(int tournamentId, int matchmakerId, long lobbyUid, int bracketSize, State state,
			long deadline, int stateVersion, TournamentBracket bracket) {
		this.tournamentId = tournamentId;
		this.matchmakerId = matchmakerId;
		this.lobbyUid = lobbyUid;
		this.bracketSize = bracketSize;
		this.state = state;
		this.deadline = deadline;
		this.stateVersion = stateVersion;
		this.bracket = bracket;
	}

	public synchronized boolean addTeam(Team team) {
		if (state != State.REGISTERING || teams.size() >= bracketSize || team == null || team.id() <= 0
				|| team.members().isEmpty() || teams.containsKey(team.id())) {
			return false;
		}
		teams.put(team.id(), team);
		stateVersion++;
		return true;
	}

	public synchronized void retainTeams(Collection<Integer> teamIds) {
		if (state != State.REGISTERING) {
			throw new IllegalStateException("Tournament registration is frozen");
		}
		if (teams.keySet().removeIf(teamId -> !teamIds.contains(teamId))) {
			stateVersion++;
		}
	}

	public synchronized void freeze(long nextDeadline) {
		if (state != State.REGISTERING || teams.size() < 2) {
			throw new IllegalStateException("Tournament requires at least two teams");
		}
		bracket = new TournamentBracket(bracketSize, new ArrayList<>(teams.keySet()));
		state = State.BETWEEN_ROUNDS;
		deadline = nextDeadline;
		stateVersion++;
	}

	public synchronized List<Match> createRound(long waitDeadline) {
		if (state != State.BETWEEN_ROUNDS || bracket == null || bracket.complete()) {
			throw new IllegalStateException("Tournament round cannot start");
		}
		List<Match> created = new ArrayList<>();
		for (TournamentBracket.Match bracketMatch : bracket.currentMatches()) {
			Match match = matches.get(bracketMatch.id());
			if (match == null) {
				match = new Match(bracketMatch.id(), bracketMatch.round(), bracketMatch.teamA(), bracketMatch.teamB());
				matches.put(match.id(), match);
			}
			match.state = MatchState.WAITING;
			match.deadline = waitDeadline;
			created.add(match.copy());
		}
		state = State.ROUND_ACTIVE;
		deadline = waitDeadline;
		stateVersion++;
		return List.copyOf(created);
	}

	public synchronized void bindStage(int matchId, long stageUid) {
		Match match = requiredMatch(matchId);
		if (stageUid <= 0 || match.stageUid != 0 && match.stageUid != stageUid) {
			throw new IllegalArgumentException("Invalid tournament stage binding");
		}
		match.stageUid = stageUid;
		stateVersion++;
	}

	public synchronized void startMatch(int matchId, long playDeadline) {
		Match match = requiredMatch(matchId);
		if (match.state != MatchState.WAITING) {
			return;
		}
		match.state = MatchState.PLAYING;
		match.deadline = playDeadline;
		deadline = Math.max(deadline, playDeadline);
		stateVersion++;
	}

	public synchronized void startOvertime(int matchId, long overtimeDeadline) {
		Match match = requiredMatch(matchId);
		if (match.state != MatchState.PLAYING) {
			return;
		}
		match.state = MatchState.OVERTIME;
		match.deadline = overtimeDeadline;
		deadline = Math.max(deadline, overtimeDeadline);
		stateVersion++;
	}

	public synchronized int addScore(int matchId, int teamId, int points) {
		Match match = requiredMatch(matchId);
		if (!match.state.active() || points <= 0) {
			return score(match, teamId);
		}
		if (teamId == match.teamA) {
			match.scoreA += points;
			match.killsA++;
		} else if (teamId == match.teamB) {
			match.scoreB += points;
			match.killsB++;
		} else {
			throw new IllegalArgumentException("Team is not in tournament match");
		}
		stateVersion++;
		return score(match, teamId);
	}

	public synchronized int advanceRelay(int matchId, int teamId) {
		Match match = requiredMatch(matchId);
		if (teamId == match.teamA) {
			stateVersion++;
			return ++match.activeA;
		}
		if (teamId == match.teamB) {
			stateVersion++;
			return ++match.activeB;
		}
		throw new IllegalArgumentException("Team is not in tournament match");
	}

	public synchronized boolean finishMatch(int matchId, int winnerId, long nextRoundDeadline) {
		Match match = requiredMatch(matchId);
		if (match.state == MatchState.FINISHED) {
			return false;
		}
		if (winnerId != match.teamA && winnerId != match.teamB) {
			throw new IllegalArgumentException("Winner is not in tournament match");
		}
		match.state = MatchState.FINISHED;
		match.winner = winnerId;
		match.deadline = 0;
		bracket.recordWinner(matchId, winnerId);
		stateVersion++;
		if (bracket.complete()) {
			state = State.COMPLETE;
			deadline = 0;
			return true;
		}
		boolean roundFinished = matches.values().stream()
				.filter(current -> current.round == match.round)
				.allMatch(current -> current.state == MatchState.FINISHED);
		if (roundFinished) {
			state = State.BETWEEN_ROUNDS;
			deadline = nextRoundDeadline;
			return true;
		}
		return false;
	}

	public synchronized String encode() {
		try {
			ByteArrayOutputStream bytes = new ByteArrayOutputStream();
			try (DataOutputStream output = new DataOutputStream(bytes)) {
				output.writeInt(FORMAT_VERSION);
				output.writeInt(tournamentId);
				output.writeInt(matchmakerId);
				output.writeLong(lobbyUid);
				output.writeInt(bracketSize);
				output.writeByte(state.ordinal());
				output.writeLong(deadline);
				output.writeInt(stateVersion);
				write(output, bracket == null ? "" : bracket.encode());
				output.writeInt(teams.size());
				for (Team team : teams.values()) {
					output.writeInt(team.id());
					output.writeInt(team.members().size());
					for (Member member : team.members()) {
						output.writeInt(member.playerId());
						write(output, member.name());
						output.writeInt(member.level());
						output.writeInt(member.classId());
						output.writeInt(member.order());
					}
				}
				output.writeInt(matches.size());
				for (Match match : matches.values()) {
					match.write(output);
				}
			}
			return Base64.getEncoder().encodeToString(bytes.toByteArray());
		} catch (IOException e) {
			throw new IllegalStateException("Failed to encode tournament session", e);
		}
	}

	public static TournamentSession decode(String encoded) {
		try (DataInputStream input = new DataInputStream(new ByteArrayInputStream(Base64.getDecoder().decode(encoded)))) {
			if (input.readInt() != FORMAT_VERSION) {
				throw new IOException("Unsupported tournament session version");
			}
			int tournamentId = input.readInt();
			int matchmakerId = input.readInt();
			long lobbyUid = input.readLong();
			int bracketSize = input.readInt();
			int stateId = input.readUnsignedByte();
			long deadline = input.readLong();
			int stateVersion = input.readInt();
			String bracketData = read(input);
			if (stateId >= State.values().length) {
				throw new IOException("Invalid tournament session state");
			}
			TournamentSession session = new TournamentSession(tournamentId, matchmakerId, lobbyUid, bracketSize,
					State.values()[stateId], deadline, stateVersion,
					bracketData.isEmpty() ? null : TournamentBracket.decode(bracketData));
			int teamCount = input.readInt();
			if (teamCount < 0 || teamCount > bracketSize) {
				throw new IOException("Invalid tournament team count");
			}
			for (int i = 0; i < teamCount; i++) {
				int teamId = input.readInt();
				int memberCount = input.readInt();
				if (memberCount < 1 || memberCount > 6) {
					throw new IOException("Invalid tournament member count");
				}
				List<Member> members = new ArrayList<>(memberCount);
				for (int member = 0; member < memberCount; member++) {
					members.add(new Member(input.readInt(), read(input), input.readInt(), input.readInt(), input.readInt()));
				}
				session.teams.put(teamId, new Team(teamId, members));
			}
			int matchCount = input.readInt();
			if (matchCount < 0 || matchCount > bracketSize - 1) {
				throw new IOException("Invalid tournament match count");
			}
			for (int i = 0; i < matchCount; i++) {
				Match match = Match.read(input);
				session.matches.put(match.id, match);
			}
			if (input.available() != 0) {
				throw new IOException("Trailing tournament session bytes");
			}
			return session;
		} catch (IOException | IllegalArgumentException e) {
			throw new IllegalStateException("Failed to decode tournament session", e);
		}
	}

	public int tournamentId() { return tournamentId; }
	public int matchmakerId() { return matchmakerId; }
	public long lobbyUid() { return lobbyUid; }
	public int bracketSize() { return bracketSize; }
	public synchronized State state() { return state; }
	public synchronized long deadline() { return deadline; }
	public synchronized int stateVersion() { return stateVersion; }
	public synchronized int round() { return bracket == null ? 0 : bracket.round(); }
	public synchronized int roundCount() { return bracket == null ? Integer.numberOfTrailingZeros(bracketSize) : bracket.roundCount(); }
	public synchronized int champion() { return bracket == null ? 0 : bracket.champion(); }
	public synchronized int placement(int teamId) { return bracket == null ? 0 : bracket.placement(teamId); }
	public synchronized List<Team> teams() { return List.copyOf(teams.values()); }
	public synchronized Team team(int teamId) { return teams.get(teamId); }
	public synchronized Match match(int matchId) { Match match = matches.get(matchId); return match == null ? null : match.copy(); }
	public synchronized List<Match> matches() { return matches.values().stream().map(Match::copy).toList(); }

	private Match requiredMatch(int matchId) {
		Match match = matches.get(matchId);
		if (match == null) {
			throw new IllegalArgumentException("Unknown tournament match " + matchId);
		}
		return match;
	}

	private static int score(Match match, int teamId) {
		if (teamId == match.teamA) return match.scoreA;
		if (teamId == match.teamB) return match.scoreB;
		throw new IllegalArgumentException("Team is not in tournament match");
	}

	private static void write(DataOutputStream output, String value) throws IOException {
		byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
		output.writeInt(bytes.length);
		output.write(bytes);
	}

	private static String read(DataInputStream input) throws IOException {
		int length = input.readInt();
		if (length < 0 || length > 16 * 1024 * 1024) {
			throw new IOException("Invalid tournament string length");
		}
		byte[] bytes = input.readNBytes(length);
		if (bytes.length != length) {
			throw new IOException("Truncated tournament string");
		}
		return new String(bytes, StandardCharsets.UTF_8);
	}

	public enum State { REGISTERING, BETWEEN_ROUNDS, ROUND_ACTIVE, COMPLETE }
	public enum MatchState {
		WAITING, PLAYING, OVERTIME, FINISHED;
		public boolean active() { return this == PLAYING || this == OVERTIME; }
	}

	public record Team(int id, List<Member> members) {
		public Team {
			members = List.copyOf(members);
		}
	}

	public record Member(int playerId, String name, int level, int classId, int order) {
	}

	public static final class Match {
		private final int id;
		private final int round;
		private final int teamA;
		private final int teamB;
		private MatchState state = MatchState.WAITING;
		private long stageUid;
		private int scoreA;
		private int scoreB;
		private int killsA;
		private int killsB;
		private int activeA;
		private int activeB;
		private int winner;
		private long deadline;

		private Match(int id, int round, int teamA, int teamB) {
			this.id = id;
			this.round = round;
			this.teamA = teamA;
			this.teamB = teamB;
		}

		private Match copy() {
			Match copy = new Match(id, round, teamA, teamB);
			copy.state = state;
			copy.stageUid = stageUid;
			copy.scoreA = scoreA;
			copy.scoreB = scoreB;
			copy.killsA = killsA;
			copy.killsB = killsB;
			copy.activeA = activeA;
			copy.activeB = activeB;
			copy.winner = winner;
			copy.deadline = deadline;
			return copy;
		}

		private void write(DataOutputStream output) throws IOException {
			output.writeInt(id); output.writeInt(round); output.writeInt(teamA); output.writeInt(teamB);
			output.writeByte(state.ordinal()); output.writeLong(stageUid); output.writeInt(scoreA); output.writeInt(scoreB);
			output.writeInt(killsA); output.writeInt(killsB); output.writeInt(activeA); output.writeInt(activeB);
			output.writeInt(winner); output.writeLong(deadline);
		}

		private static Match read(DataInputStream input) throws IOException {
			Match match = new Match(input.readInt(), input.readInt(), input.readInt(), input.readInt());
			int stateId = input.readUnsignedByte();
			if (stateId >= MatchState.values().length) throw new IOException("Invalid tournament match state");
			match.state = MatchState.values()[stateId]; match.stageUid = input.readLong();
			match.scoreA = input.readInt(); match.scoreB = input.readInt(); match.killsA = input.readInt(); match.killsB = input.readInt();
			match.activeA = input.readInt(); match.activeB = input.readInt(); match.winner = input.readInt(); match.deadline = input.readLong();
			return match;
		}

		public int id() { return id; }
		public int round() { return round; }
		public int teamA() { return teamA; }
		public int teamB() { return teamB; }
		public MatchState state() { return state; }
		public long stageUid() { return stageUid; }
		public int scoreA() { return scoreA; }
		public int scoreB() { return scoreB; }
		public int killsA() { return killsA; }
		public int killsB() { return killsB; }
		public int activeA() { return activeA; }
		public int activeB() { return activeB; }
		public int winner() { return winner; }
		public long deadline() { return deadline; }
	}
}
