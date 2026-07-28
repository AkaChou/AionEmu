package com.aionemu.gameserver.questEngine.graph.runtime;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_PLAY_MOVIE;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.PlayMovieAction;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphTransitionExecutor.ActionInvocation;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphTransitionExecutor.ActionResult;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * 将 PLAY_MOVIE post-commit action 连接到客户端协议和显式 retry 端口。
 * Connects PLAY_MOVIE post-commit actions to the client protocol and an explicit retry port.
 */
public final class QuestGraphMovieActionAdapter {

	private final int playerId;
	private final Function<PlayMovieCommand, ActionResult> endpoint;
	private final Function<PlayMovieCommand, ActionResult> retry;
	private final Set<String> acceptedKeys = new HashSet<>();

	/**
	 * 创建复用正式影片包的在线玩家 adapter；retry 端口必须持久化或可观测地接管失败投影。
	 * Creates an online-player adapter using the production movie packet; the retry port must durably or observably
	 * accept failed projections.
	 */
	public QuestGraphMovieActionAdapter(Player player, Function<PlayMovieCommand, ActionResult> retry) {
		this(requirePlayer(player).getObjectId(), command -> send(player, command), retry);
	}

	/** 创建带可注入协议与 retry 端口的聚焦测试 adapter。 / Creates a focused-test adapter with injectable protocol and retry ports. */
	QuestGraphMovieActionAdapter(int playerId, Function<PlayMovieCommand, ActionResult> endpoint,
			Function<PlayMovieCommand, ActionResult> retry) {
		if (playerId <= 0) {
			throw new IllegalArgumentException("Movie adapter player id is invalid");
		}
		this.playerId = playerId;
		this.endpoint = Objects.requireNonNull(endpoint, "endpoint");
		this.retry = Objects.requireNonNull(retry, "retry");
	}

	/**
	 * 执行或提交重试一个影片投影；稳定幂等键已接受时不重复播放，未知动作或 owner mismatch 显式失败。
	 * Executes or submits retry for a movie projection; an accepted stable idempotency key is not replayed, while an
	 * unknown action or owner mismatch fails explicitly.
	 */
	public synchronized ActionResult execute(ActionInvocation invocation) {
		if (invocation == null || invocation.event().playerId() != playerId || !(invocation.action() instanceof PlayMovieAction action)) {
			return ActionResult.FAILED;
		}
		if (acceptedKeys.contains(invocation.idempotencyKey())) {
			return ActionResult.ALREADY_APPLIED;
		}
		PlayMovieCommand command;
		try {
			command = new PlayMovieCommand(invocation.questId(), playerId, action.movieId(), invocation.idempotencyKey());
		} catch (RuntimeException e) {
			return ActionResult.FAILED;
		}
		ActionResult direct;
		try {
			direct = Objects.requireNonNull(endpoint.apply(command), "movie endpoint result");
		} catch (RuntimeException e) {
			direct = ActionResult.FAILED;
		}
		if (accepted(direct)) {
			acceptedKeys.add(command.idempotencyKey());
			return direct;
		}
		try {
			ActionResult deferred = Objects.requireNonNull(retry.apply(command), "movie retry result");
			if (accepted(deferred)) {
				acceptedKeys.add(command.idempotencyKey());
				return deferred;
			}
		} catch (RuntimeException ignored) {
			// A failed retry port remains an explicit FAILED result to its post-commit caller.
		}
		return ActionResult.FAILED;
	}

	/** 清理玩家会话结束后的临时幂等集合。 / Clears the temporary idempotency set after the player session ends. */
	public synchronized void clear() {
		acceptedKeys.clear();
	}

	/** 返回已接受键数量，仅用于确定性测试与审计。 / Returns the accepted-key count only for deterministic tests and audit. */
	public synchronized int size() {
		return acceptedKeys.size();
	}

	/** 判断端点是否已完成或接管该投影。 / Returns whether an endpoint completed or accepted ownership of the projection. */
	private static boolean accepted(ActionResult result) {
		return result == ActionResult.APPLIED || result == ActionResult.ALREADY_APPLIED;
	}

	/** 通过正式包发送影片；authority 在 SM_PLAY_MOVIE.writeImpl 中建立。 / Sends through the production packet; authority is established in SM_PLAY_MOVIE.writeImpl. */
	private static ActionResult send(Player player, PlayMovieCommand command) {
		if (player.getObjectId() != command.playerId() || player.getClientConnection() == null) {
			return ActionResult.FAILED;
		}
		PacketSendUtility.sendPacket(player, new SM_PLAY_MOVIE(0, command.movieId()));
		return ActionResult.APPLIED;
	}

	/** 返回经过校验的在线玩家引用。 / Returns a validated player reference. */
	private static Player requirePlayer(Player player) {
		return Objects.requireNonNull(player, "player");
	}

	/** 表示带稳定幂等键的影片协议投影。 / Represents a movie protocol projection with a stable idempotency key. */
	public record PlayMovieCommand(int questId, int playerId, int movieId, String idempotencyKey) {
		/** 校验 owner、影片引用和幂等键。 / Validates owner, movie reference, and idempotency key. */
		public PlayMovieCommand {
			if (questId <= 0 || playerId <= 0 || movieId <= 0 || movieId > 0xFFFF
					|| idempotencyKey == null || idempotencyKey.isBlank()) {
				throw new IllegalArgumentException("Play-movie command is invalid");
			}
		}
	}
}
