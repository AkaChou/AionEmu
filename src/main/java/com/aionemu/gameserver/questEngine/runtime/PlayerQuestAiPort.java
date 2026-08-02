package com.aionemu.gameserver.questEngine.runtime;

import com.aionemu.gameserver.ai2.NpcAI2;
import com.aionemu.gameserver.ai2.event.AIEventType;
import com.aionemu.gameserver.ai2.manager.WalkManager;
import com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices;
import com.aionemu.gameserver.model.EmotionType;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.VisibleObject;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_EMOTION;
import com.aionemu.gameserver.questEngine.definition.QuestNpcEmotion;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.task.QuestTasks;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.world.zone.ZoneName;

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
		START_WALKING,
		BROADCAST_START_EMOTE2
	}

	/** 可注入的 AI 命令委托 (生产 = 真实 AI 调用, 测试 = 记录器)。 */
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

	private final QuestPlayerPort players;
	private final QuestSpawnRegistry registry;
	private final AiCall ai;
	private final TargetResolver targets;
	private final FollowCall follow;

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
			case START_WALKING -> WalkManager.startWalking((NpcAI2) npc.getAi2());
			case BROADCAST_START_EMOTE2 -> {
				PacketSendUtility.broadcastPacket(npc,
					new SM_EMOTION(npc, EmotionType.START_EMOTE2, 0, npc.getObjectId()));
				yield true;
			}
		}, objectId -> GameWorldBootstrapServices.world().findVisibleObject(objectId),
		(player, npc, questId, zone) -> QuestTasks.newFollowingToTargetCheckTask(
			new QuestEnv(null, player, questId, 0), npc, ZoneName.get(zone)));
	}

	public PlayerQuestAiPort(QuestPlayerPort players, QuestSpawnRegistry registry, AiCall ai) {
		this(players, registry, ai, objectId -> GameWorldBootstrapServices.world().findVisibleObject(objectId),
			(player, npc, questId, zone) -> QuestTasks.newFollowingToTargetCheckTask(
				new QuestEnv(null, player, questId, 0), npc, ZoneName.get(zone)));
	}

	public PlayerQuestAiPort(QuestPlayerPort players, QuestSpawnRegistry registry, AiCall ai,
			TargetResolver targets, FollowCall follow) {
		this.players = Objects.requireNonNull(players, "players");
		this.registry = Objects.requireNonNull(registry, "registry");
		this.ai = Objects.requireNonNull(ai, "ai");
		this.targets = Objects.requireNonNull(targets, "targets");
		this.follow = Objects.requireNonNull(follow, "follow");
	}

	@Override
	public boolean startFollow(QuestSnapshot snapshot, QuestMutationPlan plan, String slot) {
		return run(snapshot, slot, Command.START_FOLLOW);
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
			// 该 slot 无 handle (可能从未 spawn 或已清理):无法寻址,best-effort 跳过。
			return false;
		}
		Player player = players.find(snapshot.playerId());
		if (player == null) {
			// 玩家已登出:无可跟随/攻击的对象上下文,best-effort 跳过。
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
}
