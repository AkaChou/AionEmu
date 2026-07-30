package com.aionemu.gameserver.questEngine.graph.runtime;

import java.util.Objects;
import java.util.function.Function;

import com.aionemu.boot.i18n.I18n;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.TeleportInstancePolicy;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.TeleportPlayerAction;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphTransitionExecutor.ActionInvocation;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphTransitionExecutor.ActionResult;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphTransitionExecutor.PreflightResult;
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
	private final Function<TeleportCommand, ActionResult> endpoint;

	/**
	 * 创建在线玩家 adapter：调用正式 TeleportService2。
	 * Creates an online-player adapter that calls production TeleportService2.
	 */
	public QuestGraphTeleportActionAdapter(Player player) {
		this(requirePlayer(player).getObjectId(), productionInstances(player), command -> teleport(player, command));
	}

	/** 创建可注入端点的聚焦测试 adapter。 / Creates a focused-test adapter with an injectable endpoint. */
	QuestGraphTeleportActionAdapter(int playerId, Function<TeleportCommand, ActionResult> endpoint) {
		this(playerId, RegisteredInstanceGateway.UNAVAILABLE, endpoint);
	}

	/** 创建可注入 instance 与传送端点的聚焦测试 adapter。 / Creates a focused-test adapter with injectable instance and teleport endpoints. */
	QuestGraphTeleportActionAdapter(int playerId, RegisteredInstanceGateway instances,
			Function<TeleportCommand, ActionResult> endpoint) {
		if (playerId <= 0) {
			throw new IllegalArgumentException("Teleport adapter player id is invalid");
		}
		this.playerId = playerId;
		this.instances = Objects.requireNonNull(instances, "instances");
		this.endpoint = Objects.requireNonNull(endpoint, "endpoint");
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
			command = command(invocation);
		} catch (RuntimeException e) {
			return ActionResult.FAILED;
		}
		try {
			return Objects.requireNonNull(endpoint.apply(resolveInstance(command)), "teleport result");
		} catch (RuntimeException e) {
			log.error(I18n.get("log.quest_graph_teleport_failed", command.questId(), command.playerId(), command.worldId(),
				command.instancePolicy(), command.idempotencyKey()), e);
			return ActionResult.FAILED;
		}
	}

	private TeleportCommand command(ActionInvocation invocation) {
		if (invocation == null || invocation.event().playerId() != playerId
				|| !(invocation.action() instanceof TeleportPlayerAction action)) {
			throw new IllegalArgumentException("Teleport invocation is invalid");
		}
		if (action.instancePolicy() == TeleportInstancePolicy.PLAYER_REGISTERED_OR_CREATE
				&& !instances.isInstanceWorld(action.worldId())) {
			throw new IllegalArgumentException("Registered-instance teleport requires an instance world");
		}
		return new TeleportCommand(invocation.questId(), playerId, action.worldId(), action.instanceId(), action.instancePolicy(),
			action.x(), action.y(), action.z(), action.heading(), invocation.idempotencyKey());
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

	private static ActionResult teleport(Player player, TeleportCommand command) {
		if (player.getObjectId() != command.playerId()) {
			return ActionResult.FAILED;
		}
		boolean ok;
		if (command.instanceId() > 0) {
			ok = TeleportService2.teleportTo(player, command.worldId(), command.instanceId(), command.x(), command.y(), command.z(),
				command.heading());
		} else {
			ok = TeleportService2.teleportTo(player, command.worldId(), command.x(), command.y(), command.z(), command.heading());
		}
		return ok ? ActionResult.APPLIED : ActionResult.FAILED;
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
