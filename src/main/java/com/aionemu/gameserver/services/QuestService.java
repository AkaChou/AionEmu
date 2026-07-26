package com.aionemu.gameserver.services;

import lombok.extern.slf4j.Slf4j;
import com.aionemu.gameserver.lifecycle.GameHousingServices;

import com.aionemu.gameserver.lifecycle.GameLocationBootstrapServices;

import com.aionemu.gameserver.lifecycle.GameFeatureServices;

import com.aionemu.gameserver.lifecycle.GameEngineServices;

import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import java.sql.Timestamp;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Future;

import com.aionemu.commons.utils.Rnd;
import com.aionemu.gameserver.configs.main.CustomConfig;
import com.aionemu.gameserver.configs.main.GroupConfig;
import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.dataholders.QuestsData;
import com.aionemu.gameserver.lifecycle.GameWorldServices;
import com.aionemu.gameserver.model.DescriptionId;
import com.aionemu.gameserver.model.DialogAction;
import com.aionemu.gameserver.model.PlayerClass;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.TaskId;
import com.aionemu.gameserver.model.drop.Drop;
import com.aionemu.gameserver.model.drop.DropItem;
import com.aionemu.gameserver.model.gameobjects.DropNpc;
import com.aionemu.gameserver.model.gameobjects.Item;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.VisibleObject;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.gameobjects.player.QuestStateList;
import com.aionemu.gameserver.model.gameobjects.player.RewardType;
import com.aionemu.gameserver.model.gameobjects.player.npcFaction.NpcFaction;
import com.aionemu.gameserver.model.items.ItemId;
import com.aionemu.gameserver.model.landing.LandingPointsEnum;
import com.aionemu.gameserver.model.skill.PlayerSkillEntry;
import com.aionemu.gameserver.model.team2.alliance.PlayerAlliance;
import com.aionemu.gameserver.model.team2.common.legacy.LootRuleType;
import com.aionemu.gameserver.model.team2.group.PlayerGroup;
import com.aionemu.gameserver.model.templates.QuestTemplate;
import com.aionemu.gameserver.model.templates.npc.NpcTemplate;
import com.aionemu.gameserver.model.templates.quest.CollectItem;
import com.aionemu.gameserver.model.templates.quest.CollectItems;
import com.aionemu.gameserver.model.templates.quest.HandlerSideDrop;
import com.aionemu.gameserver.model.templates.quest.InventoryItem;
import com.aionemu.gameserver.model.templates.quest.InventoryItems;
import com.aionemu.gameserver.model.templates.quest.QuestBonuses;
import com.aionemu.gameserver.model.templates.quest.QuestCategory;
import com.aionemu.gameserver.model.templates.quest.QuestDrop;
import com.aionemu.gameserver.model.templates.quest.QuestItems;
import com.aionemu.gameserver.model.templates.quest.QuestMentorType;
import com.aionemu.gameserver.model.templates.quest.QuestRepeatCycle;
import com.aionemu.gameserver.model.templates.quest.QuestTargetType;
import com.aionemu.gameserver.model.templates.quest.QuestWorkItems;
import com.aionemu.gameserver.model.templates.quest.Rewards;
import com.aionemu.gameserver.model.templates.quest.XMLStartCondition;
import com.aionemu.gameserver.network.aion.serverpackets.SM_DIALOG_WINDOW;
import com.aionemu.gameserver.network.aion.serverpackets.SM_LOOT_STATUS;
import com.aionemu.gameserver.network.aion.serverpackets.SM_LOOT_STATUS.Status;
import com.aionemu.gameserver.network.aion.serverpackets.SM_QUEST_ACTION;
import com.aionemu.gameserver.network.aion.serverpackets.SM_STATS_INFO;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.questEngine.QuestEngine;
import com.aionemu.gameserver.questEngine.handlers.HandlerResult;
import com.aionemu.gameserver.questEngine.handlers.models.WorkOrdersData;
import com.aionemu.gameserver.questEngine.handlers.models.XMLQuest;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.services.abyss.AbyssPointsService;
import com.aionemu.gameserver.services.craft.CraftSkillUpdateService;
import com.aionemu.gameserver.services.item.ItemPacketService.ItemUpdateType;
import com.aionemu.gameserver.services.item.ItemService;
import com.aionemu.gameserver.spawnengine.SpawnEngine;
import com.aionemu.gameserver.utils.MathUtil;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.audit.AuditLogger;
import com.aionemu.gameserver.utils.stats.AbyssRankEnum;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
/**
 * 任务服务，处理任务开始/完成、掉落、计时器与放弃等核心流程。
 * Quest service handling start/finish, drops, timers, abandon, and related core flows.
 */
@Slf4j
public final class QuestService {

	/** 任务静态数据。 / Quest static data. */
	static QuestsData questsData = DataManager.QUEST_DATA;
	/** NPC ID 到任务掉落条目的 Multimap / Multimap of NPC id to quest drop entries */
	private static Multimap<Integer, QuestDrop> questDrop = ArrayListMultimap.create();

	/**
	 * 清空全部任务掉落缓存。
	 * Clears all quest-drop cache entries.
	 */
	public static void clearQuestDrops() {
		questDrop.clear();
	}

	/**
	 * 以默认奖励索引完成任务。
	 * Finishes the quest with the default reward index.
	 *
	 * @param env 任务环境 / quest environment
	 * whether successful
	 */
	public static boolean finishQuest(QuestEnv env) {
		return finishQuest(env, 0);
	}

	public static boolean finishReportedQuest(Player player, int questId, int dialogActionId) {
		QuestTemplate template = questsData.getQuestById(questId);
		QuestState state = player.getQuestStateList().getQuestState(questId);
		int rewardDialogId = reportedRewardDialogId(dialogActionId);
		if (!canFinishReportedQuest(template, state, dialogActionId)) {
			return false;
		}
		if (!finishQuest(new QuestEnv(null, player, questId, rewardDialogId))) {
			return false;
		}
		PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(0, 0));
		return true;
	}

	static boolean canFinishReportedQuest(QuestTemplate template, QuestState state, int dialogActionId) {
		return reportedRewardDialogId(dialogActionId) >= 0 && template != null && template.isCanReport()
			&& state != null && state.getStatus() == QuestStatus.REWARD;
	}

	static int reportedRewardDialogId(int dialogActionId) {
		if (dialogActionId == DialogAction.AUTO_REWARD.id()) {
			return 0;
		}
		int first = DialogAction.QUEST_AUTO_REWARD_1.id();
		int last = DialogAction.QUEST_AUTO_REWARD_15.id();
		return dialogActionId >= first && dialogActionId <= last ? dialogActionId - first + 8 : -1;
	}

	public static boolean isReportedRewardAction(int dialogActionId) {
		return reportedRewardDialogId(dialogActionId) >= 0;
	}

	/**
	 * 完成任务并发放指定奖励。
	 * Finishes the quest and grants the specified reward.
	 *
	 * @param env 任务环境 / quest environment
	 * reward index
	 * whether successful
	 */
	public static boolean finishQuest(QuestEnv env, int reward) {
		Player player = env.getPlayer();
		int id = env.getQuestId();
		QuestState qs = player.getQuestStateList().getQuestState(id);
		Rewards rewards = new Rewards();
		Rewards extendedRewards = new Rewards();
		if (qs == null || qs.getStatus() != QuestStatus.REWARD) {
			return false;
		}
		QuestTemplate template = questsData.getQuestById(id);
		if (template.getCategory() == QuestCategory.MISSION && qs.getCompleteCount() != 0) {
			return false;
		}
		List<QuestItems> questItems = new ArrayList<QuestItems>();
		if (!template.getExtendedRewards().isEmpty()) {
			if (qs.getCompleteCount() == template.getMaxRepeatCount() - 1) {
				questItems.addAll(getRewardItems(env, template, true, reward));
				extendedRewards = template.getExtendedRewards().get(0);
			}
		}
		if (!template.getRewards().isEmpty() || !template.getBonus().isEmpty()) {
			questItems.addAll(getRewardItems(env, template, false, reward));
			rewards = template.getRewards().get(reward);
		}
		if (ItemService.addQuestItems(player, questItems)) {
			giveReward(env, rewards);
			giveReward(env, extendedRewards);
			if (template.getCategory() == QuestCategory.CHALLENGE_TASK) {
				GameHousingServices.challengeTaskService().onChallengeQuestFinish(player, id);
			}
			return setFinishingState(env, template, reward);
		}
		if (player.getInventory().isFull()) {
			PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_WAREHOUSE_FULL_INVENTORY);
			return false;
		}
		return false;
	}

	private static List<QuestItems> getRewardItems(QuestEnv env, QuestTemplate template, boolean extended, int reward) {
		Player player = env.getPlayer();
		int id = env.getQuestId();
		List<QuestItems> questItems = new ArrayList<QuestItems>();
		Rewards rewards;
		if (extended) {
			rewards = template.getExtendedRewards().get(0);
		} else {
			rewards = template.getRewards().get(reward);
		}
		questItems.addAll(rewards.getRewardItem());
		int dialogId = env.getDialogId();
		if (dialogId != 23 && dialogId != 0 && !extended) {
			QuestState qs = player.getQuestStateList().getQuestState(id);
			boolean isLastRepeat = qs.getCompleteCount() == template.getMaxRepeatCount() - 1 && template.getMaxRepeatCount() < 255;
			if (isLastRepeat && template.isUseSingleClassReward() || template.isUseRepeatedClassReward()) {
				QuestItems classRewardItem = null;
				PlayerClass playerClass = player.getCommonData().getPlayerClass();
				int selRewIndex = dialogId - 8;
				switch (playerClass) {
				case ASSASSIN: {
					classRewardItem = getQuestItemsbyClass(id, template.getAssassinSelectableReward(), selRewIndex);
					break;
				}
				case CHANTER: {
					classRewardItem = getQuestItemsbyClass(id, template.getChanterSelectableReward(), selRewIndex);
					break;
				}
				case CLERIC: {
					classRewardItem = getQuestItemsbyClass(id, template.getPriestSelectableReward(), selRewIndex);
					break;
				}
				case GLADIATOR: {
					classRewardItem = getQuestItemsbyClass(id, template.getFighterSelectableReward(), selRewIndex);
					break;
				}
				case RANGER: {
					classRewardItem = getQuestItemsbyClass(id, template.getRangerSelectableReward(), selRewIndex);
					break;
				}
				case SORCERER: {
					classRewardItem = getQuestItemsbyClass(id, template.getWizardSelectableReward(), selRewIndex);
					break;
				}
				case SPIRIT_MASTER: {
					classRewardItem = getQuestItemsbyClass(id, template.getElementalistSelectableReward(), selRewIndex);
					break;
				}
				case TEMPLAR: {
					classRewardItem = getQuestItemsbyClass(id, template.getKnightSelectableReward(), selRewIndex);
					break;
				}
				case GUNSLINGER: {
					classRewardItem = getQuestItemsbyClass(id, template.getGunslingerSelectableReward(), selRewIndex);
					break;
				}
				case SONGWEAVER: {
					classRewardItem = getQuestItemsbyClass(id, template.getSongweaverSelectableReward(), selRewIndex);
					break;
				}
				case AETHERTECH: {
					classRewardItem = getQuestItemsbyClass(id, template.getAethertechSelectableReward(), selRewIndex);
					break;
				}
				default:
					break;
				}
				if (classRewardItem != null) {
					questItems.add(classRewardItem);
				}
			} else {
				QuestItems selectebleRewardItem = null;
				if (dialogId - 8 >= 0 && dialogId - 8 < rewards.getSelectableRewardItem().size()) {
					selectebleRewardItem = rewards.getSelectableRewardItem().get(dialogId - 8);
				}
				if (selectebleRewardItem != null) {
					questItems.add(selectebleRewardItem);
				}
			}
		} else if (dialogId == 23 && dialogId != 0 && !extended) {
			QuestState qs = player.getQuestStateList().getQuestState(id);
			boolean isLastRepeat = qs.getCompleteCount() == template.getMaxRepeatCount() - 1 && template.getMaxRepeatCount() < 255;
			if (isLastRepeat && template.isUseSingleClassReward() || template.isUseRepeatedClassReward()) {
				QuestItems classRewardItem = null;
				PlayerClass playerClass = player.getCommonData().getPlayerClass();
				int selRewIndex = env.getExtendedRewardIndex() - 8;
				switch (playerClass) {
				case ASSASSIN: {
					classRewardItem = getQuestItemsbyClass(id, template.getAssassinSelectableReward(), selRewIndex);
					break;
				}
				case CHANTER: {
					classRewardItem = getQuestItemsbyClass(id, template.getChanterSelectableReward(), selRewIndex);
					break;
				}
				case CLERIC: {
					classRewardItem = getQuestItemsbyClass(id, template.getPriestSelectableReward(), selRewIndex);
					break;
				}
				case GLADIATOR: {
					classRewardItem = getQuestItemsbyClass(id, template.getFighterSelectableReward(), selRewIndex);
					break;
				}
				case RANGER: {
					classRewardItem = getQuestItemsbyClass(id, template.getRangerSelectableReward(), selRewIndex);
					break;
				}
				case SORCERER: {
					classRewardItem = getQuestItemsbyClass(id, template.getWizardSelectableReward(), selRewIndex);
					break;
				}
				case SPIRIT_MASTER: {
					classRewardItem = getQuestItemsbyClass(id, template.getElementalistSelectableReward(), selRewIndex);
					break;
				}
				case TEMPLAR: {
					classRewardItem = getQuestItemsbyClass(id, template.getKnightSelectableReward(), selRewIndex);
					break;
				}
				case AETHERTECH: {
					classRewardItem = getQuestItemsbyClass(id, template.getAethertechSelectableReward(), selRewIndex);
					break;
				}
				case GUNSLINGER: {
					classRewardItem = getQuestItemsbyClass(id, template.getGunslingerSelectableReward(), selRewIndex);
					break;
				}
				case SONGWEAVER: {
					classRewardItem = getQuestItemsbyClass(id, template.getSongweaverSelectableReward(), selRewIndex);
					break;
				}
				default:
					break;
				}
				if (classRewardItem != null) {
					questItems.add(classRewardItem);
				}
			}
		} else if (dialogId == 23 && extended && !rewards.getSelectableRewardItem().isEmpty()) {
			QuestItems selectebleRewardItem = null;
			int index = env.getExtendedRewardIndex();
			if (index - 8 >= 0 && index - 8 < rewards.getSelectableRewardItem().size()) {
				selectebleRewardItem = rewards.getSelectableRewardItem().get(index - 8);
			} else if ((index - 1) >= 0 && (index - 1) < rewards.getSelectableRewardItem().size()) {
				selectebleRewardItem = rewards.getSelectableRewardItem().get(index - 1);
			}
			if (selectebleRewardItem != null) {
				questItems.add(selectebleRewardItem);
			}
		}
		if (!template.getBonus().isEmpty()) {
			QuestBonuses bonus = template.getBonus().get(0);
			// 处理器可在重复时添加额外奖励（活动任务无数据）。 / Handler can add additional bonuses on repeat (for event quests no data)
			HandlerResult result = GameEngineServices.questEngine().onBonusApplyEvent(env, bonus.getType(), questItems);
			if (result != HandlerResult.FAILED) {
				QuestItems additional = GameFeatureServices.bonusService().getQuestBonus(player, template);
				if (additional != null) {
					questItems.add(additional);
				}
			}
		}
		return questItems;
	}

	private static void giveReward(QuestEnv env, Rewards rewards) {
		Player player = env.getPlayer();
		if (rewards.getGold() != null) {
			player.getInventory().increaseKinah((long) (player.getRates().getQuestKinahRate() * rewards.getGold()), ItemUpdateType.INC_KINAH_QUEST);
		}
		if (rewards.getExp() != null) {
			NpcTemplate npcTemplate = DataManager.NPC_DATA.getNpcTemplate(env.getTargetId());
			player.getCommonData().addExp(rewards.getExp(), RewardType.QUEST);
		}
		// 成长光环 / Aura Of Growth
		if (rewards.getExpBoost() != null) {
			player.getCommonData().addAuraOfGrowth(1060000 * rewards.getExpBoost());
		}
		// CP 奖励 5.3 / CP Reward 5.3
		if (rewards.getCP() != null) {
			// To Do...
		}
		// 欧比斯登陆 4.9.1 / Abyss Landing 4.9.1
		if (rewards.getAbyssOp() != null) {
			GameLocationBootstrapServices.abyssLandingService().AnnounceToPoints(player, null, null, rewards.getAbyssOp(), LandingPointsEnum.QUEST);
			if (player.getRace() == Race.ASMODIANS) {
				GameLocationBootstrapServices.abyssLandingService().updateHarbingerLanding(rewards.getAbyssOp(), LandingPointsEnum.QUEST, true);
			}
			if (player.getRace() == Race.ELYOS) {
				GameLocationBootstrapServices.abyssLandingService().updateRedemptionLanding(rewards.getAbyssOp(), LandingPointsEnum.QUEST, true);
			}
		}
		// 玩家完成任务现可获得“DP”。 / Now player can win "Dp" if finish quest.
		if (rewards.getDp() != null) {
			player.getCommonData().addDp(rewards.getDp());
		}
		if (rewards.getTitle() != null) {
			player.getTitleList().addTitle(rewards.getTitle(), true, 0);
		}
		if (rewards.getAp() != null) { // Abyss Points
			AbyssPointsService.addAp(player, (int) (player.getRates().getQuestApRate() * rewards.getAp()));
		}
		if (rewards.getGp() != null) { // Glory Points
			AbyssPointsService.addGp(player, (int) (player.getRates().getQuestGpRate() * rewards.getGp()));
		}
		if (rewards.getExtendInventory() != null) {
			if (rewards.getExtendInventory() == 1) {
				CubeExpandService.expand(player, false);
			} else if (rewards.getExtendInventory() == 2) {
				WarehouseService.expand(player);
			}
		}
		// 发送：成长光环、伯丁眷顾与欧比斯眷顾 / Send for: "Aura Of Growth & Berdin's Favor & Abyss Favor"
		PacketSendUtility.sendPacket(player, new SM_STATS_INFO(player));
	}

	private static boolean setFinishingState(QuestEnv env, QuestTemplate template, int reward) {
		Player player = env.getPlayer();
		int id = env.getQuestId();
		QuestState qs = player.getQuestStateList().getQuestState(id);
		QuestWorkItems qwi = questsData.getQuestById(id).getQuestWorkItems();
		if (qwi != null) {
			long count = 0;
			for (QuestItems qi : qwi.getQuestWorkItem()) {
				if (qi != null) {
					count = player.getInventory().getItemCountByItemId(qi.getItemId());
					if (count > 0) {
						if (!player.getInventory().decreaseByItemId(qi.getItemId(), count)) {
							return false;
						}
					}
				}
			}
		}
		qs.setStatus(QuestStatus.COMPLETE);
		qs.setQuestVar(0);
		qs.setReward(reward);
		qs.setCompleteCount(qs.getCompleteCount() + 1);
		if (template.getRepeatCycle() != null && player.getAccessLevel() == 0 || template.getQuestCoolTime() > 0) {
			qs.setNextRepeatTime(countNextRepeatTime(player, template));
		} else if (template.isTimeBased() && player.getAccessLevel() > 0) {
			PacketSendUtility.sendMessage(player, "You're GM! So system won't apply countNextRepeatTime()");
		}
		PacketSendUtility.sendPacket(player, new SM_QUEST_ACTION(id, qs.getStatus(), qs.getQuestVars().getQuestVars()));
		player.getController().updateZone();
		player.getController().updateNearbyQuests();
		GameEngineServices.questEngine().onLvlUp(env);
		if (template.getNpcFactionId() != 0) {
			player.getNpcFactions().completeQuest(template);
		}
		notifyQuestFinished(env);
		return true;
	}

	static void notifyQuestFinished(QuestEnv env) {
		if (env.getVisibleObject() instanceof Npc npc) {
			npc.getAi2().onQuestFinished(env.getPlayer(), env.getQuestId());
		}
	}

	private static QuestItems getQuestItemsbyClass(int id, List<QuestItems> classSelRew, int selRewIndex) {
		if (selRewIndex >= 0 && selRewIndex < classSelRew.size()) {
			return classSelRew.get(selRewIndex);
		}
		return null;
	}

	private static Timestamp countNextRepeatTime(Player player, QuestTemplate template) {
		int questCooltime = template.getQuestCoolTime();
		ZonedDateTime now = ZonedDateTime.now();
		ZonedDateTime repeatDate = now.withHour(9).withMinute(0).withSecond(0).withNano(0);

		if (template.isDaily()) {
			if (now.isAfter(repeatDate)) {
				repeatDate = repeatDate.plusHours(24);
			}
			PacketSendUtility.sendPacket(player, new SM_SYSTEM_MESSAGE(1400855, "9"));
		} else if (template.getQuestCoolTime() > 0) {
			repeatDate = repeatDate.plusSeconds(template.getQuestCoolTime());
			// 此任务可在 %DURATIONDAY0s 后重新尝试。 / This quest can be re-attempted in %DURATIONDAY0s.
			PacketSendUtility.sendPacket(player, new SM_SYSTEM_MESSAGE(1402676, questCooltime));
		} else {
			int daysToAdd = 7;
			int startDay = 7;
			for (QuestRepeatCycle weekDay : template.getRepeatCycle()) {
				int dayValue = weekDay.getDay();
				int diff = dayValue - repeatDate.getDayOfWeek().getValue();
				if (diff > 0 && diff < daysToAdd) {
					daysToAdd = diff;
				}
				if (startDay > dayValue) {
					startDay = dayValue;
				}
			}
			if (startDay == daysToAdd) {
				daysToAdd = 7;
			} else if (daysToAdd == 7 && startDay < 7) {
				daysToAdd = 7 - repeatDate.getDayOfWeek().getValue() + startDay;
			}
			repeatDate = repeatDate.plusDays(daysToAdd);
			PacketSendUtility.sendPacket(player, new SM_SYSTEM_MESSAGE(1400857, new DescriptionId(1800667), "9"));
		}
		return new Timestamp(repeatDate.toInstant().toEpochMilli());
	}

	/**
	 * 检查任务开始条件。
	 * Checks quest start conditions.
	 *
	 * @param env 任务环境 / quest environment
	 * @param warn 是否向玩家提示 / whether to warn the player
	 * whether conditions are met
	 */
	public static boolean checkStartConditions(QuestEnv env, boolean warn) {
		return checkStartConditionsImpl(env, warn);
	}

	private static boolean checkStartConditionsImpl(QuestEnv env, boolean warn) {
		Player player = env.getPlayer();
		QuestTemplate template = questsData.getQuestById(env.getQuestId());
        QuestState qs = player.getQuestStateList().getQuestState(env.getQuestId());
        if (qs != null && qs.getStatus() != QuestStatus.NONE && !qs.canRepeat()) {
            return false;
        }
		if (template == null) {
			return false;
		}
		if (template.getRacePermitted() != null) {
			if (template.getRacePermitted() != player.getRace() && template.getRacePermitted() != Race.PC_ALL) {
				return false;
			}
		}
        int levelDiff = template.getMinlevelPermitted() - player.getLevel();
        if (levelDiff > 0 && template.getMinlevelPermitted() != 999) {
            if (warn) {
                  PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_QUEST_ACQUIRE_ERROR_MIN_LEVEL(Integer.toString(template.getMinlevelPermitted())));
            }
            return false;
        }
		if (template.getMaxlevelPermitted() != 0 && player.getLevel() > template.getMaxlevelPermitted()) {
			if (warn) {
				PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_QUEST_ACQUIRE_ERROR_MAX_LEVEL(Integer.toString(template.getMaxlevelPermitted())));
			}
			return false;
		}
		if (!template.getClassPermitted().isEmpty() && !template.getClassPermitted().contains(player.getCommonData().getPlayerClass())) {
			if (warn) {
				PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_QUEST_ACQUIRE_ERROR_CLASS);
			}
			return false;
		}
		if (template.getGenderPermitted() != null && template.getGenderPermitted() != player.getGender()) {
			if (warn) {
				PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_QUEST_ACQUIRE_ERROR_GENDER);
			}
			return false;
		}
		if (template.getRequiredRank() != 0) {
			if (player.getAbyssRank().getRank().getId() < template.getRequiredRank()) {
				if (warn) {
					PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_QUEST_ACQUIRE_ERROR_MIN_RANK(AbyssRankEnum.getRankById(template.getRequiredRank()).getDescriptionId()));
				}
				return false;
			}
		}
		if (template.getTitleId() != 0) {
			if (!player.getTitleList().contains(template.getTitleId())) {
				if (warn) {
					// 仅在拥有 %0 头衔时才能接取此任务。 / You can only receive this quest when you have the %0 title.
					PacketSendUtility.sendPacket(player, new SM_SYSTEM_MESSAGE(1300588, template.getTitleId()));
				}
				return false;
			}
		}
		if (((template.isMaster()) && (!CraftSkillUpdateService.canLearnMoreMasterCraftingSkill(player))) || ((template.isExpert()) && (!CraftSkillUpdateService.canLearnMoreExpertCraftingSkill(player)))) {
			return false;
		}
		int fulfilledStartConditions = 0;
		if (!template.getXMLStartConditions().isEmpty()) {
			for (XMLStartCondition startCondition : template.getXMLStartConditions()) {
				if (startCondition.check(player, warn)) {
					fulfilledStartConditions++;
				}
			}
			if (fulfilledStartConditions < 1) {
				return false;
			}
		}
		if (warn && !inventoryItemCheck(env, warn)) {
			return false;
		}
		if (template.getCombineSkill() != null) {
			List<Integer> skills = new ArrayList<Integer>();
			if (template.getCombineSkill() == -1) {
				skills.add(30002);
				skills.add(30003);
				skills.add(40001);
				skills.add(40002);
				skills.add(40003);
				skills.add(40004);
				skills.add(40007);
				skills.add(40008);
				skills.add(40010);
			} else {
				skills.add(template.getCombineSkill());
			}
			boolean result = false;
			for (int skillId : skills) {
				PlayerSkillEntry skill = player.getSkillList().getSkillEntry(skillId);
				if (skill != null && skill.getSkillLevel() >= template.getCombineSkillPoint()) {
					if (template.getCategory().equals(QuestCategory.TASK) && skill.getSkillLevel() - 40 > template.getCombineSkillPoint())
						continue;
					result = true;
					break;
				}
			}
			if (!result) {
				return false;
			}
		}
		if (warn && template.getNpcFactionId() != 0 && !template.isTimeBased()) {
			if (!player.getNpcFactions().canStartQuest(template)) {
				return false;
			}
		}
		return true;
	}

	/**
	 * 以指定状态启动任务，是否警告取决于对话 ID。
	 * Starts a quest with the given status; warn flag depends on dialog id.
	 *
	 * @param env 任务环境 / quest environment
	 * initial status
	 * whether successful
	 */
	public static boolean startQuest(QuestEnv env, QuestStatus status) {
		return startQuest(env, status, env.getDialogId() != 0);
	}

	/**
	 * 以指定状态启动任务。
	 * Starts a quest with the given status.
	 *
	 * @param env 任务环境 / quest environment
	 * initial status
	 * @param warn 是否向玩家提示失败原因 / whether to warn the player on failure
	 * whether successful
	 */
	public static boolean startQuest(QuestEnv env, QuestStatus status, boolean warn) {
		Player player = env.getPlayer();
		int id = env.getQuestId();
		QuestStateList qsl = player.getQuestStateList();
		QuestState qs = qsl.getQuestState(id);
		QuestTemplate template = questsData.getQuestById(env.getQuestId());
		// 稍后待办 / TO DO LATER
		/*
		 * if (template.getTargetType() == QuestTargetType.FORCE ||
		 * template.getTargetType() == QuestTargetType.UNION) {
		 * PacketSendUtility.sendPacket(player,
		 * SM_SYSTEM_MESSAGE.STR_UNION_YOU_ARE_NOT_UNION_MEMBER); return true; }
		 */
		if (template.getNpcFactionId() != 0) {
			NpcFaction faction = player.getNpcFactions().getNpcFactionById(template.getNpcFactionId());
			if (!faction.isActive() || faction.getQuestId() != env.getQuestId()) {
				return false;
			}
		}
		if (!checkStartConditions(env, warn)) {
			return false;
		}
		if (player.getLevel() < template.getMinlevelPermitted() && template.getMinlevelPermitted() != 999) {
			return false;
		}
		if (!template.isNoCount() && !checkQuestListSize(qsl)) {
			PacketSendUtility.sendPacket(player, new SM_SYSTEM_MESSAGE(1300622, template.getName()));
			return false;
		}
		if (qs != null) {
			if (!qs.canRepeat()) {
				return false;
			}
		}
		if (template.getNpcFactionId() != 0 && !template.isTimeBased()) {
			if (!player.getNpcFactions().canStartQuest(template)) {
				return false;
			}
		}
		if (!LimitedQuestService.tryAcquire(template)) {
			return false;
		}
		if (qs != null) {
			qs.setStatus(status);
		} else {
			player.getQuestStateList().addQuest(id, new QuestState(id, status, 0, 0, null, 0, null));
		}
		if (template.getNpcFactionId() != 0 && !template.isTimeBased()) {
			player.getNpcFactions().startQuest(template);
		}
		PacketSendUtility.sendPacket(player, new SM_QUEST_ACTION(id, status.value(), 0));
		player.getController().updateZone();
		player.getController().updateNearbyQuests();
		return true;
	}

	/**
	 * 以 START 状态启动任务。
	 * Starts a quest with START status.
	 *
	 * @param env 任务环境 / quest environment
	 * whether successful
	 */
	public static boolean startQuest(QuestEnv env) {
		return startQuest(env, QuestStatus.START, env.getDialogId() != 0);
	}

	/**
	 * 启动主线/剧情任务（Mission）。
	 * Starts a mission quest.
	 *
	 * @param env 任务环境 / quest environment
	 * initial status
	 */
	public static void startMission(QuestEnv env, QuestStatus status) {
		Player player = env.getPlayer();
		int questId = env.getQuestId();
		if (player.getQuestStateList().getQuestState(questId) != null) {
			return;
		} else {
			player.getQuestStateList().addQuest(questId, new QuestState(questId, status, 0, 0, null, 0, null));
		}
		PacketSendUtility.sendPacket(player, new SM_QUEST_ACTION(questId, status.value(), 0));
	}

	/**
	 * 检查主线任务属性/职业等前置条件。
	 * Checks mission stat/class and related prerequisites.
	 *
	 * @param env 任务环境 / quest environment
	 * whether conditions are met
	 */
	public static boolean checkMissionStatConditions(QuestEnv env) {
		Player player = env.getPlayer();
		QuestTemplate template = questsData.getQuestById(env.getQuestId());
		if (template == null) {
			return false;
		}
		if (template.getRacePermitted() != null && template.getRacePermitted() != player.getRace()) {
			return false;
		}
		if (template.getClassPermitted().size() != 0 && !template.getClassPermitted().contains(player.getCommonData().getPlayerClass())) {
			return false;
		}
		if (template.getGenderPermitted() != null && template.getGenderPermitted() != player.getGender()) {
			return false;
		}
		if (template.getCombineSkill() != null) {
			List<Integer> skills = new ArrayList<Integer>();
			if (template.getCombineSkill() == -1) {
				skills.add(30002);
				skills.add(30003);
				skills.add(40001);
				skills.add(40002);
				skills.add(40003);
				skills.add(40004);
				skills.add(40007);
				skills.add(40008);
				skills.add(40010);
			} else {
				skills.add(template.getCombineSkill());
			}
			boolean result = false;
			for (int skillId : skills) {
				PlayerSkillEntry skill = player.getSkillList().getSkillEntry(skillId);
				if (skill != null && skill.getSkillLevel() >= template.getCombineSkillPoint() && skill.getSkillLevel() - 40 <= template.getCombineSkillPoint()) {
					result = true;
					break;
				}
			}
			if (!result) {
				return false;
			}
		}
		return true;
	}

	/**
	 * 启动活动类任务。
	 * Starts an event-category quest.
	 *
	 * @param env 任务环境 / quest environment
	 * initial status
	 * whether successful
	 */
	public static boolean startEventQuest(QuestEnv env, QuestStatus questStatus) {
		QuestTemplate template = questsData.getQuestById(env.getQuestId());
		if (template.getCategory() != QuestCategory.EVENT) {
			return false;
		}
		int id = env.getQuestId();
		Player player = env.getPlayer();
		PacketSendUtility.sendPacket(player, new SM_QUEST_ACTION(id, questStatus, 0));
		if ((player.getLevel() < template.getMinlevelPermitted()) && (template.getMinlevelPermitted() != 999)) {
			return false;
		}
		if (template.getMaxlevelPermitted() != 0 && player.getLevel() > template.getMaxlevelPermitted()) {
			return false;
		}
		if (template.getRacePermitted() != null) {
			if (template.getRacePermitted() != player.getRace() && template.getRacePermitted() != Race.PC_ALL) {
				return false;
			}
		}
		if (!template.getClassPermitted().isEmpty()) {
			if (!template.getClassPermitted().contains(player.getCommonData().getPlayerClass())) {
				return false;
			}
		}
		if (template.getGenderPermitted() != null) {
			if (template.getGenderPermitted() != player.getGender()) {
				return false;
			}
		}
		QuestState qs = player.getQuestStateList().getQuestState(id);
		if (qs == null) {
			qs = new QuestState(template.getId(), questStatus, 0, 0, null, 0, null);
			player.getQuestStateList().addQuest(id, qs);
		} else {
			if (template.getMaxRepeatCount() >= qs.getCompleteCount()) {
				qs.setStatus(questStatus);
				qs.setQuestVar(0);
			}
		}
		player.getController().updateZone();
		player.getController().updateNearbyQuests();
		return true;
	}

	private static boolean checkQuestListSize(QuestStateList qsl) {
		return (qsl.getNormalQuestListSize() + 1) <= CustomConfig.BASIC_QUEST_SIZE_LIMIT;
	}

	/**
	 * 将任务标记为完成状态（不走完整奖励流程时使用）。
	 * Marks the quest as complete (used when not running the full reward flow).
	 *
	 * @param env 任务环境 / quest environment
	 * whether successful
	 */
	public boolean completeQuest(QuestEnv env) {
		Player player = env.getPlayer();
		int id = env.getQuestId();
		QuestState qs = player.getQuestStateList().getQuestState(id);
		if (qs == null || qs.getStatus() != QuestStatus.START) {
			return false;
		}
		qs.setQuestVarById(0, qs.getQuestVarById(0) + 1);
		qs.setStatus(QuestStatus.REWARD);
		PacketSendUtility.sendPacket(player, new SM_QUEST_ACTION(id, qs.getStatus(), qs.getQuestVars().getQuestVars()));
		player.getController().updateZone();
		player.getController().updateNearbyQuests();
		return true;
	}

	/**
	 * 检查（并可选移除）任务收集道具是否齐全。
	 * Checks (and optionally removes) whether quest collect items are complete.
	 *
	 * @param env 任务环境 / quest environment
	 * @param removeItem 是否扣除道具 / whether to remove items
	 * whether items are sufficient
	 */
	public static boolean collectItemCheck(QuestEnv env, boolean removeItem) {
		Player player = env.getPlayer();
		QuestState qs = player.getQuestStateList().getQuestState(env.getQuestId());
		if (qs == null && removeItem) {
			return false;
		}
		QuestTemplate template = questsData.getQuestById(env.getQuestId());
		CollectItems collectItems = template.getCollectItems();
		if (collectItems == null) {
			InventoryItems inventoryItems = template.getInventoryItems();
			if (inventoryItems == null) {
				return true;
			}
			for (InventoryItem inventoryItem : inventoryItems.getInventoryItem()) {
				int itemId = inventoryItem.getItemId();
				if (player.getInventory().getItemCountByItemId(itemId) < 1) {
					return false;
				}
			}
			if (removeItem) {
				for (InventoryItem inventoryItem : inventoryItems.getInventoryItem()) {
					player.getInventory().decreaseByItemId(inventoryItem.getItemId(), 1);
				}
			}
			return true;
		}
		for (CollectItem collectItem : collectItems.getCollectItem()) {
			int itemId = collectItem.getItemId();
			long count = itemId == ItemId.KINAH.value() ? player.getInventory().getKinah(): player.getInventory().getItemCountByItemId(itemId);
			if (collectItem.getCount() > count) {
				return false;
			}
		}
		if (removeItem) {
			for (CollectItem collectItem : collectItems.getCollectItem()) {
				if (collectItem.getItemId() == 182400001) {
					player.getInventory().decreaseKinah(collectItem.getCount());
				} else {
					player.getInventory().decreaseByItemId(collectItem.getItemId(), collectItem.getCount());
				}
			}
		}
		return true;
	}

	/**
	 * 检查背包是否持有任务要求的道具。
	 * Checks whether the inventory holds required quest items.
	 *
	 * @param env 任务环境 / quest environment
	 * @param showWarning 是否提示玩家 / whether to show a warning
	 * whether items are present
	 */
	public static boolean inventoryItemCheck(QuestEnv env, boolean showWarning) {
		Player player = env.getPlayer();
		QuestTemplate template = questsData.getQuestById(env.getQuestId());
		InventoryItems inventoryItems = template.getInventoryItems();
		if (inventoryItems == null) {
			return true;
		}
		int requiredItemNameId = 0;
		for (InventoryItem inventoryItem : inventoryItems.getInventoryItem()) {
			Item item = player.getInventory().getFirstItemByItemId(inventoryItem.getItemId());
			if (item == null) {
				requiredItemNameId = DataManager.ITEM_DATA.getItemTemplate(inventoryItem.getItemId()).getNameId();
				break;
			}
		}
		if (requiredItemNameId != 0 && showWarning) {
			PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_QUEST_ACQUIRE_ERROR_INVENTORY_ITEM(new DescriptionId(requiredItemNameId)));
		}
		return requiredItemNameId == 0;
	}

	/**
	 * 在指定实例生成一次性任务 NPC。
	 * Spawns a one-time quest NPC in the given instance.
	 *
	 * 世界 ID / world id
	 * instance id
	 * NPC 模板 ID / NPC template id
	 * @param x X 坐标 / X
	 * @param y Y 坐标 / Y
	 * @param z Z 坐标 / Z
	 * 朝向 / heading
	 * spawned object
	 */
	public static VisibleObject spawnQuestNpc(int worldId, int instanceId, int templateId, float x, float y, float z, byte heading) {
		return SpawnEngine.spawnObject(SpawnEngine.addNewSingleTimeSpawn(worldId, templateId, x, y, z, heading), instanceId);
	}

	/**
	 * 生成一次性任务相关生物（别名接口）。
	 * Spawns a one-time quest-related creature (alias API).
	 *
	 * 世界 ID / world id
	 * instance id
	 * template id
	 * @param x X 坐标 / X
	 * @param y Y 坐标 / Y
	 * @param z Z 坐标 / Z
	 * 朝向 / heading
	 * spawned object
	 */
	public static VisibleObject addNewSpawn(int worldId, int instanceId, int templateId, float x, float y, float z, byte heading) {
		return SpawnEngine.spawnObject(SpawnEngine.addNewSingleTimeSpawn(worldId, templateId, x, y, z, heading), instanceId);
	}

	/**
	 * 生成限时任务对象，到时后自动 despawn。
	 * Spawns a timed quest object that despawns after the given seconds.
	 */
	public static VisibleObject addNewSpawnForSeconds(int worldId, int instanceId, int templateId, float x, float y, float z,
			byte heading, int lifetimeSeconds) {
		VisibleObject object = spawnQuestNpc(worldId, instanceId, templateId, x, y, z, heading);
		GameThreadPoolServices.threadPoolManager().schedule(() -> {
			if (object.isSpawned()) {
				object.getController().onDelete();
			}
		}, lifetimeSeconds * 1000L);
		return object;
	}

	/** Spawns a timed quest object at a retail-compatible random walkable point within five metres of the player. */
	public static VisibleObject addNewSpawnForSeconds(Player player, int templateId, int lifetimeSeconds) {
		var point = MathUtil.get2DPointInsideCircle(player.getX(), player.getY(), 5);
		float x = point.x;
		float y = point.y;
		float z = GameWorldServices.geoService().getZ(player.getWorldId(), x, y, player.getZ(), 100, player.getInstanceId());
		if (!GameWorldServices.pathService().canMoveStraight(player, x, y, z)) {
			x = player.getX();
			y = player.getY();
			z = player.getZ();
		}
		return addNewSpawnForSeconds(player.getWorldId(), player.getInstanceId(), templateId, x, y, z, (byte) 0,
				lifetimeSeconds);
	}

	/**
	 * 生成限时任务 NPC，到时后自动 despawn。
	 * Spawns a timed quest NPC that despawns after the given minutes.
	 *
	 * 世界 ID / world id
	 * instance id
	 * template id
	 * @param x X 坐标 / X
	 * @param y Y 坐标 / Y
	 * @param z Z 坐标 / Z
	 * 朝向 / heading
	 * @param timeInMin 存活分钟数 / lifetime in minutes
	 */
	public static void addNewSpawn(int worldId, int instanceId, int templateId, float x, float y, float z, byte heading, int timeInMin) {
		addNewSpawnForSeconds(worldId, instanceId, templateId, x, y, z, heading, timeInMin * 60);
	}

	/**
	 * 计算 NPC 对玩家/队伍的任务掉落并写入 dropItems。
	 * Computes quest drops from an NPC for player/group and appends them to dropItems.
	 *
	 * drop item set
	 * @param index 起始索引 / start index
	 * killed NPC
	 * @param players 参与玩家集合 / participating players
	 * @param player 主要拾取者 / primary looter
	 * next index
	 */
	public static int getQuestDrop(Set<DropItem> dropItems, int index, Npc npc, Collection<Player> players, Player player) {
		Collection<QuestDrop> drops = getQuestDrop(npc.getNpcId());
		if (drops.isEmpty()) {
			return index;
		}
		DropNpc dropNpc = GameWorldServices.dropRegistrationService().getDropRegistrationMap().get(npc.getObjectId());
		for (QuestDrop drop : drops) {
			if (Rnd.get() * 100 > drop.getChance()) {
				continue;
			}
			if (players != null && player.isInGroup2()) {
				List<Player> pls = new ArrayList<Player>();
				if (drop.isDropEachMemberGroup()) {
					for (Player member : players) {
						if (isQuestDrop(member, drop)) {
							pls.add(member);
							dropItems.add(regQuestDropItem(drop, index++, member.getObjectId()));
						}
					}
				} else {
					for (Player member : players) {
						if (isQuestDrop(member, drop)) {
							pls.add(member);
							break;
						}
					}
				}
				if (pls.size() > 0) {
					if (!drop.isDropEachMemberGroup()) {
						dropItems.add(regQuestDropItem(drop, index++, 0));
					}
					for (Player p : pls) {
						dropNpc.setAllowedLooter(p);
						if (player.getPlayerGroup2().getLootGroupRules().getLootRule() != LootRuleType.FREEFORALL) {
							PacketSendUtility.sendPacket(p, new SM_LOOT_STATUS(npc.getObjectId(), Status.LOOT_ENABLE));
						}
					}
					pls.clear();
				}
			} else if (players != null && player.isInAlliance2()) {
				List<Player> pls = new ArrayList<Player>();
				if (drop.isDropEachMemberAlliance()) {
					for (Player member : players) {
						if (isQuestDrop(member, drop)) {
							pls.add(member);
							dropItems.add(regQuestDropItem(drop, index++, member.getObjectId()));
						}
					}
				} else {
					for (Player member : players) {
						if (isQuestDrop(member, drop)) {
							pls.add(member);
							break;
						}
					}
				}
				if (pls.size() > 0) {
					if (!drop.isDropEachMemberAlliance()) {
						dropItems.add(regQuestDropItem(drop, index++, 0));
					}
					for (Player p : pls) {
						dropNpc.setAllowedLooter(p);
						if (player.getPlayerAlliance2().getLootGroupRules().getLootRule() != LootRuleType.FREEFORALL) {
							PacketSendUtility.sendPacket(p, new SM_LOOT_STATUS(npc.getObjectId(), Status.LOOT_ENABLE));
						}
					}
					pls.clear();
				}
			} else {
				if (isQuestDrop(player, drop)) {
					dropItems.add(regQuestDropItem(drop, index++, player.getObjectId()));
				}
			}
		}
		return index;
	}

	private static DropItem regQuestDropItem(QuestDrop drop, int index, Integer winner) {
		DropItem item = new DropItem(new Drop(drop.getItemId(), 1, 1, drop.getChance(), false, false));
		item.setPlayerObjId(winner);
		item.setIndex(index);
		item.setCount(1);
		return item;
	}

	/**
	 * 检查任务物品是否应该掉落
	 * Check if quest item should drop
	 * Player object
	 *
	 * @param drop 掉落物品信息 / Drop item information
	 * @param drop
	 * @return 是否允许掉落 / Whether dropping is allowed
	 */
	private static boolean isQuestDrop(Player player, QuestDrop drop) {
		// 获取任务 ID / Get quest ID
		int questId = drop.getQuestId();
		// 获取玩家的任务状态 / Get player's quest state
		QuestState qs = player.getQuestStateList().getQuestState(questId);
		// 检查任务是否处于进行状态 / Check if quest is in progress
		if (qs == null || qs.getStatus() != QuestStatus.START) {
			return false;
		}
		
		// 检查收集步骤是否匹配 / Check if collecting step matches
		if (drop.getCollectingStep() != 0) {
			if (drop.getCollectingStep() != qs.getQuestVarById(0)) {
				return false;
			}
		}
		
		// 获取任务模板 / Get quest template
		QuestTemplate qt = DataManager.QUEST_DATA.getQuestById(questId);
		
		// 检查联盟任务限制 / Check alliance quest restrictions
		if (player.isInAlliance2()) {
			if (!qt.getTargetType().equals(QuestTargetType.UNION)) { // League.
				return false;
			}
		}
		
		// 检查导师任务限制 / Check mentor quest restrictions
		if (qt.getMentorType() == QuestMentorType.MENTE) {
			if (!player.isInGroup2()) {
				return false;
			}
			PlayerGroup group = player.getPlayerGroup2();
			boolean found = false;
			// 检查组内是否有导师在有效距离内 / Check if there's a mentor within valid distance in group
			for (Player member : group.getMembers()) {
				if (member.isMentor() && MathUtil.getDistance(player, member) < GroupConfig.GROUP_MAX_DISTANCE) {
					found = true;
					break;
				}
			}
			if (!found) {
				return false;
			}
		}
		
		// 处理特殊掉落物品 / Handle special drop items
		if (drop instanceof HandlerSideDrop) {
			return ((HandlerSideDrop) drop).getNeededAmount() > player.getInventory().getItemCountByItemId(drop.getItemId());
		}
		
		// 获取当前掉落物品的 ID / Get current drop item ID
		int dropItemId = drop.getItemId();
		
		// 检查是否是任务工作物品 / Check if it's a quest work item
		QuestWorkItems workItems = qt.getQuestWorkItems();
		if (workItems != null && workItems.getQuestWorkItem().stream().anyMatch(workItem -> workItem != null && workItem.getItemId() == dropItemId)) {
			// 检查玩家背包中是否已有该工作物品 / Check if player already has this work item
			long count = player.getInventory().getItemCountByItemId(dropItemId);
			// 如果已有工作物品则不再掉落 / Don't drop if player already has the work item
			if (count > 0) {
				return false;
			}
			
			// 检查玩家是否已经完成了相关任务 / Check if player has completed the related quest
			for (QuestItems workItem : workItems.getQuestWorkItem()) {
				QuestState questState = player.getQuestStateList().getQuestState(qt.getId());
				if (questState != null && questState.getStatus() == QuestStatus.COMPLETE) {
					// 如果任务已完成，则不再掉落工作物品 / Don't drop work item if quest is complete
					return false;
				}
			}
		}
		
		// 检查是否是任务收集物品 / Check if it's a quest collect item
		CollectItems collectItems = qt.getCollectItems();
		if (collectItems == null) {
			return true;
		}
		
		// 检查当前掉落物品是否达到收集上限 / Check if current drop item has reached collection limit
		for (CollectItem collectItem : collectItems.getCollectItem()) {
			if (collectItem.getItemId() == dropItemId) {
				// 获取玩家当前收集的数量 / Get current collected amount
				long count = player.getInventory().getItemCountByItemId(dropItemId);
				// 如果未达到所需数量则允许掉落 / Allow drop if required amount not reached
				return collectItem.getCount() > count;
			}
		}
		
		// 如果不是收集物品则允许掉落 / Allow drop if not a collect item
		return true;
	}

	/**
	 * 检查玩家等级是否满足任务最低要求。
	 * Checks whether the player level meets the quest minimum.
	 *
	 * quest id
	 * player level
	 * whether met
	 */
	public static boolean checkLevelRequirement(int questId, int playerLevel) {
		return playerLevel >= questsData.getQuestById(questId).getMinlevelPermitted();
	}

	/**
	 * 返回玩家相对任务最低等级的差值（不足时为正）。
	 * Returns the level gap relative to the quest minimum (positive when under-leveled).
	 *
	 * quest id
	 * player level
	 * level difference
	 */
	public static int getLevelRequirement(int questId, int playerLevel) {
		QuestTemplate template = questsData.getQuestById(questId);
		if (template == null) {
			return 999;
		}
		if (questsData.getQuestById(questId).getMinlevelPermitted() == 999) {
			return 0;
		}
		return questsData.getQuestById(questId).getMinlevelPermitted() - playerLevel;
	}

	/*
	 * public static boolean bountyReward(Player player, int questId) {
	 * QuestTemplate template = questsData.getQuestById(questId); if (template ==
	 * null) { return false; } if (!template.isBountyReward()) { return false; }
	 * QuestState qs = player.getQuestStateList().getQuestState(questId); if (qs ==
	 * null) { return false; } if (qs.getStatus() == QuestStatus.COMPLETE ||
	 * qs.getStatus() == QuestStatus.LOCKED) { return false; } QuestEnv env = new
	 * QuestEnv(player, player, questId, QuestDialog.AUTO_REWARD.id());
	 * finishQuest(env); player.getController().updateZone();
	 * player.getController().updateNearbyQuests(); return true; }
	 */

	/**
	 * 启动可见的任务计时器。
	 * Starts a visible quest timer.
	 *
	 * @param env 任务环境 / quest environment
	 * seconds
	 *
	 * @return 是否已启动 / whether started
	 */
	public static boolean questTimerStart(QuestEnv env, int timeInSeconds) {
		final Player player = env.getPlayer();
		Future<?> task = GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			@Override
			public void run() {
				GameEngineServices.questEngine().onQuestTimerEnd(new QuestEnv(null, player, 0, 0));
			}
		}, timeInSeconds * 1000);
		player.getController().addTask(TaskId.QUEST_TIMER, task);
		PacketSendUtility.sendPacket(player, new SM_QUEST_ACTION(env.getQuestId(), timeInSeconds));
		return true;
	}

	/**
	 * 启动不可见任务计时器（超时回调引擎）。
	 * Starts an invisible quest timer (engine callback on timeout).
	 *
	 * @param env 任务环境 / quest environment
	 * seconds
	 *
	 * @return 是否已启动 / whether started
	 */
	public static boolean invisibleTimerStart(QuestEnv env, int timeInSeconds) {
		final Player player = env.getPlayer();
		GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			@Override
			public void run() {
				GameEngineServices.questEngine().onInvisibleTimerEnd(new QuestEnv(null, player, 0, 0));
			}
		}, timeInSeconds * 1000);
		return true;
	}

	/**
	 * 结束并取消任务计时器。
	 * Ends and cancels the quest timer.
	 *
	 * @param env 任务环境 / quest environment
	 * whether successful
	 */
	public static boolean questTimerEnd(QuestEnv env) {
		final Player player = env.getPlayer();
		player.getController().cancelTask(TaskId.QUEST_TIMER);
		PacketSendUtility.sendPacket(player, new SM_QUEST_ACTION(env.getQuestId(), 0));
		return true;
	}

	/**
	 * 放弃任务并清理相关状态。
	 * Abandons a quest and cleans related state.
	 *
	 * 玩家 / player
	 * quest id
	 * whether successful
	 */
	public static boolean abandonQuest(Player player, int questId) {
		QuestTemplate template = questsData.getQuestById(questId);
		if (template == null) {
			return false;
		}
		
		if (template.isCannotGiveup()) {
			return false;
		}
		QuestState qs = player.getQuestStateList().getQuestState(questId);
		if (qs == null) {
			return false;
		}
 
		if (template.getNpcFactionId() != 0) {
			player.getNpcFactions().abortQuest(template);
		}
		qs.setStatus(QuestStatus.NONE);
		qs.setQuestVar(0);
		QuestWorkItems qwi = template.getQuestWorkItems();
		if (qwi != null) {
			long count = 0;
			for (QuestItems qi : qwi.getQuestWorkItem()) {
				if (qi != null) {
					count = player.getInventory().getItemCountByItemId(qi.getItemId());
					if (count > 0) {
						player.getInventory().decreaseByItemId(qi.getItemId(), count);
					}
				}
			}
		}
		if (template.getCategory() == QuestCategory.TASK) {
			WorkOrdersData wod = null;
			for (XMLQuest xmlQuest : DataManager.XML_QUESTS.getQuest()) {
				if (xmlQuest.getId() == questId) {
					if (xmlQuest instanceof WorkOrdersData) {
						wod = (WorkOrdersData) xmlQuest;
						break;
					}
				}
			}
			if (wod != null) {
				player.getRecipeList().deleteRecipe(player, wod.getRecipeId());
			}
		}
		if (player.getController().getTask(TaskId.QUEST_TIMER) != null) {
			questTimerEnd(new QuestEnv(null, player, questId, 0));
		}
		PacketSendUtility.sendPacket(player, new SM_QUEST_ACTION(questId));
		player.getController().updateZone();
		player.getController().updateNearbyQuests();
		return true;
	}

	/**
	 * 获取 NPC 关联的任务掉落配置。
	 * Returns quest drop entries associated with an NPC.
	 *
	 * NPC 模板 ID / NPC template id
	 * drop collection
	 */
	public static Collection<QuestDrop> getQuestDrop(int npcId) {
		if (questDrop.containsKey(npcId)) {
			return questDrop.get(npcId);
		}
		return Collections.<QuestDrop>emptyList();
	}

	/**
	 * 注册 NPC 的任务掉落条目。
	 * Registers a quest drop entry for an NPC.
	 *
	 * NPC 模板 ID / NPC template id
	 * @param drop 掉落配置 / drop entry
	 */
	public static void addQuestDrop(int npcId, QuestDrop drop) {
		if (!questDrop.containsKey(npcId)) {
			questDrop.put(npcId, drop);
		} else {
			questDrop.get(npcId).add(drop);
		}
	}

	/**
	 * 返回小队中各自需要该任务掉落的成员列表。
	 * Returns group members who each need the quest drop.
	 *
	 * player group
	 * NPC 模板 ID / NPC template id
	 * quest id
	 * member list
	 */
	public static List<Player> getEachDropMembersGroup(PlayerGroup group, int npcId, int questId) {
		List<Player> players = new ArrayList<Player>();
		for (QuestDrop qd : getQuestDrop(npcId)) {
			if (qd.isDropEachMemberGroup()) {
				for (Player player : group.getMembers()) {
					QuestState qstel = player.getQuestStateList().getQuestState(questId);
					if (qstel != null && qstel.getStatus() == QuestStatus.START) {
						players.add(player);
					}
				}
				break;
			}
		}
		return players;
	}

	/**
	 * 返回联盟中各自需要该任务掉落的成员列表。
	 * Returns alliance members who each need the quest drop.
	 *
	 * player alliance
	 * NPC 模板 ID / NPC template id
	 * quest id
	 * member list
	 */
	public static List<Player> getEachDropMembersAlliance(PlayerAlliance alliance, int npcId, int questId) {
		List<Player> players = new ArrayList<Player>();
		for (QuestDrop qd : getQuestDrop(npcId)) {
			if (qd.isDropEachMemberGroup()) {
				for (Player player : alliance.getMembers()) {
					QuestState qstel = player.getQuestStateList().getQuestState(questId);
					if (qstel != null && qstel.getStatus() == QuestStatus.START) {
						players.add(player);
					}
				}
				break;
			}
		}
		return players;
	}
}
