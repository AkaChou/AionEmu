package com.aionemu.gameserver.questEngine.handlers.template;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.handlers.models.QuestSkillData;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 技能使用任务模板：对配置技能累计使用次数（6 位打包变量），完成后到结束 NPC 领奖；含短时去抖缓存。
 * Skill-use quest template: counts configured skill uses with 6-bit packed vars, rewards at the end NPC; includes short debounce cache.
 */
public class SkillUse extends QuestHandler {

	/** 任务 ID / quest id */
	private final int questId;
	/** 起始 NPC ID / start NPC id */
	private final int startNpc;
	/** 结束 NPC ID / end NPC id */
	private final int endNpc;
	/** 技能 ID 列表到配置的映射 / map of skill-id lists to config */
	private final Map<List<Integer>, QuestSkillData> qsd;
	/** 技能 usedebouncecachekeylastusetime / skill-use debounce cache: key → last use time */
	private final Map<String, Long> lastSkillUseCache = new HashMap<String, Long>();

	/**
	 * 构造技能使用任务处理器。
	 * Constructs a skill-use quest handler.
	 *
	 * quest id
	 * start NPC
	 * @param endNpc 结束 NPC，0 则使用起始 NPC / end NPC, 0 uses startNpc
	 * @param qsd 技能配置映射 / skill config map
	 */
	public SkillUse(int questId, int startNpc, int endNpc, Map<List<Integer>, QuestSkillData> qsd) {
		super(questId);
		this.questId = questId;
		this.startNpc = startNpc;
		if (endNpc != 0) {
			this.endNpc = endNpc;
		} else {
			this.endNpc = startNpc;
		}
		this.qsd = qsd;
	}

	/**
	 * 注册接取/对话 NPC 与技能使用监听。
	 * Registers start/talk NPCs and skill-use listeners.
	 */
	@Override
	public void register() {
		qe.registerQuestNpc(startNpc).addOnQuestStart(questId);
		qe.registerQuestNpc(startNpc).addOnTalkEvent(questId);
		if (endNpc != startNpc) {
			qe.registerQuestNpc(endNpc).addOnTalkEvent(questId);
		}
		for (List<Integer> skillIds : qsd.keySet()) {
			Iterator<Integer> iterator = skillIds.iterator();
			while (iterator.hasNext()) {
				int SkillId = iterator.next();
				qe.registerQuestSkill(SkillId, questId);
			}
		}
	}

	/**
	 * 处理接取、交任务与奖励对话事件。
	 * Handles accept, turn-in and reward dialog events.
	 *
	 * @param env 任务环境 / quest environment
	 * @return 是否已处理 / whether the dialog event was handled
	 */
	@Override
	public boolean onDialogEvent(QuestEnv env) {
		Player player = env.getPlayer();
		QuestState qs = player.getQuestStateList().getQuestState(questId);
		QuestDialog dialog = env.getDialog();
		int targetId = env.getTargetId();
		if (qs == null || qs.getStatus() == QuestStatus.NONE || qs.canRepeat()) {
			if (targetId == startNpc) {
				if (dialog == QuestDialog.START_DIALOG) {
					return sendQuestDialog(env, 4762);
				} else {
					return sendQuestStartDialog(env);
				}
			}
		} else if (qs.getStatus() == QuestStatus.START) {
			int var = qs.getQuestVarById(0);
			if (targetId == endNpc) {
				if (dialog == QuestDialog.START_DIALOG) {
					return sendQuestDialog(env, 10002);
				} else if (dialog == QuestDialog.SELECT_REWARD) {
					changeQuestStep(env, var, var, true);
					return sendQuestDialog(env, 5);
				}
			}
		} else if (qs.getStatus() == QuestStatus.REWARD) {
			if (targetId == endNpc) {
				return sendQuestEndDialog(env);
			}
		}
		return false;
	}

	/**
	 * 处理技能使用事件：去抖后按配置累加 6 位打包变量。
	 * Handles skill-use events: after debounce, increments 6-bit packed vars per config.
	 *
	 * @param env 任务环境 / quest environment
	 * @param skillId 使用的技能 ID / used skill id
	 * @return 是否已处理 / whether the skill-use event was handled
	 */
	@Override
	public boolean onUseSkillEvent(QuestEnv env, int skillId) {
		Player player = env.getPlayer();
		QuestState qs = player.getQuestStateList().getQuestState(questId);

		if (qs != null && qs.getStatus() == QuestStatus.START) {
			String cacheKey = player.getObjectId() + "_" + skillId + "_" + questId;
			Long lastUsed = lastSkillUseCache.get(cacheKey);
			long currentTime = System.currentTimeMillis();

			if (lastUsed != null && (currentTime - lastUsed) < 500) {
				return false;
			}

			lastSkillUseCache.put(cacheKey, currentTime);

			if (lastSkillUseCache.size() > 1000) {
				Iterator<Map.Entry<String, Long>> iterator = lastSkillUseCache.entrySet().iterator();
				long cleanupTime = currentTime - 30000;
				while (iterator.hasNext()) {
					if (iterator.next().getValue() < cleanupTime) {
						iterator.remove();
					}
				}
			}

			for (QuestSkillData qd : qsd.values()) {
				if (qd.getSkillIds().contains(skillId)) {
					int endVar = qd.getEndVar();
					int varId = qd.getVarNum();
					int total = 0;
					do {
						int currentVar = qs.getQuestVarById(varId);
						total += currentVar << ((varId - qd.getVarNum()) * 6);
						endVar >>= 6;
						varId++;
					} while (endVar > 0);
					total += 1;
					if (total <= qd.getEndVar()) {
						for (int varsUsed = qd.getVarNum(); varsUsed < varId; varsUsed++) {
							int value = total & 0x3F;
							total >>= 6;
							qs.setQuestVarById(varsUsed, value);
						}
						updateQuestStatus(env);
						return true;
					}
				}
			}
		}
		return false;
	}
}
