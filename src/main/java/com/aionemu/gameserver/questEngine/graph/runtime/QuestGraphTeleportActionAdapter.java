package com.aionemu.gameserver.questEngine.graph.runtime;

import java.util.Objects;
import java.util.function.Function;

import com.aionemu.boot.i18n.I18n;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.TeleportHeadingPolicy;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.TeleportInstancePolicy;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.TeleportPlayerAction;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphTransitionExecutor.ActionInvocation;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphTransitionExecutor.ActionResult;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphTransitionExecutor.PreflightResult;
import com.aionemu.gameserver.questEngine.graph.state.PlayerQuestGraphState.TeleportPlan;
import com.aionemu.gameserver.questEngine.graph.state.TeleportOutboxCommand;
import com.aionemu.gameserver.questEngine.graph.state.TeleportOutboxCommand.InstanceRecoveryMode;
import com.aionemu.gameserver.services.instance.InstanceService;
import com.aionemu.gameserver.services.teleport.TeleportService2;
import com.aionemu.gameserver.world.WorldMapInstance;

import lombok.extern.slf4j.Slf4j;

/**
 * 将 teleport-player 接到 TeleportService2 服务端传送权威。
 * Connects teleport-player to TeleportService2 server teleport authority.
 */
@Slf4j
public final class QuestGraphTeleportActionAdapter {

	private final int playerId;
	private final RegisteredInstanceGateway instances;
	private final CurrentPlayerContextGateway currentContext;
	private final Function<TeleportCommand, ActionResult> endpoint;
	private final QuestGraphTeleportOutbox outbox;

	/**
	 * 创建在线玩家 adapter：调用正式 TeleportService2。
	 * Creates an online-player adapter that calls production TeleportService2.
	 */
	public QuestGraphTeleportActionAdapter(Player player) {
		this(requirePlayer(player).getObjectId(), productionInstances(player), productionCurrentContext(player), null,
			new QuestGraphTeleportOutbox(player));
	}

	/** 创建可注入端点的聚焦测试 adapter。 / Creates a focused-test adapter with an injectable endpoint. */
	QuestGraphTeleportActionAdapter(int playerId, Function<TeleportCommand, ActionResult> endpoint) {
		this(playerId, RegisteredInstanceGateway.UNAVAILABLE, CurrentPlayerContextGateway.UNAVAILABLE, endpoint);
	}

	/** 创建可注入 instance 与传送端点的聚焦测试 adapter。 / Creates a focused-test adapter with injectable instance and teleport endpoints. */
	QuestGraphTeleportActionAdapter(int playerId, RegisteredInstanceGateway instances,
			Function<TeleportCommand, ActionResult> endpoint) {
		this(playerId, instances, CurrentPlayerContextGateway.UNAVAILABLE, endpoint);
	}

	/** 创建可注入 instance、玩家上下文与传送端点的聚焦测试 adapter。 / Creates a focused-test adapter with injectable instance, player-context, and teleport endpoints. */
	QuestGraphTeleportActionAdapter(int playerId, RegisteredInstanceGateway instances, CurrentPlayerContextGateway currentContext,
			Function<TeleportCommand, ActionResult> endpoint) {
		this(playerId, instances, currentContext, endpoint, null);
	}

	QuestGraphTeleportActionAdapter(int playerId, RegisteredInstanceGateway instances, CurrentPlayerContextGateway currentContext,
			QuestGraphTeleportOutbox outbox) {
		this(playerId, instances, currentContext, null, outbox);
	}

	private QuestGraphTeleportActionAdapter(int playerId, RegisteredInstanceGateway instances, CurrentPlayerContextGateway currentContext,
			Function<TeleportCommand, ActionResult> endpoint, QuestGraphTeleportOutbox outbox) {
		if (playerId <= 0) {
			throw new IllegalArgumentException("Teleport adapter player id is invalid");
		}
		if ((endpoint == null) == (outbox == null)) {
			throw new IllegalArgumentException("Teleport adapter requires exactly one execution authority");
		}
		this.playerId = playerId;
		this.instances = Objects.requireNonNull(instances, "instances");
		this.currentContext = Objects.requireNonNull(currentContext, "currentContext");
		this.endpoint = endpoint;
		this.outbox = outbox;
	}

	/** 在任何副作用与 PREPARED 写入前冻结动态 instance/heading。 / Freezes dynamic instance/heading before side effects and PREPARED persistence. */
	public TeleportPlan preparePlan(ActionInvocation invocation) {
		TeleportPlayerAction action = action(invocation);
		if (!action.requiresCurrentContext()) {
			return null;
		}
		PlayerContext snapshot = Objects.requireNonNull(currentContext.snapshot(), "current player context");
		if (snapshot.playerId() != playerId
				|| (action.instancePolicy() == TeleportInstancePolicy.PLAYER_CURRENT
					|| action.instancePolicy() == TeleportInstancePolicy.EXPLICIT_OR_DEFAULT && action.instanceId() == 0)
					&& snapshot.instanceId() <= 0) {
			throw new IllegalStateException("Current player teleport context is invalid");
		}
		int instanceId;
		if (action.instancePolicy() == TeleportInstancePolicy.PLAYER_CURRENT) {
			instanceId = snapshot.instanceId();
		} else if (action.instancePolicy() == TeleportInstancePolicy.EXPLICIT_OR_DEFAULT && action.instanceId() == 0) {
			if (snapshot.worldId() <= 0) {
				throw new IllegalStateException("Current player world is unavailable for default-instance teleport");
			}
			instanceId = snapshot.worldId() == action.worldId() ? snapshot.instanceId() : 1;
		} else {
			instanceId = action.instanceId();
		}
		byte heading = action.headingPolicy() == TeleportHeadingPolicy.PLAYER_CURRENT ? snapshot.heading() : action.heading();
		TeleportPlan plan = new TeleportPlan(invocation.actionIndex(), action.worldId(), instanceId, action.x(), action.y(), action.z(), heading);
		requirePlan(invocation, action, plan);
		return plan;
	}

	/**
	 * 预检传送命令；错误 owner/动作失败关闭。
	 * Preflights the teleport command and fails closed on wrong owner/action.
	 */
	public PreflightResult preflight(ActionInvocation invocation) {
		try {
			command(invocation);
			return PreflightResult.READY;
		} catch (RuntimeException e) {
			return PreflightResult.FAILED;
		}
	}

	/**
	 * 执行服务端传送；失败关闭且无默认成功。
	 * Executes the server teleport and fails closed with no default success.
	 */
	public ActionResult execute(ActionInvocation invocation) {
		TeleportCommand command;
		try {
			command = resolveInstance(command(invocation));
		} catch (RuntimeException e) {
			return ActionResult.FAILED;
		}
		try {
			if (outbox != null) {
				if (!invocation.hasJournalIdentity()) {
					return ActionResult.FAILED;
				}
				return outbox.accept(outboxCommand(invocation, command));
			}
			return Objects.requireNonNull(endpoint.apply(command), "teleport result");
		} catch (RuntimeException e) {
			log.error(I18n.get("log.quest_graph_teleport_failed", command.questId(), command.playerId(), command.worldId(),
				command.instancePolicy(), command.idempotencyKey()), e);
			return ActionResult.FAILED;
		}
	}

	/** Acknowledges the durable teleport only after the executor has advanced its journal cursor. */
	public ActionResult acknowledgeGraph(ActionInvocation invocation) {
		if (outbox == null) {
			return ActionResult.ALREADY_APPLIED;
		}
		if (invocation == null || !invocation.hasJournalIdentity() || invocation.event().playerId() != playerId
				|| !(invocation.action() instanceof TeleportPlayerAction)) {
			return ActionResult.FAILED;
		}
		return outbox.acknowledgeGraph(invocation.questId(), invocation.baseRevision(), invocation.transitionId(), invocation.actionIndex(),
			invocation.idempotencyKey());
	}

	private static TeleportOutboxCommand outboxCommand(ActionInvocation invocation, TeleportCommand command) {
		TeleportPlayerAction action = (TeleportPlayerAction) invocation.action();
		return new TeleportOutboxCommand(command.playerId(), command.questId(), invocation.baseRevision(), invocation.transitionId(),
			invocation.actionIndex(), command.worldId(), command.instanceId(), recoveryMode(action), command.x(), command.y(), command.z(), command.heading(),
			command.idempotencyKey());
	}

	private static InstanceRecoveryMode recoveryMode(TeleportPlayerAction action) {
		return switch (action.instancePolicy()) {
			case PLAYER_CURRENT -> InstanceRecoveryMode.PLAYER_CURRENT;
			case PLAYER_REGISTERED_OR_CREATE -> InstanceRecoveryMode.PLAYER_REGISTERED_OR_CREATE;
			case EXPLICIT_OR_DEFAULT -> action.instanceId() == 0 ? InstanceRecoveryMode.DEFAULT_INSTANCE : InstanceRecoveryMode.EXACT;
		};
	}

	private TeleportCommand command(ActionInvocation invocation) {
		TeleportPlayerAction action = action(invocation);
		if (action.instancePolicy() == TeleportInstancePolicy.PLAYER_REGISTERED_OR_CREATE
				&& !instances.isInstanceWorld(action.worldId())) {
			throw new IllegalArgumentException("Registered-instance teleport requires an instance world");
		}
		if (action.requiresCurrentContext()) {
			TeleportPlan plan = invocation.teleportPlan();
			requirePlan(invocation, action, plan);
			TeleportInstancePolicy effectivePolicy = action.instancePolicy() == TeleportInstancePolicy.PLAYER_CURRENT
				? TeleportInstancePolicy.EXPLICIT_OR_DEFAULT : action.instancePolicy();
			return new TeleportCommand(invocation.questId(), playerId, plan.worldId(), plan.instanceId(),
				effectivePolicy, plan.x(), plan.y(), plan.z(), plan.heading(), invocation.idempotencyKey());
		}
		if (invocation.teleportPlan() != null) {
			throw new IllegalArgumentException("Static teleport must not carry a frozen plan");
		}
		return new TeleportCommand(invocation.questId(), playerId, action.worldId(), action.instanceId(), action.instancePolicy(),
			action.x(), action.y(), action.z(), action.heading(), invocation.idempotencyKey());
	}

	private TeleportPlayerAction action(ActionInvocation invocation) {
		if (invocation == null || invocation.event().playerId() != playerId
				|| !(invocation.action() instanceof TeleportPlayerAction action)) {
			throw new IllegalArgumentException("Teleport invocation is invalid");
		}
		return action;
	}

	private static void requirePlan(ActionInvocation invocation, TeleportPlayerAction action, TeleportPlan plan) {
		if (plan == null || plan.actionIndex() != invocation.actionIndex() || plan.worldId() != action.worldId()
				|| Float.compare(plan.x(), action.x()) != 0 || Float.compare(plan.y(), action.y()) != 0
				|| Float.compare(plan.z(), action.z()) != 0
				|| ((action.instancePolicy() == TeleportInstancePolicy.PLAYER_CURRENT
					|| action.instancePolicy() == TeleportInstancePolicy.EXPLICIT_OR_DEFAULT && action.instanceId() == 0)
					? plan.instanceId() <= 0 : plan.instanceId() != action.instanceId())
				|| action.headingPolicy() != TeleportHeadingPolicy.PLAYER_CURRENT && plan.heading() != action.heading()) {
			throw new IllegalArgumentException("Frozen teleport plan does not match its action");
		}
	}

	private TeleportCommand resolveInstance(TeleportCommand command) {
		if (command.instancePolicy() == TeleportInstancePolicy.EXPLICIT_OR_DEFAULT) {
			return command;
		}
		int instanceId = instances.findRegistered(command.worldId(), playerId);
		if (instanceId <= 0) {
			instanceId = instances.createAndRegister(command.worldId(), playerId);
		}
		if (instanceId <= 0) {
			throw new IllegalStateException("Registered instance resolution failed");
		}
		return command.withInstanceId(instanceId);
	}

	private static Player requirePlayer(Player player) {
		return Objects.requireNonNull(player, "player");
	}

	private static RegisteredInstanceGateway productionInstances(Player player) {
		return new RegisteredInstanceGateway() {
			@Override
			public boolean isInstanceWorld(int worldId) {
				return com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().getWorldMap(worldId).isInstanceType();
			}

			@Override
			public int findRegistered(int worldId, int playerId) {
				WorldMapInstance instance = InstanceService.getRegisteredInstance(worldId, playerId);
				return instance == null ? 0 : instance.getInstanceId();
			}

			@Override
			public int createAndRegister(int worldId, int playerId) {
				if (player.getObjectId() != playerId) {
					return 0;
				}
				WorldMapInstance instance = InstanceService.getRegisteredOrCreateAndRegister(worldId, player);
				return instance == null ? 0 : instance.getInstanceId();
			}
		};
	}

	private static CurrentPlayerContextGateway productionCurrentContext(Player player) {
		return () -> new PlayerContext(player.getObjectId(), player.getWorldId(), player.getInstanceId(), player.getHeading());
	}

	/** 封闭玩家注册副本的查找与创建登记边界。 / Closes registered-instance lookup and create/register behind a typed boundary. */
	interface RegisteredInstanceGateway {
		RegisteredInstanceGateway UNAVAILABLE = new RegisteredInstanceGateway() {
			@Override
			public boolean isInstanceWorld(int worldId) {
				return false;
			}

			@Override
			public int findRegistered(int worldId, int playerId) {
				return 0;
			}

			@Override
			public int createAndRegister(int worldId, int playerId) {
				return 0;
			}
		};

		boolean isInstanceWorld(int worldId);

		int findRegistered(int worldId, int playerId);

		int createAndRegister(int worldId, int playerId);
	}

	/** 只读读取 PREPARED 前的玩家 instance 与朝向。 / Read-only boundary for the player's pre-PREPARED instance and heading. */
	interface CurrentPlayerContextGateway {
		CurrentPlayerContextGateway UNAVAILABLE = () -> {
			throw new IllegalStateException("Current player context is unavailable");
		};

		PlayerContext snapshot();
	}

	/** 玩家动态传送输入的一次性快照。 / One-shot snapshot of dynamic player teleport inputs. */
	record PlayerContext(int playerId, int worldId, int instanceId, byte heading) {
		PlayerContext(int playerId, int instanceId, byte heading) {
			this(playerId, 0, instanceId, heading);
		}

		PlayerContext {
			if (playerId <= 0 || worldId < 0 || instanceId < 0) {
				throw new IllegalArgumentException("Player teleport context is invalid");
			}
		}
	}

	/** 表示服务端传送命令。 / Represents a server teleport command. */
	public record TeleportCommand(int questId, int playerId, int worldId, int instanceId,
			TeleportInstancePolicy instancePolicy, float x, float y, float z, byte heading, String idempotencyKey) {
		/** 校验传送命令。 / Validates the teleport command. */
		public TeleportCommand {
			if (questId <= 0 || playerId <= 0 || worldId <= 0 || instanceId < 0 || instancePolicy == null
					|| instancePolicy == TeleportInstancePolicy.PLAYER_REGISTERED_OR_CREATE && instanceId != 0
					|| !Float.isFinite(x) || !Float.isFinite(y)
					|| !Float.isFinite(z) || idempotencyKey == null || idempotencyKey.isBlank()) {
				throw new IllegalArgumentException("Teleport command is invalid");
			}
		}

		private TeleportCommand withInstanceId(int resolvedInstanceId) {
			return new TeleportCommand(questId, playerId, worldId, resolvedInstanceId,
				TeleportInstancePolicy.EXPLICIT_OR_DEFAULT, x, y, z, heading, idempotencyKey);
		}
	}
}
