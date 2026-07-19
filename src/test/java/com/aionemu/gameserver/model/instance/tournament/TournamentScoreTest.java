package com.aionemu.gameserver.model.instance.tournament;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.api.Test;

class TournamentScoreTest {
	@Test
	void writesFixedRetailBodies() {
		TournamentSession session = new TournamentSession(2, 125, 7, 32, 1_000);
		session.addTeam(new TournamentSession.Team(1,
				List.of(new TournamentSession.Member(11, "Alpha", 75, 4, 0))));
		session.addTeam(new TournamentSession.Team(2,
				List.of(new TournamentSession.Member(22, "Beta", 75, 5, 0))));
		session.freeze(2_000);
		TournamentSession.Match match = session.createRound(3_000).getFirst();

		byte[] lobby = TournamentScore.lobby(session, 0).payload();
		byte[] stage = TournamentScore.stage(session, match.id(), 4).payload();
		assertEquals(7_788, lobby.length);
		assertEquals(40, stage.length);
		assertFalse(new String(lobby, StandardCharsets.ISO_8859_1).contains("ENCOM"));
	}
}
