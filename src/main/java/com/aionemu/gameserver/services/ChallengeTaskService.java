package com.aionemu.gameserver.services;

import com.aionemu.boot.i18n.I18n;
import com.aionemu.gameserver.lifecycle.GameHousingServices;

import com.aionemu.gameserver.lifecycle.GameFeatureServices;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.springframework.beans.factory.ObjectProvider;

import com.aionemu.commons.database.dao.DAOManager;
import com.aionemu.gameserver.configs.main.CustomConfig;
import com.aionemu.gameserver.dao.ChallengeTasksDAO;
import com.aionemu.gameserver.dao.LegionMemberDAO;
import com.aionemu.gameserver.dao.PlayerDAO;
import com.aionemu.gameserver.dao.TownDAO;
import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.model.challenge.ChallengeQuest;
import com.aionemu.gameserver.model.challenge.ChallengeTask;
import com.aionemu.gameserver.model.gameobjects.LetterType;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.team.legion.LegionMember;
import com.aionemu.gameserver.model.templates.challenge.ChallengeTaskTemplate;
import com.aionemu.gameserver.model.templates.challenge.ChallengeType;
import com.aionemu.gameserver.model.templates.challenge.ContributionReward;
import com.aionemu.gameserver.model.town.Town;
import com.aionemu.gameserver.network.aion.serverpackets.SM_CHALLENGE_LIST;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.services.mail.SystemMailService;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.world.World;

import java.util.LinkedHashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

/**
 * 挑战任务服务，管理城镇/军团挑战任务列表、完成结算与奖励。
 * Challenge task service managing town/legion challenge lists, completion and rewards.
 */
@Slf4j
public class ChallengeTaskService {
	private static volatile ObjectProvider<ChallengeTaskService> instanceProvider;
	/** 城镇 ID → 挑战任务映射。 / Town id → challenge task map. */
	private Map<Integer, Map<Integer, ChallengeTask>> cityTasks;
	/** 军团 ID → 挑战任务映射。 / Legion id → challenge task map. */
	private Map<Integer, Map<Integer, ChallengeTask>> legionTasks;

	private static class SingletonHolder {
		protected static final ChallengeTaskService instance = new ChallengeTaskService();
	}

	/**
	 * 获取服务单例，优先走 Spring ObjectProvider。
	 * Returns the service singleton, preferring Spring ObjectProvider when available.
	 *
	 * service instance
	 */
	public static final ChallengeTaskService getInstance() {
		ObjectProvider<ChallengeTaskService> provider = instanceProvider;
		if (provider != null) {
			return provider.getIfAvailable(() -> SingletonHolder.instance);
		}
		return SingletonHolder.instance;
	}

	/**
	 * 注入 Spring ObjectProvider 以覆盖默认单例。
	 * Injects a Spring ObjectProvider to override the default singleton.
	 *
	 * provider
	 */
	public static void setInstanceProvider(ObjectProvider<ChallengeTaskService> provider) {
		instanceProvider = provider;
	}

	/**
	 * 构造服务并初始化任务缓存。
	 * Constructs the service and initializes task caches.
	 */
	public ChallengeTaskService() {
		cityTasks = new LinkedHashMap<Integer, Map<Integer, ChallengeTask>>();
		legionTasks = new LinkedHashMap<Integer, Map<Integer, ChallengeTask>>();
		log.info(I18n.get("log.e300214dba9a"));
	}

	/**
	 * 向玩家展示指定所有者的挑战任务列表。
	 * Shows the challenge task list for the given owner to the player.
	 *
	 * 玩家 / player
	 * challenge type
	 * @param ownerId 所有者 ID（城镇/军团） / owner id (town/legion)
	 */
	public void showTaskList(Player player, ChallengeType challengeType, int ownerId) {
		if (CustomConfig.CHALLENGE_TASKS_ENABLED) {
			int ownerLevel = 0;
			switch (challengeType) {
			case LEGION:
				ownerLevel = player.getLegion().getLegionLevel();
				break;
			case TOWN:
				ownerLevel = GameHousingServices.townService().getTownById(ownerId).getLevel();
				break;
			default:
				break;
			}
			List<ChallengeTask> availableTasks = buildTaskList(player, challengeType, ownerId, ownerLevel);
			PacketSendUtility.sendPacket(player, new SM_CHALLENGE_LIST(2, ownerId, challengeType, availableTasks));
			for (ChallengeTask task : availableTasks) {
				PacketSendUtility.sendPacket(player, new SM_CHALLENGE_LIST(7, ownerId, challengeType, task));
			}
		}
	}

	/**
	 * 构建可用挑战任务列表（加载缓存、补齐新任务）。
	 * Builds the available challenge task list (loads cache and creates missing tasks).
	 *
	 * 玩家 / player
	 * challenge type
	 * owner id
	 * @param ownerLevel 所有者等级 / owner level
	 * @return 可用任务列表 / available tasks
	 */
	private List<ChallengeTask> buildTaskList(Player player, ChallengeType challengeType, int ownerId, int ownerLevel) {
		Map<Integer, Map<Integer, ChallengeTask>> taskMap = null;
		if (challengeType == ChallengeType.LEGION) {
			taskMap = legionTasks;
		} else if (challengeType == ChallengeType.TOWN) {
			taskMap = cityTasks;
		}
		int playerTownId = GameHousingServices.townService().getTownResidence(player);
		List<ChallengeTask> availableTasks = new ArrayList<ChallengeTask>();
		if (!taskMap.containsKey(ownerId)) {
			Map<Integer, ChallengeTask> tasks = DAOManager.getDAO(ChallengeTasksDAO.class).load(ownerId, challengeType);
			taskMap.put(ownerId, tasks);
		}
		for (ChallengeTask ct : taskMap.get(ownerId).values()) {
			if (ct.getTemplate().isRepeatable()) {
				availableTasks.add(ct);
			} else if (!ct.isCompleted()) {
				availableTasks.add(ct);
			}
		}
		for (ChallengeTaskTemplate template : DataManager.CHALLENGE_DATA.getTasks().values()) {
			if (template.getType() == challengeType && template.getRace() == player.getRace()) {
				if (!taskMap.get(ownerId).containsKey(template.getId())) {
					if (ownerLevel >= template.getMinLevel() && ownerLevel <= template.getMaxLevel()) {
						if (template.isTownResidence() && playerTownId != ownerId) {
							continue;
						}
						if (template.getPrevTask() == null) {
							ChallengeTask task = new ChallengeTask(ownerId, template);
							taskMap.get(ownerId).put(task.getTaskId(), task);
							DAOManager.getDAO(ChallengeTasksDAO.class).storeTask(task);
							availableTasks.add(task);
							continue;
						} else {
							int prevTaskId = template.getPrevTask();
							if (taskMap.get(ownerId).containsKey(prevTaskId)) {
								ChallengeTask prevTask = taskMap.get(ownerId).get(prevTaskId);
								if (prevTask.isCompleted()) {
									ChallengeTask task = new ChallengeTask(ownerId, template);
									taskMap.get(ownerId).put(task.getTaskId(), task);
									DAOManager.getDAO(ChallengeTasksDAO.class).storeTask(task);
									availableTasks.add(task);
								}
							}
						}
					}
				}
			}
		}
		return availableTasks;
	}

	/**
	 * 挑战子任务完成时的分发入口。
	 * Entry point when a challenge sub-quest is finished.
	 *
	 * 玩家 / player
	 * quest id
	 */
	public void onChallengeQuestFinish(Player player, int questId) {
		ChallengeTaskTemplate taskTemplate = DataManager.CHALLENGE_DATA.getTaskByQuestId(questId);
		switch (taskTemplate.getType()) {
		case TOWN:
			onCityTaskFinish(player, taskTemplate, questId);
			break;
		case LEGION:
			onLegionTaskFinish(player, taskTemplate, questId);
			break;
		}
	}

	/**
	 * 处理城镇挑战任务完成：加分、升级与奖励。
	 * Handles town challenge completion: points, level-up and rewards.
	 *
	 * 玩家 / player
	 * task template
	 * quest id
	 */
	private void onCityTaskFinish(Player player, ChallengeTaskTemplate taskTemplate, int questId) {
		int townId = GameHousingServices.townService().getTownIdByPosition(player);
		if (cityTasks.get(townId) == null) {
			buildTaskList(player, ChallengeType.TOWN, townId, GameHousingServices.townService().getTownById(townId).getLevel());
			if (cityTasks.get(townId) == null) {
				return;
			}
		}
		ChallengeTask task = cityTasks.get(townId).get(taskTemplate.getId());
		if (task == null || task.getQuests().get(questId) == null) {
			return;
		}
		ChallengeQuest quest = task.getQuests().get(questId);
		if (quest.getCompleteCount() >= quest.getMaxRepeats()) {
			return;
		}
		if (!task.isCompleted()) {
			task.updateCompleteTime();
			quest.increaseCompleteCount();
			DAOManager.getDAO(ChallengeTasksDAO.class).storeTask(task);
			Town town = GameHousingServices.townService().getTownById(townId);
			if (town != null) {
				int oldLevel = town.getLevel();
				town.increasePoints(quest.getScorePerQuest());
				if (task.isCompleted()) {
					switch (taskTemplate.getReward().getType()) {
					case POINT:
						town.increasePoints(taskTemplate.getReward().getValue());
						break;
					case SPAWN:
						break;
					default:
						break;
					}
				}
				if (town.getLevel() != oldLevel) {
					PacketSendUtility.sendPacket(player,
							new SM_SYSTEM_MESSAGE(1401520, town.getNameId(), town.getLevel()));
				}
				DAOManager.getDAO(TownDAO.class).store(town);
			}
		}
	}

	/**
	 * 处理军团挑战任务完成：累计贡献并在整任务完成后按贡献发奖。
	 * Handles legion challenge completion: tracks contribution and mails rewards by rank.
	 *
	 * 玩家 / player
	 * task template
	 * quest id
	 */
	private void onLegionTaskFinish(Player player, ChallengeTaskTemplate taskTemplate, int questId) {
		if (player.getLegion() == null) {
			return;
		}
		int legionId = player.getLegion().getLegionId();
		if (!legionTasks.containsKey(legionId)) {
			return;
		}
		if (legionTasks.get(legionId).get(taskTemplate.getId()) == null) {
			return;
		}
		ChallengeTask task = legionTasks.get(player.getLegion().getLegionId()).get(taskTemplate.getId());
		ChallengeQuest quest = task.getQuests().get(questId);
		if (quest.getCompleteCount() >= quest.getMaxRepeats()) {
			return;
		}
		player.getLegionMember().increaseChallengeScore(quest.getScorePerQuest());
		if (!task.isCompleted()) {
			task.updateCompleteTime();
			quest.increaseCompleteCount();
			DAOManager.getDAO(ChallengeTasksDAO.class).storeTask(task);
			if (task.isCompleted()) {
				TreeMap<Integer, List<Integer>> winnersByPoints = new TreeMap<Integer, List<Integer>>();
				for (Integer memberObjId : player.getLegion().getLegionMembers()) {
					Player member = com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().findPlayer(memberObjId);
					if (member != null) {
						int score = member.getLegionMember().getChallengeScore();
						if (winnersByPoints.get(score) == null) {
							winnersByPoints.put(score, new ArrayList<Integer>());
						}
						winnersByPoints.get(score).add(member.getObjectId());
						member.getLegionMember().setChallengeScore(0);
						continue;
					} else {
						LegionMember legionMember = DAOManager.getDAO(LegionMemberDAO.class)
								.loadLegionMember(memberObjId);
						int score = legionMember.getChallengeScore();
						if (score <= 0) {
							continue;
						}
						if (winnersByPoints.get(score) == null) {
							winnersByPoints.put(score, new ArrayList<Integer>());
						}
						winnersByPoints.get(score).add(legionMember.getObjectId());
						legionMember.setChallengeScore(0);
						DAOManager.getDAO(LegionMemberDAO.class).storeLegionMember(memberObjId, legionMember);
					}
				}
				int rewardsAdded = 0, itemId, itemCount;
				for (Entry<Integer, List<Integer>> e : winnersByPoints.descendingMap().entrySet()) {
					for (int objectId : e.getValue()) {
						for (ContributionReward reward : taskTemplate.getContrib()) {
							if (rewardsAdded <= reward.getNumber()) {
								rewardsAdded++;
								itemId = reward.getRewardId();
								itemCount = reward.getItemCount();
								String recipientName = DAOManager.getDAO(PlayerDAO.class).loadPlayerCommonData(objectId)
										.getName();
								GameFeatureServices.systemMailService().sendMail("Legion reward", recipientName, "", "", itemId,
										itemCount, 0, 0, LetterType.NORMAL);
								break;
							}
						}
					}
					e.getValue().clear();
				}
				winnersByPoints.clear();
				winnersByPoints = null;
			}
		}
	}

	/**
	 * 判断军团是否满足指定等级的挑战升级条件。
	 * Returns whether the legion meets the challenge requirement to raise the given level.
	 *
	 * legion id
	 *
	 * @param legionLevel 目标军团等级 / target legion level
	 * @param legionLevel
	 * @return 可升级返回 true / true if allowed
	 */
	public boolean canRaiseLegionLevel(int legionId, int legionLevel) {
		Map<Integer, ChallengeTask> tasks;
		if (legionTasks.containsKey(legionId)) {
			tasks = legionTasks.get(legionId);
		} else {
			tasks = DAOManager.getDAO(ChallengeTasksDAO.class).load(legionId, ChallengeType.LEGION);
		}
		for (ChallengeTask task : tasks.values()) {
			if (task.getTemplate().getMinLevel() == legionLevel && task.isCompleted()) {
				return true;
			}
		}
		return false;
	}
}
