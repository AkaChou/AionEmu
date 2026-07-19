package com.aionemu.gameserver.model.instance.tournament;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class TournamentScore {
	private static final int LOBBY_PAYLOAD_SIZE = 7_788;
	private static final int STAGE_PAYLOAD_SIZE = 40;
	private final byte[] payload;

	private TournamentScore(byte[] payload) {
		this.payload = payload;
	}

	public static TournamentScore lobby(TournamentSession session, int packetType) {
		List<Participant> participants = participants(session);
		if (participants.size() > 96) {
			throw new IllegalStateException("Tournament lobby supports at most 96 participants");
		}
		ByteBuffer buffer = ByteBuffer.allocate(LOBBY_PAYLOAD_SIZE).order(ByteOrder.LITTLE_ENDIAN);
		buffer.putInt(packetType);
		buffer.putInt(session.roundCount());
		buffer.putInt(participants.size());
		for (Participant participant : participants) {
			buffer.putInt(participant.playerId());
			putString(buffer, participant.name(), 52);
			buffer.putInt(participant.teamId());
			buffer.putShort((short) participant.level());
			buffer.putInt(participant.classId());
			buffer.putInt(participant.order());
			buffer.put(participant.side());
			buffer.put(participant.results());
		}
		return new TournamentScore(buffer.array());
	}

	public static TournamentScore stage(TournamentSession session, int matchId, int packetType) {
		TournamentSession.Match match = session.match(matchId);
		if (match == null) {
			throw new IllegalArgumentException("Unknown tournament match " + matchId);
		}
		ByteBuffer buffer = ByteBuffer.allocate(STAGE_PAYLOAD_SIZE).order(ByteOrder.LITTLE_ENDIAN);
		buffer.putInt(packetType);
		buffer.putInt(match.teamA());
		buffer.putInt(match.scoreA());
		buffer.putInt(match.teamB());
		buffer.putInt(match.scoreB());
		buffer.putInt(match.activeA());
		buffer.putInt(match.activeB());
		buffer.putInt(match.winner());
		buffer.putInt(match.round());
		buffer.putInt(match.state().ordinal());
		return new TournamentScore(buffer.array());
	}

	public byte[] payload() {
		return payload.clone();
	}

	private static List<Participant> participants(TournamentSession session) {
		List<Participant> participants = new ArrayList<>();
		int order = 0;
		for (TournamentSession.Team team : session.teams()) {
			byte side = side(session, team.id());
			byte[] results = results(session, team.id());
			for (TournamentSession.Member member : team.members()) {
				participants.add(new Participant(member.playerId(), member.name(), team.id(), member.level(), member.classId(),
						order++, side, results));
			}
		}
		participants.sort(Comparator.comparingInt(Participant::order));
		return participants;
	}

	private static byte side(TournamentSession session, int teamId) {
		for (TournamentSession.Match match : session.matches()) {
			if (match.state() != TournamentSession.MatchState.FINISHED) {
				if (match.teamA() == teamId) return 0;
				if (match.teamB() == teamId) return 1;
			}
		}
		return 0;
	}

	private static byte[] results(TournamentSession session, int teamId) {
		byte[] values = new byte[10];
		for (TournamentSession.Match match : session.matches()) {
			if (match.state() == TournamentSession.MatchState.FINISHED && (match.teamA() == teamId || match.teamB() == teamId)
					&& match.round() < values.length) {
				values[match.round()] = (byte) (match.winner() == teamId ? 1 : 2);
			}
		}
		return values;
	}

	private static void putString(ByteBuffer buffer, String value, int bytes) {
		int chars = Math.min(value == null ? 0 : value.length(), bytes / 2);
		for (int i = 0; i < chars; i++) {
			buffer.putChar(value.charAt(i));
		}
		for (int i = chars * 2; i < bytes; i++) {
			buffer.put((byte) 0);
		}
	}

	private record Participant(int playerId, String name, int teamId, int level, int classId, int order, byte side,
			byte[] results) {
	}
}
