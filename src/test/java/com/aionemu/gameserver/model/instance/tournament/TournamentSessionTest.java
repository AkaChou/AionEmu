package com.aionemu.gameserver.model.instance.tournament;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

class TournamentSessionTest {
	@Test
	void persistsLobbyStageAndRelayState() {
		TournamentSession session = new TournamentSession(3, 129, 77, 16, 1_000);
		for (int team = 1; team <= 4; team++) {
			session.addTeam(new TournamentSession.Team(team,
					List.of(new TournamentSession.Member(team * 10, "P" + team, 75, team, 0))));
		}
		session.freeze(2_000);
		List<TournamentSession.Match> matches = session.createRound(3_000);
		TournamentSession.Match match = matches.getFirst();
		session.bindStage(match.id(), 88);
		session.startMatch(match.id(), 4_000);
		session.addScore(match.id(), match.teamA(), 1);
		int version = session.stateVersion();
		assertEquals(1, session.advanceRelay(match.id(), match.teamA()));
		assertEquals(version + 1, session.stateVersion());

		TournamentSession restored = TournamentSession.decode(session.encode());
		assertEquals(77, restored.lobbyUid());
		assertEquals(88, restored.match(match.id()).stageUid());
		assertEquals(1, restored.match(match.id()).scoreA());
		assertEquals(1, restored.match(match.id()).activeA());
		assertEquals(TournamentSession.MatchState.PLAYING, restored.match(match.id()).state());
	}

	@Test
	void completesRoundsAndChampion() {
		TournamentSession session = new TournamentSession(1, 124, 7, 8, 1_000);
		for (int team = 1; team <= 3; team++) {
			session.addTeam(new TournamentSession.Team(team,
					List.of(new TournamentSession.Member(team, "P" + team, 75, 1, 0))));
		}
		session.freeze(2_000);
		while (session.state() != TournamentSession.State.COMPLETE) {
			for (TournamentSession.Match match : session.createRound(3_000)) {
				session.finishMatch(match.id(), match.teamA(), 4_000);
			}
		}
		assertTrue(session.champion() > 0);
	}
}
