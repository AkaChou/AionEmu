package com.aionemu.gameserver.questEngine.handlers.template;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.skill.PlayerSkillEntry;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SKILL_LIST;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.services.craft.CraftSkillUpdateService;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * 制作技能奖励任务模板：完成任务后提升对应制作技能等级，可在可选电影结束后再授予技能。
 * Crafting skill reward quest template: raises the craft skill level on completion, optionally after a quest movie ends.
 */
public class CraftingRewards extends QuestHandler {
	/** 任务 ID / quest id */
	private final int questId;
	/** 起始 NPC ID / start NPC id */
	private final int startNpcId;
	/** 奖励的制作技能 ID / craft skill id to reward */
	private final int skillId;
	/** 奖励后的技能等级 / rewarded skill level */
	private final int levelReward;
	/** 完成时播放的任务电影 ID，0 表示无 / quest movie id on complete, 0 if none */
	private final int questMovie;
	/** 结束/交任务 NPC ID，0 时回退为起始 NPC / end NPC id, falls back to startNpcId when 0 */
	private final int endNpcId;

	/**
	 * 构造制作技能奖励任务处理器。
	 * Constructs a crafting-rewards quest handler.
	 *
	 * quest id
	 * start NPC id
	 * craft skill id
	 * @param levelReward 奖励技能等级 / rewarded skill level
	 * @param endNpcId 结束 NPC ID，0 则使用起始 NPC / end NPC id, 0 uses startNpcId
	 * @param questMovie 任务电影 ID，0 表示无 / quest movie id, 0 if none
	 */
	public CraftingRewards(int questId, int startNpcId, int skillId, int levelReward, int endNpcId, int questMovie) {
		super(questId);
		this.questId = questId;
		this.startNpcId = startNpcId;
		this.skillId = skillId;
		this.levelReward = levelReward;
		if (endNpcId != 0) {
			this.endNpcId = endNpcId;
		} else {
			this.endNpcId = startNpcId;
		}
		this.questMovie = questMovie;
	}

	/**
	 * 注册接取/对话 NPC 及可选电影结束事件。
	 * Registers start/talk NPCs and optional movie-end event.
	 */
	@Override
	public void register() {
		qe.registerQuestNpc(startNpcId).addOnQuestStart(questId);
		qe.registerQuestNpc(startNpcId).addOnTalkEvent(questId);
		if (questMovie != 0) {
			qe.registerOnMovieEndQuest(questMovie, questId);
		}
		if (endNpcId != startNpcId) {
			qe.registerQuestNpc(endNpcId).addOnTalkEvent(questId);
		}
	}

	/**
	 * 处理接取、交任务与奖励对话；在选择奖励时授予技能或播放电影。
	 * Handles accept, turn-in and reward dialogs; grants the skill or plays the movie on reward select.
	 *
	 * @param env 任务环境 / quest environment
	 * @return 是否已处理该对话事件 / whether the dialog event was handled
	 */
	@Override
	public boolean onDialogEvent(QuestEnv env) {
		Player player = env.getPlayer();
		QuestState qs = player.getQuestStateList().getQuestState(questId);
		QuestDialog dialog = env.getDialog();
		int targetId = env.getTargetId();
		PlayerSkillEntry skill = player.getSkillList().getSkillEntry(skillId);
		if (skill != null) {
			int playerSkillLevel = skill.getSkillLevel();
			if (!canLearn(player) && playerSkillLevel != levelReward) {
				return false;
			}
		}
		if (qs == null || qs.getStatus() == QuestStatus.NONE) {
			if (targetId == startNpcId) {
				switch (dialog) {
				case START_DIALOG: {
					return sendQuestDialog(env, 1011);
				}
				default: {
					return sendQuestStartDialog(env);
				}
				}
			}
		} else if (qs.getStatus() == QuestStatus.START) {
			if (targetId == endNpcId) {
				switch (dialog) {
				case START_DIALOG: {
					return sendQuestDialog(env, 2375);
				}
				case SELECT_REWARD: {
					qs.setQuestVar(0);
					qs.setStatus(QuestStatus.REWARD);
					updateQuestStatus(env);
					if (questMovie != 0) {
						playQuestMovie(env, questMovie);
					} else {
						player.getSkillList().addSkill(player, skillId, levelReward);
					}
					return sendQuestEndDialog(env);
				}
				}
			}
		} else if (qs.getStatus() == QuestStatus.REWARD) {
			if (targetId == endNpcId) {
				switch (dialog) {
				case START_DIALOG: {
					return sendQuestEndDialog(env);
				}
				default: {
					return sendQuestEndDialog(env);
				}
				}
			}
		}
		return false;
	}

	/**
	 * 判断玩家是否可学习目标等级的制作技能（专家/大师位限制）。
	 * Checks whether the player may learn the target craft skill level (expert/master slot limits).
	 *
	 * 玩家 / player
	 * @return 是否可学习 / whether learning is allowed
	 */
	private boolean canLearn(Player player) {
		return levelReward == 400 ? CraftSkillUpdateService.canLearnMoreExpertCraftingSkill(player)
				: levelReward == 500 ? CraftSkillUpdateService.canLearnMoreMasterCraftingSkill(player) : true;
	}

	/**
	 * 处理任务电影结束事件：授予技能、自动学习配方并同步技能列表。
	 * Handles the quest movie end event: grants the skill, auto-learns recipes and syncs the skill list.
	 *
	 * @param env 任务环境 / quest environment
	 * @param movieId 结束的电影 ID / finished movie id
	 * @return 是否已处理 / whether the event was handled
	 */
	@Override
	public boolean onMovieEndEvent(QuestEnv env, int movieId) {
		Player player = env.getPlayer();
		QuestState qs = player.getQuestStateList().getQuestState(questId);
		if (qs.getStatus() == QuestStatus.REWARD) {
			if (movieId == questMovie && canLearn(player)) {
				player.getSkillList().addSkill(player, skillId, levelReward);
				player.getRecipeList().autoLearnRecipe(player, skillId, levelReward);
				PacketSendUtility.sendPacket(player,
						new SM_SKILL_LIST(player.getSkillList().getSkillEntry(skillId), 1330064, false));
				return true;
			}
		}
		return false;
	}
}
