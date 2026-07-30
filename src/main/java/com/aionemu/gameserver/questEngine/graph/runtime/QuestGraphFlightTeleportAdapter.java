package com.aionemu.gameserver.questEngine.graph.runtime;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.IntPredicate;

import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.model.EmotionType;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.gameobjects.state.CreatureState;
import com.aionemu.gameserver.network.aion.serverpackets.SM_EMOTION;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.StartFlightTeleportAction;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphTransitionExecutor.ActionInvocation;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphTransitionExecutor.ActionResult;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphTransitionExecutor.PreflightResult;

/** 将任务图飞行传送命令接到玩家飞行状态与客户端协议。 / Connects quest-graph flight commands to player state and protocol. */
public final class QuestGraphFlightTeleportAdapter {

	private final int playerId;
	private final IntPredicate pathReferences;
	private final FlightSession session;
	private final Map<String, FlightTeleportCommand> acceptedCommands = new HashMap<>();

	/** 创建使用正式飞行路径数据和玩家连接的在线 adapter。 / Creates an online adapter backed by formal fly-path data and the player connection. */
	public QuestGraphFlightTeleportAdapter(Player player) {
		this(requirePlayer(player).getObjectId(), QuestGraphFlightTeleportAdapter::isFormalPath,
			new PlayerFlightSession(player));
	}

	/** 创建可注入路径目录和飞行会话的聚焦测试 adapter。 / Creates a focused-test adapter with injectable path and session gateways. */
	QuestGraphFlightTeleportAdapter(int playerId, IntPredicate pathReferences, FlightSession session) {
		if (playerId <= 0) {
			throw new IllegalArgumentException("Flight-teleport adapter player id is invalid");
		}
		this.playerId = playerId;
		this.pathReferences = Objects.requireNonNull(pathReferences, "pathReferences");
		this.session = Objects.requireNonNull(session, "session");
	}

	/** 在不修改玩家状态的情况下验证 owner、路径、连接与活动路径。 / Validates owner, path, connection, and active path without mutation. */
	public synchronized PreflightResult preflight(ActionInvocation invocation) {
		FlightTeleportCommand command = command(invocation);
		return command == null ? PreflightResult.FAILED : preflight(command);
	}

	/** 在不修改玩家状态的情况下验证 owner、路径、连接与活动路径。 / Validates owner, path, connection, and active path without mutation. */
	public synchronized PreflightResult preflight(FlightTeleportCommand command) {
		if (!owns(command) || session.playerId() != playerId) {
			return PreflightResult.FAILED;
		}
		FlightTeleportCommand accepted = acceptedCommands.get(command.idempotencyKey());
		if (accepted != null) {
			return accepted.equals(command) ? PreflightResult.READY : PreflightResult.REJECTED;
		}
		if (!session.hasConnection()) {
			return PreflightResult.FAILED;
		}
		if (!pathReferences.test(command.pathId())) {
			return PreflightResult.REJECTED;
		}
		int activeProtocolId = session.protocolId();
		if (session.isFlightTeleport()) {
			return activeProtocolId == command.protocolId() ? PreflightResult.READY : PreflightResult.REJECTED;
		}
		return activeProtocolId == 0 ? PreflightResult.READY : PreflightResult.REJECTED;
	}

	/** 启动飞行传送；相同命令幂等重放，不同活动路径显式拒绝。 / Starts flight teleport with idempotent replay and explicit active-path conflict rejection. */
	public synchronized ActionResult execute(ActionInvocation invocation) {
		FlightTeleportCommand command = command(invocation);
		return command == null ? ActionResult.FAILED : execute(command);
	}

	/** 启动飞行传送；相同命令幂等重放，不同活动路径显式拒绝。 / Starts flight teleport with idempotent replay and explicit active-path conflict rejection. */
	public synchronized ActionResult execute(FlightTeleportCommand command) {
		if (!owns(command) || session.playerId() != playerId) {
			return ActionResult.FAILED;
		}
		FlightTeleportCommand accepted = acceptedCommands.get(command.idempotencyKey());
		if (accepted != null) {
			return accepted.equals(command) ? ActionResult.ALREADY_APPLIED : ActionResult.REJECTED;
		}
		if (!session.hasConnection()) {
			return ActionResult.FAILED;
		}
		if (!pathReferences.test(command.pathId())) {
			return ActionResult.REJECTED;
		}

		ActionResult result = start(command);
		if (result == ActionResult.APPLIED || result == ActionResult.ALREADY_APPLIED) {
			acceptedCommands.put(command.idempotencyKey(), command);
		}
		return result;
	}

	/** 清理玩家会话结束后的临时幂等账本。 / Clears the session-local idempotency ledger. */
	public synchronized void clear() {
		acceptedCommands.clear();
	}

	/** 返回已接受命令数，仅用于确定性测试与审计。 / Returns the accepted-command count for deterministic tests and audit. */
	public synchronized int size() {
		return acceptedCommands.size();
	}

	private ActionResult start(FlightTeleportCommand command) {
		int priorProtocolId = session.protocolId();
		if (session.isFlightTeleport()) {
			return priorProtocolId == command.protocolId() ? ActionResult.ALREADY_APPLIED : ActionResult.REJECTED;
		}
		if (priorProtocolId != 0) {
			return ActionResult.REJECTED;
		}

		int priorState = session.state();
		try {
			session.setFlightTeleport();
			session.unsetActive();
			session.setProtocolId(command.protocolId());
			session.sendStart(command.protocolId());
			return ActionResult.APPLIED;
		} catch (RuntimeException e) {
			try {
				session.restore(priorState, priorProtocolId);
			} catch (RuntimeException ignored) {
				// The caller still receives an explicit failure if the underlying player object cannot be restored.
			}
			return ActionResult.FAILED;
		}
	}

	private boolean owns(FlightTeleportCommand command) {
		return command != null && command.playerId() == playerId;
	}

	private static FlightTeleportCommand command(ActionInvocation invocation) {
		if (invocation == null || !(invocation.action() instanceof StartFlightTeleportAction action)) {
			return null;
		}
		return new FlightTeleportCommand(invocation.questId(), invocation.event().playerId(), action.pathId(),
			action.protocolId(), invocation.idempotencyKey());
	}

	private static boolean isFormalPath(int pathId) {
		return DataManager.FLY_PATH != null && DataManager.FLY_PATH.containsPath(pathId);
	}

	private static Player requirePlayer(Player player) {
		return Objects.requireNonNull(player, "player");
	}

	/** 封闭正式玩家飞行状态和协议发送。 / Closes production player flight state and protocol delivery behind a typed boundary. */
	interface FlightSession {
		int playerId();

		boolean hasConnection();

		int state();

		boolean isFlightTeleport();

		int protocolId();

		void setFlightTeleport();

		void unsetActive();

		void setProtocolId(int protocolId);

		void sendStart(int protocolId);

		void restore(int state, int protocolId);
	}

	private static final class PlayerFlightSession implements FlightSession {
		private final Player player;

		private PlayerFlightSession(Player player) {
			this.player = player;
		}

		@Override
		public int playerId() {
			return player.getObjectId();
		}

		@Override
		public boolean hasConnection() {
			return player.getClientConnection() != null;
		}

		@Override
		public int state() {
			return player.getState();
		}

		@Override
		public boolean isFlightTeleport() {
			return player.isInState(CreatureState.FLIGHT_TELEPORT);
		}

		@Override
		public int protocolId() {
			return player.getFlightTeleportId();
		}

		@Override
		public void setFlightTeleport() {
			player.setState(CreatureState.FLIGHT_TELEPORT);
		}

		@Override
		public void unsetActive() {
			player.unsetState(CreatureState.ACTIVE);
		}

		@Override
		public void setProtocolId(int protocolId) {
			player.setFlightTeleportId(protocolId);
		}

		@Override
		public void sendStart(int protocolId) {
			player.getClientConnection().sendPacket(new SM_EMOTION(player, EmotionType.START_FLYTELEPORT, protocolId, 0));
		}

		@Override
		public void restore(int state, int protocolId) {
			player.setState(state);
			player.setFlightTeleportId(protocolId);
		}
	}

	/** 冻结一次飞行传送的 owner、路径、客户端协议和稳定幂等键。 / Freezes one flight teleport's owner, path, protocol, and stable idempotency key. */
	public record FlightTeleportCommand(int questId, int playerId, int pathId, int protocolId, String idempotencyKey) {
		/** 校验命令字段及客户端 flight protocol 关系。 / Validates command fields and the client flight-protocol relation. */
		public FlightTeleportCommand {
			if (questId <= 0 || playerId <= 0 || pathId <= 0 || pathId > (Integer.MAX_VALUE - 1) / 1000
					|| protocolId != pathId * 1000 + 1 || idempotencyKey == null || idempotencyKey.isBlank()) {
				throw new IllegalArgumentException("Flight-teleport command is invalid");
			}
		}
	}
}
