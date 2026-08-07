package com.aionemu.gameserver.questEngine.runtime;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.definition.QuestMovieType;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import org.junit.jupiter.api.Test;
import org.objenesis.ObjenesisStd;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerQuestMoviePortTest {
	@Test
	void movieUsesTheCompiledIdAndAuthoritativePlayer() {
		Player player = new ObjenesisStd().newInstance(Player.class);
		List<Integer> movies = new ArrayList<>();
		PlayerQuestMoviePort port = new PlayerQuestMoviePort(playerId -> player, (resolved, movieId) -> {
			assertEquals(player, resolved);
			movies.add(movieId);
			return true;
		});

		assertTrue(port.playMovie(snapshot(), plan(), 250));
		assertEquals(List.of(250), movies);
	}

	@Test
	void movieFailsClosedOnInvalidIdAndReportsLoggedOutPlayer() {
		PlayerQuestMoviePort port = new PlayerQuestMoviePort(playerId -> null, (player, movieId) -> true);

		assertFalse(port.playMovie(snapshot(), plan(), 250));
		assertThrows(IllegalArgumentException.class, () -> port.playMovie(snapshot(), plan(), 0));
	}

	@Test
	void moviePassesTheClientResourceType() {
		Player player = new ObjenesisStd().newInstance(Player.class);
		List<QuestMovieType> types = new ArrayList<>();
		PlayerQuestMoviePort port = new PlayerQuestMoviePort(playerId -> player, (resolved, movieId, type) -> {
			assertEquals(player, resolved);
			assertEquals(30, movieId);
			types.add(type);
			return true;
		});

		assertTrue(port.playMovie(snapshot(), plan(), 30, QuestMovieType.CUTSCENE_MOVIE));
		assertEquals(List.of(QuestMovieType.CUTSCENE_MOVIE), types);
	}

	private static QuestSnapshot snapshot() {
		return new QuestSnapshot(7, 24154, QuestStatus.START, 2, Map.of(), Map.of());
	}

	private static QuestMutationPlan plan() {
		return new QuestMutationPlan(24154, QuestStatus.START, 2, List.of(), List.of());
	}
}
