package com.aionemu.gameserver.model.instance.tournament;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;

class TournamentBracketTest {
	@Test
	void supportsRetailTreeSizesAndRoundStages() {
		for (int size : List.of(4, 8, 16, 32)) {
			TournamentBracket bracket = new TournamentBracket(size, teams(size));
			assertEquals(size / 2, bracket.currentMatches().size());
			while (!bracket.complete()) {
				List<TournamentBracket.Match> matches = bracket.currentMatches();
				for (TournamentBracket.Match match : matches) {
					bracket.recordWinner(match.id(), match.teamA());
				}
			}
			assertEquals(1, bracket.champion());
			assertEquals(1, bracket.placement(1));
			assertEquals(2, bracket.placement(size / 2 + 1));
			assertEquals(size - 1, bracket.results().size());
		}
	}

	@Test
	void advancesByesAndRestoresMidRound() {
		TournamentBracket bracket = new TournamentBracket(8, teams(5));
		assertEquals(2, bracket.currentMatches().size());
		TournamentBracket.Match first = bracket.currentMatches().getFirst();
		bracket.recordWinner(first.id(), first.teamB());

		TournamentBracket restored = TournamentBracket.decode(bracket.encode());
		assertEquals(bracket.round(), restored.round());
		assertEquals(1, restored.currentMatches().size());
		assertFalse(restored.complete());

		while (!restored.complete()) {
			for (TournamentBracket.Match match : restored.currentMatches()) {
				restored.recordWinner(match.id(), match.teamA());
			}
		}
		assertTrue(restored.champion() > 0);
	}

	private static List<Integer> teams(int size) {
		return IntStream.rangeClosed(1, size).boxed().toList();
	}
}
