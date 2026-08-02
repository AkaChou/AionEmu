package com.aionemu.gameserver.questEngine.runtime;

/** Typed boundary for the after-commit movie playback effect. */
public interface QuestMoviePort {
	/**
	 * Plays the given cutscene after commit. The movie-end event is the
	 * authoritative client callback; a local sleep/timer must never estimate it.
	 *
	 * @return true 表示已发送播放; false 表示玩家离线/失败 (best-effort, 记录审计)
	 */
	boolean playMovie(QuestSnapshot snapshot, QuestMutationPlan plan, int movieId);
}
