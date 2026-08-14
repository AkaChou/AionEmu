package com.aionemu.gameserver.questEngine.runtime;

import com.aionemu.gameserver.questEngine.definition.QuestMovieType;

/** 提交后影片播放效果的类型化边界。 / Typed boundary for the after-commit movie playback effect. */
public interface QuestMoviePort {
	/**
	 * 提交后播放指定过场。影片结束事件是权威客户端回调；本地 sleep/计时器绝不能估算它。
	 * Plays the given cutscene after commit. The movie-end event is the
	 * authoritative client callback; a local sleep/timer must never estimate it.
	 *
	 * @return true 表示已发送播放；false 表示玩家离线/失败（best-effort，记录审计） / true if playback was sent; false if the player is offline or it failed (best-effort, audited)
	 */
	boolean playMovie(QuestSnapshot snapshot, QuestMutationPlan plan, int movieId);

	/** 从请求的客户端资源族播放影片。 / Plays a movie from the requested client resource family. */
	default boolean playMovie(QuestSnapshot snapshot, QuestMutationPlan plan, int movieId, QuestMovieType type) {
		return playMovie(snapshot, plan, movieId);
	}
}
