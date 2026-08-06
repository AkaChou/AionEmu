package com.aionemu.gameserver.questEngine.runtime;

import com.aionemu.gameserver.questEngine.definition.AfterCommitAction;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * play-movie-random 运行层：TypedQuestAfterCommitPort 等概率随机选一播放（80016/80018 的 MOVIE bonus 语义）。
 */
class QuestMovieRandomDispatchTest {

	@Test
	void afterCommitDispatchPlaysOneOfTheRandomVariants() {
		List<Integer> movies = new ArrayList<>();
		TypedQuestAfterCommitPort port = new TypedQuestAfterCommitPort(dialogPort(), teleportPort(), (snapshot, plan, movieId) -> {
			movies.add(movieId);
			return true;
		});

		port.execute(new AfterCommitAction.PlayMovieRandom(List.of(103, 104)), snapshot(), plan());

		assertEquals(1, movies.size(), "exactly one movie must play");
		assertTrue(movies.get(0) == 103 || movies.get(0) == 104, "played movie must be one of the variants");
	}

	@Test
	void randomMovieDispatchFailsClosedWithoutMoviePort() {
		TypedQuestAfterCommitPort port = new TypedQuestAfterCommitPort(dialogPort(), teleportPort(), null);

		assertThrows(IllegalArgumentException.class,
			() -> port.execute(new AfterCommitAction.PlayMovieRandom(List.of(103, 104)), snapshot(), plan()));
	}

	private static QuestDialogPort dialogPort() {
		return new QuestDialogPort() {
			@Override
			public boolean closeDialog(QuestSnapshot snapshot, QuestMutationPlan plan) {
				return true;
			}

			@Override
			public boolean showDialog(QuestSnapshot snapshot, QuestMutationPlan plan, int dialogId) {
				return true;
			}

			@Override
			public boolean showSelectionDialog(QuestSnapshot snapshot, QuestMutationPlan plan, int dialogId) {
				return true;
			}
		};
	}

	private static QuestTeleportPort teleportPort() {
		return (snapshot, plan, worldId, x, y, z, heading) -> true;
	}

	private static QuestSnapshot snapshot() {
		return new QuestSnapshot(7, 80016, QuestStatus.REWARD, 1, Map.of(), Map.of());
	}

	private static QuestMutationPlan plan() {
		return new QuestMutationPlan(80016, QuestStatus.REWARD, 1, List.of(), List.of());
	}
}
