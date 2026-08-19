package com.aionemu.gameserver.questEngine.runtime;

import com.aionemu.gameserver.ai2.NpcAI2;
import com.aionemu.gameserver.ai2.event.AIEventType;
import com.aionemu.gameserver.ai2.manager.EmoteManager;
import com.aionemu.gameserver.ai2.manager.WalkManager;
import com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices;
import com.aionemu.gameserver.model.EmotionId;
import com.aionemu.gameserver.model.EmotionType;
import com.aionemu.gameserver.model.TaskId;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.VisibleObject;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_EMOTION;
import com.aionemu.gameserver.network.aion.serverpackets.SM_NPC_INFO;
import com.aionemu.gameserver.questEngine.definition.QuestLureCompletion;
import com.aionemu.gameserver.questEngine.definition.QuestNpcEmotion;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.task.QuestTasks;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.world.zone.ZoneName;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.Future;

/**
 * Real {@link QuestAiPort}: issues AI commands to the authoritative NPC handle
 * registered under a slot by {@link QuestSpawnPort#spawnNpc}. 命令绝不凭
 * templateId 寻址,只通过本任务 spawn 的权威 handle。
 */
public final class PlayerQuestAiPort implements QuestAiPort {
	public enum Command {
		START_FOLLOW,
		STOP_FOLLOW,
		ATTACK_TARGET,
		ATTACK_NPC_TEMPLATE,
		START_WALKING,
		BROADCAST_START_EMOTE2,
		BROADCAST_INTERACTION_EMOTION
	}

	/** 可注入的 AI 命令委托（生产 = 真实 AI 调用，测试 = 记录器）。 / Injectable AI command delegate (production = real AI calls, tests = recorder). */
	@FunctionalInterface
	public interface AiCall {
		boolean apply(Npc npc, Player player, VisibleObject target, Command command, String argument);
	}

	@FunctionalInterface
	public interface TargetResolver {
		VisibleObject find(int objectId);
	}

	@FunctionalInterface
	public interface FollowCall {
		Future<?> start(Player player, Npc npc, int questId, String zone);
	}

	@FunctionalInterface
	public interface CoordinateFollowCall {
		Future<?> start(Player player, Npc npc, int questId, float x, float y, float z);
	}

	@FunctionalInterface
	public interface TargetNpcFollowCall {
		Future<?> start(Player player, Npc npc, Npc target, int questId);
	}

	/**
	 * 任务诱导坐标检查器启动委托，保留 NPC 到达后的显式完成策略。
	 * Delegate that starts a lure-coordinate watcher while preserving the explicit NPC completion effect.
	 */
	@FunctionalInterface
	public interface LuredNpcWatchCall {
		boolean start(Player player, Npc npc, int questId, float x, float y, float z, float radius,
			QuestLureCompletion completion);
	}

	@FunctionalInterface
	public interface TargetNpcResolver {
		Npc find(Player player, int templateId);
	}

	@FunctionalInterface
	public interface FollowTaskRegistrar {
		void register(Player player, Future<?> task);
	}

	/**
	 * 向玩家同步常驻 NPC 信息。生产实现发送真实的 {@code SM_NPC_INFO}，测试可注入无副作用记录器。
	 * Syncs the persistent NPC info to the player. Production sends the real {@code SM_NPC_INFO};
	 * tests may inject a side-effect-free recorder.
	 */
	@FunctionalInterface
	public interface NpcInfoCall {
		void send(Player player, Npc npc);
	}

	@FunctionalInterface
	public interface WorldNpcResolver {
		Npc find(Player player, int templateId);
	}

	private final QuestPlayerPort players;
	private final QuestSpawnRegistry registry;
	private final AiCall ai;
	private final TargetResolver targets;
	private final FollowCall follow;
	private final CoordinateFollowCall coordinateFollow;
	private TargetNpcFollowCall targetNpcFollow;
	private LuredNpcWatchCall luredNpcWatch;
	private TargetNpcResolver targetNpcResolver;
	private final FollowTaskRegistrar taskRegistrar;
	private final NpcInfoCall npcInfo;
	private WorldNpcResolver worldNpcResolver;

	public PlayerQuestAiPort(QuestPlayerPort players, QuestSpawnRegistry registry) {
		this(players, registry, (npc, player, target, command, argument) -> switch (command) {
			case START_FOLLOW -> {
				npc.getAi2().onCreatureEvent(AIEventType.FOLLOW_ME, player);
				yield true;
			}
			case STOP_FOLLOW -> {
				npc.getAi2().onCreatureEvent(AIEventType.STOP_FOLLOW_ME, player);
				yield true;
			}
			case ATTACK_TARGET -> {
				if (target == null) {
					yield false;
				}
				npc.setTarget(target);
				npc.getMoveController().moveToTargetObject();
				npc.getAi2().onGeneralEvent(AIEventType.ATTACK);
				yield true;
			}
			case ATTACK_NPC_TEMPLATE -> {
				if (!(target instanceof Creature creature)) {
					yield false;
				}
				npc.setTarget(target);
				npc.getMoveController().moveToTargetObject();
				npc.getAggroList().addHate(creature, 1000);
				EmoteManager.emoteStartAttacking(npc);
				npc.getAi2().onGeneralEvent(AIEventType.ATTACK);
				yield true;
			}
			case START_WALKING -> WalkManager.startWalking((NpcAI2) npc.getAi2());
			case BROADCAST_START_EMOTE2 -> {
				PacketSendUtility.broadcastPacket(npc,
					new SM_EMOTION(npc, EmotionType.START_EMOTE2, 0, npc.getObjectId()));
				yield true;
			}
			case BROADCAST_INTERACTION_EMOTION -> {
				EmotionId emotion = EmotionId.valueOf(argument);
				PacketSendUtility.broadcastPacket(player,
					new SM_EMOTION(npc, EmotionType.EMOTE, emotion.id(), player.getObjectId()), true);
				yield true;
			}
		}, objectId -> GameWorldBootstrapServices.world().findVisibleObject(objectId),
		(player, npc, questId, zone) -> QuestTasks.newFollowingToTargetCheckTask(
			new QuestEnv(null, player, questId, 0), npc, ZoneName.get(zone)),
		(player, npc, questId, x, y, z) -> QuestTasks.newFollowingToTargetCheckTask(
			new QuestEnv(null, player, questId, 0), npc, x, y, z),
		(player, task) -> player.getController().addTask(TaskId.QUEST_FOLLOW, task));
	}

	public PlayerQuestAiPort(QuestPlayerPort players, QuestSpawnRegistry registry, AiCall ai) {
		this(players, registry, ai, objectId -> GameWorldBootstrapServices.world().findVisibleObject(objectId),
			(player, npc, questId, zone) -> QuestTasks.newFollowingToTargetCheckTask(
				new QuestEnv(null, player, questId, 0), npc, ZoneName.get(zone)),
			(player, npc, questId, x, y, z) -> QuestTasks.newFollowingToTargetCheckTask(
				new QuestEnv(null, player, questId, 0), npc, x, y, z),
			(player, task) -> player.getController().addTask(TaskId.QUEST_FOLLOW, task));
	}

	public PlayerQuestAiPort(QuestPlayerPort players, QuestSpawnRegistry registry, AiCall ai,
			TargetResolver targets, FollowCall follow) {
		this(players, registry, ai, targets, follow,
			(player, npc, questId, x, y, z) -> QuestTasks.newFollowingToTargetCheckTask(
				new QuestEnv(null, player, questId, 0), npc, x, y, z),
			(player, task) -> player.getController().addTask(TaskId.QUEST_FOLLOW, task));
	}

	public PlayerQuestAiPort(QuestPlayerPort players, QuestSpawnRegistry registry, AiCall ai,
			TargetResolver targets, FollowCall follow, CoordinateFollowCall coordinateFollow,
			FollowTaskRegistrar taskRegistrar) {
		this(players, registry, ai, targets, follow, coordinateFollow, taskRegistrar,
			(player, npc) -> PacketSendUtility.sendPacket(player, new SM_NPC_INFO(npc, player)));
	}

	public PlayerQuestAiPort(QuestPlayerPort players, QuestSpawnRegistry registry, AiCall ai,
			TargetResolver targets, FollowCall follow, CoordinateFollowCall coordinateFollow,
			FollowTaskRegistrar taskRegistrar, NpcInfoCall npcInfo) {
		this.players = Objects.requireNonNull(players, "players");
		this.registry = Objects.requireNonNull(registry, "registry");
		this.ai = Objects.requireNonNull(ai, "ai");
		this.targets = Objects.requireNonNull(targets, "targets");
		this.follow = Objects.requireNonNull(follow, "follow");
		this.coordinateFollow = Objects.requireNonNull(coordinateFollow, "coordinateFollow");
		this.targetNpcFollow = (player, npc, target, questId) -> QuestTasks.newFollowingToTargetCheckTask(
			new QuestEnv(null, player, questId, 0), npc, target);
		this.luredNpcWatch = (player, npc, questId, x, y, z, radius, completion) -> {
			if (player.getController().hasScheduledTask(TaskId.QUEST_LURE)) {
				return true;
			}
			Future<?> task = QuestTasks.newLuredNpcToCoordinateCheckTask(
				new QuestEnv(npc, player, questId, 0), npc, x, y, z, radius, completion);
			if (task == null) {
				return false;
			}
			player.getController().addTask(TaskId.QUEST_LURE, task);
			return true;
		};
		this.targetNpcResolver = (player, templateId) -> findLivingNpc(player, templateId);
		this.taskRegistrar = Objects.requireNonNull(taskRegistrar, "taskRegistrar");
		this.npcInfo = Objects.requireNonNull(npcInfo, "npcInfo");
		this.worldNpcResolver = (player, templateId) -> findLivingNpc(player, templateId);
	}

	PlayerQuestAiPort(QuestPlayerPort players, QuestSpawnRegistry registry, AiCall ai,
		TargetResolver targets, FollowCall follow, CoordinateFollowCall coordinateFollow,
		FollowTaskRegistrar taskRegistrar, NpcInfoCall npcInfo, WorldNpcResolver worldNpcResolver) {
		this(players, registry, ai, targets, follow, coordinateFollow, taskRegistrar, npcInfo);
		this.worldNpcResolver = Objects.requireNonNull(worldNpcResolver, "worldNpcResolver");
	}

	PlayerQuestAiPort(QuestPlayerPort players, QuestSpawnRegistry registry, AiCall ai,
		TargetResolver targets, FollowCall follow, CoordinateFollowCall coordinateFollow,
		FollowTaskRegistrar taskRegistrar, NpcInfoCall npcInfo, LuredNpcWatchCall luredNpcWatch) {
		this(players, registry, ai, targets, follow, coordinateFollow, taskRegistrar, npcInfo);
		this.luredNpcWatch = Objects.requireNonNull(luredNpcWatch, "luredNpcWatch");
	}

	@Override
	public boolean startFollow(QuestSnapshot snapshot, QuestMutationPlan plan, String slot) {
		return run(snapshot, slot, Command.START_FOLLOW);
	}

	@Override
	public boolean startFollowCurrentTargetToPoint(QuestSnapshot snapshot, QuestMutationPlan plan,
			float x, float y, float z) {
		Objects.requireNonNull(snapshot, "snapshot");
		if (!Float.isFinite(x) || !Float.isFinite(y) || !Float.isFinite(z)) {
			throw new IllegalArgumentException("follow destination coordinates must be finite");
		}
		Player player = players.find(snapshot.playerId());
		if (player == null || snapshot.interactionObjectId() == 0) {
			return false;
		}
		VisibleObject visible = targets.find(snapshot.interactionObjectId());
		if (!(visible instanceof Npc npc)) {
			return false;
		}
		npcInfo.send(player, npc);
		if (!ai.apply(npc, player, null, Command.START_FOLLOW, null)) {
			return false;
		}
		Future<?> task = coordinateFollow.start(player, npc, snapshot.questId(), x, y, z);
		if (task == null) {
			return false;
		}
		taskRegistrar.register(player, task);
		return true;
	}

	PlayerQuestAiPort(QuestPlayerPort players, QuestSpawnRegistry registry, AiCall ai,
		TargetResolver targets, FollowCall follow, CoordinateFollowCall coordinateFollow,
		FollowTaskRegistrar taskRegistrar, NpcInfoCall npcInfo, TargetNpcResolver targetNpcResolver,
		TargetNpcFollowCall targetNpcFollow) {
		this(players, registry, ai, targets, follow, coordinateFollow, taskRegistrar, npcInfo);
		this.targetNpcResolver = Objects.requireNonNull(targetNpcResolver, "targetNpcResolver");
		this.targetNpcFollow = Objects.requireNonNull(targetNpcFollow, "targetNpcFollow");
	}

	@Override
	public boolean startFollowCurrentTargetToNpc(QuestSnapshot snapshot, QuestMutationPlan plan, int npcId) {
		Objects.requireNonNull(snapshot, "snapshot");
		if (npcId <= 0) {
			throw new IllegalArgumentException("npcId must be positive");
		}
		Player player = players.find(snapshot.playerId());
		if (player == null || snapshot.interactionObjectId() == 0) {
			return false;
		}
		VisibleObject visible = targets.find(snapshot.interactionObjectId());
		if (!(visible instanceof Npc npc)) {
			return false;
		}
		Npc target = targetNpcResolver.find(player, npcId);
		if (target == null) {
			return false;
		}
		npcInfo.send(player, npc);
		if (!ai.apply(npc, player, null, Command.START_FOLLOW, null)) {
			return false;
		}
		Future<?> task = targetNpcFollow.start(player, npc, target, snapshot.questId());
		if (task == null) {
			return false;
		}
		taskRegistrar.register(player, task);
		return true;
	}

	@Override
	public boolean stopFollow(QuestSnapshot snapshot, QuestMutationPlan plan, String slot) {
		return run(snapshot, slot, Command.STOP_FOLLOW);
	}

	@Override
	public boolean attackTarget(QuestSnapshot snapshot, QuestMutationPlan plan, String slot) {
		return run(snapshot, slot, Command.ATTACK_TARGET);
	}

	@Override
	public boolean attackNpcTemplate(QuestSnapshot snapshot, QuestMutationPlan plan, String slot, int templateId) {
		Objects.requireNonNull(snapshot, "snapshot");
		if (templateId <= 0) {
			throw new IllegalArgumentException("templateId must be positive");
		}
		if (slot == null || slot.isBlank()) {
			throw new IllegalArgumentException("slot must not be blank");
		}
		Npc npc = registry.get(snapshot, slot);
		Player player = players.find(snapshot.playerId());
		if (npc == null || player == null) {
			return false;
		}
		Npc target = worldNpcResolver.find(player, templateId);
		return target != null && ai.apply(npc, player, target, Command.ATTACK_NPC_TEMPLATE,
			Integer.toString(templateId));
	}

	@Override
	public boolean startWalking(QuestSnapshot snapshot, QuestMutationPlan plan, String slot) {
		return run(snapshot, slot, Command.START_WALKING, null);
	}

	@Override
	public boolean broadcastEmotion(QuestSnapshot snapshot, QuestMutationPlan plan, String slot,
			QuestNpcEmotion emotion) {
		Objects.requireNonNull(emotion, "emotion");
		if (emotion != QuestNpcEmotion.START_EMOTE2) {
			throw new IllegalArgumentException("unsupported quest NPC emotion: " + emotion);
		}
		return run(snapshot, slot, Command.BROADCAST_START_EMOTE2, null);
	}

	@Override
	public boolean broadcastInteractionEmotion(QuestSnapshot snapshot, QuestMutationPlan plan,
			QuestNpcEmotion emotion) {
		Objects.requireNonNull(snapshot, "snapshot");
		if (emotion != QuestNpcEmotion.NO && emotion != QuestNpcEmotion.PANIC) {
			throw new IllegalArgumentException("unsupported interaction NPC emotion: " + emotion);
		}
		Player player = players.find(snapshot.playerId());
		if (player == null || snapshot.interactionObjectId() == 0) {
			return false;
		}
		VisibleObject visible = targets.find(snapshot.interactionObjectId());
		return visible instanceof Npc npc
			&& ai.apply(npc, player, player, Command.BROADCAST_INTERACTION_EMOTION, emotion.name());
	}

	@Override
	public boolean watchFollowZone(QuestSnapshot snapshot, QuestMutationPlan plan, String slot, String zone) {
		if (zone == null || zone.isBlank()) {
			throw new IllegalArgumentException("zone must not be blank");
		}
		Objects.requireNonNull(snapshot, "snapshot");
		Npc npc = registry.get(snapshot, slot);
		Player player = players.find(snapshot.playerId());
		if (npc == null || player == null) {
			return false;
		}
		Future<?> task = follow.start(player, npc, snapshot.questId(), zone);
		if (task == null) {
			return false;
		}
		return registry.registerFollowTask(snapshot, slot, task);
	}

	@Override
	public boolean watchFollowCoordinate(QuestSnapshot snapshot, QuestMutationPlan plan, String slot,
			float x, float y, float z) {
		if (!Float.isFinite(x) || !Float.isFinite(y) || !Float.isFinite(z)) {
			throw new IllegalArgumentException("follow destination coordinates must be finite");
		}
		Objects.requireNonNull(snapshot, "snapshot");
		Npc npc = registry.get(snapshot, slot);
		Player player = players.find(snapshot.playerId());
		if (npc == null || player == null) {
			return false;
		}
		Future<?> task = coordinateFollow.start(player, npc, snapshot.questId(), x, y, z);
		if (task == null) {
			return false;
		}
		return registry.registerFollowTask(snapshot, slot, task);
	}

	@Override
	public boolean watchLuredNpcCoordinate(QuestSnapshot snapshot, QuestMutationPlan plan,
			float x, float y, float z, float radius, QuestLureCompletion completion) {
		Objects.requireNonNull(snapshot, "snapshot");
		Objects.requireNonNull(completion, "completion");
		if (!Float.isFinite(x) || !Float.isFinite(y) || !Float.isFinite(z)
				|| !Float.isFinite(radius) || radius <= 0) {
			throw new IllegalArgumentException("lure destination must be finite and radius must be positive");
		}
		Player player = players.find(snapshot.playerId());
		if (player == null || snapshot.interactionObjectId() <= 0) {
			return false;
		}
		VisibleObject visible = targets.find(snapshot.interactionObjectId());
		return visible instanceof Npc npc
			&& luredNpcWatch.start(player, npc, snapshot.questId(), x, y, z, radius, completion);
	}

	private boolean run(QuestSnapshot snapshot, String slot, Command command) {
		return run(snapshot, slot, command, null);
	}

	private boolean run(QuestSnapshot snapshot, String slot, Command command, String argument) {
		Objects.requireNonNull(snapshot, "snapshot");
		if (slot == null || slot.isBlank()) {
			throw new IllegalArgumentException("slot must not be blank");
		}
		Npc npc = registry.get(snapshot, slot);
		if (npc == null) {
			// 该 slot 无 handle（可能从未 spawn 或已清理）：无法寻址，best-effort 跳过。 / No handle for this slot (never spawned or cleaned up): not addressable, best-effort skip.
			return false;
		}
		Player player = players.find(snapshot.playerId());
		if (player == null) {
			// 玩家已登出：无可跟随/攻击的对象上下文，best-effort 跳过。 / Player logged out: no follow/attack context available, best-effort skip.
			return false;
		}
		VisibleObject target = null;
		if (command == Command.ATTACK_TARGET) {
			if (snapshot.targetObjectId() == 0) {
				return false;
			}
			target = targets.find(snapshot.targetObjectId());
			if (target == null) {
				return false;
			}
		}
		return ai.apply(npc, player, target, command, argument);
	}

	/** 在当前地图实例中选择第一个已生成且存活的目标 NPC。 / Picks the first spawned and living target NPC in the current world instance. */
	private static Npc findLivingNpc(Player player, int templateId) {
		if (player == null || player.getPosition() == null || player.getPosition().getWorldMapInstance() == null) {
			return null;
		}
		List<Npc> npcs = player.getPosition().getWorldMapInstance().getNpcs(templateId);
		for (Npc npc : npcs) {
			if (npc != null && npc.isSpawned()
				&& (npc.getLifeStats() == null || !npc.getLifeStats().isAlreadyDead())) {
				return npc;
			}
		}
		return null;
	}
}
