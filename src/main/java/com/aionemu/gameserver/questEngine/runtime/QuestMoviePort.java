package com.aionemu.gameserver.questEngine.runtime;

import com.aionemu.gameserver.questEngine.definition.QuestMovieType;

/** Typed boundary for the after-commit movie playback effect. */
public interface QuestMoviePort {
	/**
	 * Plays the given cutscene after commit. The movie-end event is the
	 * authoritative client callback; a local sleep/timer must never estimate it.
	 *
	 * @return true 表示已发送播放; false 表示玩家离线/失败 (best-effort, 记录审计)
	 */
	boolean playMovie(QuestSnapshot snapshot, QuestMutationPlan plan, int movieId);

	/** Plays a movie from the requested client resource family. */
	default boolean playMovie(QuestSnapshot snapshot, QuestMutationPlan plan, int movieId, QuestMovieType type) {
		return playMovie(snapshot, plan, movieId);
	}
}
