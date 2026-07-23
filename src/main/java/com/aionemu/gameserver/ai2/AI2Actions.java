package com.aionemu.gameserver.ai2;

import com.aionemu.gameserver.lifecycle.GameEngineServices;

import java.util.Collection;

import com.aionemu.gameserver.controllers.observer.DialogObserver;
import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.lifecycle.GameWorldServices;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.gameobjects.player.RequestResponseHandler;
import com.aionemu.gameserver.model.skill.NpcSkillEntry;
import com.aionemu.gameserver.model.skill.NpcSkillList;
import com.aionemu.gameserver.network.aion.serverpackets.SM_QUESTION_WINDOW;
import com.aionemu.gameserver.questEngine.QuestEngine;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.scriptEngine.ScriptNpc;
import com.aionemu.gameserver.skillengine.model.Effect;
import com.aionemu.gameserver.skillengine.model.SkillTemplate;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * AI2 常用动作工具类，封装对 AI 所有者的通用操作。
 * Common AI2 action helpers with access to the AI owner's operations.
 *
 * @author ATracer
 */
public class AI2Actions {

	/**
	 * 删除并清理 AI 所有者。
	 * Despawns and deletes the AI owner.
	 *
	 * @param ai2 AI 实例 / AI instance
	 */
	public static void deleteOwner(AbstractAI ai2) {
		ai2.getOwner().getController().onDelete();
	}

	/**
	 * 以 AI 所有者为最后攻击者静默击杀目标。
	 * Silently kills the target using the AI owner as the last attacker.
	 *
	 * @param ai2 AI 实例 / AI instance
	 * target creature
	 */
	public static void killSilently(AbstractAI ai2, Creature target) {
		target.getController().onDie(ai2.getOwner());
	}

	/**
	 * 让 AI 所有者被指定攻击者静默击杀。
	 * Silently kills the AI owner by the given attacker.
	 *
	 * @param ai2 AI 实例 / AI instance
	 * attacker
	 */
	public static void dieSilently(AbstractAI ai2, Creature attacker) {
		ai2.getOwner().getController().onDie(attacker);
	}

	/**
	 * 使用技能（或加入使用意图，后续可扩展）。
	 * Uses a skill (or queues an intention to use it later).
	 *
	 * @param ai2 AI 实例 / AI instance
	 * skill id
	 */
	public static void useSkill(AbstractAI ai2, int skillId) {
		ai2.getOwner().getController().useSkill(skillId);
	}

	/**
	 * 创建并强制对目标应用技能效果（100% 成功）。
	 * Creates and force-applies a skill effect to the target with 100% success.
	 *
	 * @param ai2 AI 实例 / AI instance
	 * skill template
	 * target creature
	 */
	public static void applyEffect(AbstractAI ai2, SkillTemplate template, Creature target) {
		Effect effect = new Effect(ai2.getOwner(), target, template, template.getLvl(), 0);
		effect.setIsForcedEffect(true);
		effect.initialize();
		effect.applyEffect();
	}

	/**
	 * 对 AI 所有者自身应用技能效果。
	 * Applies a skill effect to the AI owner itself.
	 *
	 * @param ai2 AI 实例 / AI instance
	 * skill id
	 */
	public static void applyEffectSelf(AbstractAI ai2, int skillId) {
		SkillTemplate st = DataManager.SKILL_DATA.getSkillTemplate(skillId);
		Effect effect = new Effect(ai2.getOwner(), ai2.getOwner(), st, 1, st.getEffectsDuration(skillId));
		effect.initialize();
		effect.applyEffect();
	}

	/**
	 * 将目标设为自身。
	 * Sets the target to self.
	 *
	 * @param ai2 AI 实例 / AI instance
	 */
	public static void targetSelf(AbstractAI ai2) {
		ai2.getOwner().setTarget(ai2.getOwner());
	}

	/**
	 * 将目标设为指定生物。
	 * Sets the target to the given creature.
	 *
	 * @param ai2 AI 实例 / AI instance
	 * target creature
	 */
	public static void targetCreature(AbstractAI ai2, Creature target) {
		ai2.getOwner().setTarget(target);
	}

	/**
	 * 通知副本处理器：玩家完成使用物品/交互。
	 * Notifies the instance handler that a player finished using an item/interaction.
	 *
	 * @param ai2 AI 实例 / AI instance
	 * 玩家 / player
	 */
	public static void handleUseItemFinish(AbstractAI ai2, Player player) {
		Npc npc = (Npc) ai2.getOwner();
		if (dispatchScriptNpcItemUseFinish(npc.getNpcId(), player, npc)) {
			return;
		}
		if (dispatchRetailProtectBuffItemUse(npc, player)) {
			return;
		}
		ai2.getPosition().getWorldMapInstance().getInstanceHandler().handleUseItemFinish(player, npc);
	}

	/**
	 * Dispatches the common ScriptNpc consumer before the legacy instance fallback.
	 */
	static boolean dispatchScriptNpcItemUseFinish(int npcId, Player player, Npc npc) {
		ScriptNpc script = GameEngineServices.scriptEngine().getRegistry().getScriptNpc(npcId);
		return script != null && script.onItemUseFinish(player, npc);
	}

	/** Executes the retail protect-buff action from the NPC AI and skill slot data. */
	static boolean dispatchRetailProtectBuffItemUse(Npc npc, Player player) {
		NpcSkillEntry selected = selectRetailProtectBuffSkill(npc.getObjectTemplate().getAi(), npc.getSkillList());
		if (selected == null || DataManager.SKILL_DATA.getSkillTemplate(selected.getSkillId()) == null) {
			return false;
		}
		GameEngineServices.skillEngine().getSkill(npc, selected.getSkillId(), selected.getSkillLevel(), player)
			.useNoAnimationSkill();
		return true;
	}

	static NpcSkillEntry selectRetailProtectBuffSkill(String aiName, NpcSkillList skills) {
		if (!"NPC_AI_ProtectBuff".equals(aiName)) {
			return null;
		}
		NpcSkillEntry selected = null;
		for (int i = 0; i < skills.size(); i++) {
			NpcSkillEntry entry = skills.getSkillByIndex(i);
			if (entry == null) {
				continue;
			}
			if (selected != null || entry.getSkillId() <= 0 || entry.getSkillLevel() <= 0) {
				return null;
			}
			selected = entry;
		}
		return selected;
	}

	/**
	 * 向目标 NPC 触发个体事件。
	 * Fires an individual NPC event on the target.
	 *
	 * @param ai2 AI 实例 / AI instance
	 * target NPC
	 */
	public static void fireIndividualEvent(AbstractAI ai2, Npc target) {
		target.getAi2().onIndividualNpcEvent(ai2.getOwner());
	}

	/**
	 * 触发副本内 NPC 死亡事件。
	 * Fires an instance NPC-kill event.
	 *
	 * @param ai2 AI 实例 / AI instance
	 * related player
	 */
	public static void fireNpcKillInstanceEvent(AbstractAI ai2, Player player) {
		ai2.getPosition().getWorldMapInstance().getInstanceHandler().onDie((Npc) ai2.getOwner());
	}

	/**
	 * 注册掉落分配。
	 * Registers drop distribution.
	 *
	 * @param ai2 AI 实例 / AI instance
	 * main player
	 * @param registeredPlayers 已登记玩家集合 / registered players
	 */
	public static void registerDrop(AbstractAI ai2, Player player, Collection<Player> registeredPlayers) {
		GameWorldServices.dropRegistrationService().registerDrop((Npc) ai2.getOwner(), player, registeredPlayers);
	}

	/**
	 * 安排 NPC 重生。
	 * Schedules NPC respawn.
	 *
	 * NPC AI instance
	 */
	public static void scheduleRespawn(NpcAI2 ai2) {
		ai2.getOwner().getController().scheduleRespawn();
	}

	/**
	 * 处理任务对话框选择并返回结果。
	 * Processes quest dialog selection and returns the result.
	 *
	 * @param ai2 AI 实例 / AI instance
	 * 玩家 / player
	 * quest id
	 * dialog id
	 *
	 * @return 对话框选择结果 / dialog selection result
	 */
	public static SelectDialogResult selectDialog(AbstractAI ai2, Player player, int questId, int dialogId) {
		QuestEnv env = new QuestEnv(ai2.getOwner(), player, questId, dialogId);
		boolean result = GameEngineServices.questEngine().onDialog(env);
		return new SelectDialogResult(result, env);
	}

	/**
	 * 对话框选择结果：是否成功及对应任务环境。
	 * Dialog selection result: success flag and related quest environment.
	 */
	public static final class SelectDialogResult {
		private final boolean success;
		private final QuestEnv env;

		private SelectDialogResult(boolean success, QuestEnv env) {
			this.success = success;
			this.env = env;
		}

		/**
		 * 是否处理成功。
		 * Returns whether the dialog was handled successfully.
		 *
		 * whether successful
		 */
		public boolean isSuccess() {
			return success;
		}

		/**
		 * 获取任务环境。
		 * Returns the quest environment.
		 *
		 * quest environment
		 */
		public QuestEnv getEnv() {
			return env;
		}
	}

	/**
	 * 向玩家添加请求响应处理器，senderId 默认为 AI 所有者 objectId。
	 * Adds a request/response handler to the player with senderId defaulting to the AI owner's objectId.
	 *
	 * @param ai2 AI 实例 / AI instance
	 * 玩家 / player
	 * request id
	 * request callback
	 * request parameters
	 */
	public static void addRequest(AbstractAI ai2, Player player, int requestId, AI2Request request,
			Object... requestParams) {
		addRequest(ai2, player, requestId, ai2.getObjectId(), request, requestParams);
	}

	/**
	 * 向玩家添加请求响应处理器；超出范围时取消请求。
	 * Adds a request/response handler to the player; cancels when moving out of range.
	 *
	 * @param ai2 AI 实例 / AI instance
	 * 玩家 / player
	 * request id
	 * sender id
	 * @param range 有效范围（0 表示不限） / valid range (0 = unlimited)
	 * request callback
	 * request parameters
	 */
	public static void addRequest(AbstractAI ai2, Player player, int requestId, int senderId, int range,
			final AI2Request request, Object... requestParams) {

		boolean requested = player.getResponseRequester().putRequest(requestId,
				new RequestResponseHandler(ai2.getOwner()) {

					@Override
					public void denyRequest(Creature requester, Player responder) {
						request.denyRequest(requester, responder);
					}

					@Override
					public void acceptRequest(Creature requester, Player responder) {
						request.acceptRequest(requester, responder);
					}
				});

		if (requested) {
			if (range > 0) {
				player.getObserveController().addObserver(new DialogObserver(ai2.getOwner(), player, range) {

					public void tooFar(Creature requester, Player responder) {
						request.denyRequest(requester, responder);
					}
				});
			}
			PacketSendUtility.sendPacket(player, new SM_QUESTION_WINDOW(requestId, senderId, range, requestParams));
		}
	}

	/**
	 * 向玩家添加请求响应处理器（不限制范围）。
	 * Adds a request/response handler to the player (no range limit).
	 *
	 * @param ai2 AI 实例 / AI instance
	 * 玩家 / player
	 * request id
	 * sender id
	 * request callback
	 * request parameters
	 */
	public static void addRequest(AbstractAI ai2, Player player, int requestId, int senderId, final AI2Request request,
			Object... requestParams) {
		addRequest(ai2, player, requestId, senderId, 0, request, requestParams);
	}
}
