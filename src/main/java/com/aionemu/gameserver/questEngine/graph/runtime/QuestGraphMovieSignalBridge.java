package com.aionemu.gameserver.questEngine.graph.runtime;

import java.util.Objects;

import com.aionemu.gameserver.model.gameobjects.player.MoviePlaybackAuthority.Playback;
import com.aionemu.gameserver.questEngine.graph.QuestGraphDefinitionRegistry;
import com.aionemu.gameserver.questEngine.graph.QuestGraphDefinitionRegistry.Snapshot;
import com.aionemu.gameserver.questEngine.graph.runtime.DispatchResult.Propagation;
import com.aionemu.gameserver.questEngine.graph.runtime.DispatchResult.Status;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphEvent.MovieEndedEvent;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphTransitionExecutor.TransitionContext;

/**
 * 将一次性服务端影片播放凭据转换为 typed 事件，并通过当前原子定义快照执行任务图转换。
 * Converts one-time server movie-playback authority into a typed event and executes it against the current atomic
 * definition snapshot.
 */
public final class QuestGraphMovieSignalBridge {

	private final QuestGraphDefinitionRegistry registry;
	private final QuestGraphTransitionExecutor executor;

	/**
	 * 创建绑定原子定义 registry 与正式转换执行器的影片信号 bridge。
	 * Creates a movie-signal bridge bound to the atomic definition registry and production transition executor.
	 */
	public QuestGraphMovieSignalBridge(QuestGraphDefinitionRegistry registry, QuestGraphTransitionExecutor executor) {
		this.registry = Objects.requireNonNull(registry, "quest graph definition registry");
		this.executor = Objects.requireNonNull(executor, "quest graph transition executor");
	}

	/**
	 * 从包层已消费的不可变播放凭据创建包含完整 authority 的 typed 事件。
	 * Creates a typed event with complete authority from an immutable playback consumed by the packet boundary.
	 *
	 * @param playerId 玩家标识 / player identifier
	 * @param endedAt 服务端收到结束确认的时间 / server receipt time of the completion
	 * @param playback 已消费的服务端播放凭据 / consumed server playback authority
	 * @return 可持久化且可恢复重放的 typed 影片事件 / persistable typed movie event suitable for recovery replay
	 */
	public static MovieEndedEvent fromPlayback(int playerId, long endedAt, Playback playback) {
		if (playerId <= 0 || endedAt <= 0) {
			throw new IllegalArgumentException("Movie completion player or time is invalid");
		}
		Objects.requireNonNull(playback, "consumed movie playback");
		String eventId = "movie-end-" + playerId + '-' + playback.playbackId() + '-' + playback.startedAt();
		return new MovieEndedEvent(eventId, playerId, endedAt, playback.movieId(), playback.playbackId(), playback.startedAt());
	}

	/**
	 * 使用当前已安装定义分发影片事件；未初始化 registry、玩家漂移或 executor 失败均显式失败关闭。
	 * Dispatches a movie event through the currently installed definitions; an uninitialized registry, player drift, or
	 * executor failure closes explicitly.
	 */
	public DispatchResult dispatch(MovieEndedEvent event, TransitionContext context) {
		Objects.requireNonNull(event, "movie-ended event");
		Objects.requireNonNull(context, "transition context");
		if (event.playerId() != context.playerId()) {
			return new DispatchResult(Status.FAILED, Propagation.STOP);
		}
		Snapshot snapshot = registry.snapshot();
		if (snapshot.generation() == 0) {
			return new DispatchResult(Status.FAILED, Propagation.STOP);
		}
		QuestGraphRouter router = new QuestGraphRouter(snapshot.data());
		return router.dispatch(event, context.states(), match -> executor.execute(match, context));
	}
}
