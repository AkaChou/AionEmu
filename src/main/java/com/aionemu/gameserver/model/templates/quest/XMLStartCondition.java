package com.aionemu.gameserver.model.templates.quest;

import java.util.List;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlList;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.lifecycle.GameEngineServices;
import com.aionemu.gameserver.model.DescriptionId;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.gameobjects.player.QuestStateList;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * XMLStartCondition 模板（静态数据/XML）。
 * XML template.
 *
 * @author antness
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "QuestStartConditions")
public class XMLStartCondition {

	@XmlElement(name = "finished")
	protected List<FinishedQuestCond> finished;
	@XmlList
	@XmlElement(name = "unfinished", type = Integer.class)
	protected List<Integer> unfinished;
	@XmlList
	@XmlElement(name = "noacquired", type = Integer.class)
	protected List<Integer> noacquired;
	@XmlList
	@XmlElement(name = "acquired", type = Integer.class)
	protected List<Integer> acquired;
	@XmlList
	@XmlElement(name = "equipped", type = Integer.class)
	protected List<Integer> equipped;

	/** 检查玩家是否已完成列出的任务。 / Check, if the player has finished listed quests */
	private boolean checkFinishedQuests(QuestStateList qsl) {
		if (finished != null && finished.size() > 0) {
			for (FinishedQuestCond fqc : finished) {
				int questId = fqc.getQuestId();
				int reward = fqc.getReward();
				QuestState qs = qsl.getQuestState(questId);
				if (qs == null || qs.getStatus() != QuestStatus.COMPLETE
						|| !checkReward(questId, reward, qs.getReward())) {
					return false;
				}
				var metadata = GameEngineServices.questEngine().questCatalog().findMetadata(questId).orElse(null);
				if (metadata == null) {
					return false;
				}
				if (metadata.repeatPolicy().maxRepeatCount() != 1) {
					if (metadata.repeatPolicy().maxRepeatCount() != 255
							&& qs.getCompleteCount() != metadata.repeatPolicy().maxRepeatCount()) {
						return false;
					}
				}
			}
		}
		return true;
	}

	/** 检查玩家是否未完成列出的任务。 / Check, if the player has not finished listed quests */
	private boolean checkUnfinishedQuests(QuestStateList qsl) {
		if (unfinished != null && unfinished.size() > 0) {
			for (Integer questId : unfinished) {
				QuestState qs = qsl.getQuestState(questId);
				if (qs != null && qs.getStatus() == QuestStatus.COMPLETE)
					return false;
			}
		}
		return true;
	}

	/** 检查玩家是否未接取列出的任务。 / Check, if the player has not acquired listed quests */
	private boolean checkNoAcquiredQuests(QuestStateList qsl) {
		if (noacquired != null && noacquired.size() > 0) {
			for (Integer questId : noacquired) {
				QuestState qs = qsl.getQuestState(questId);
				if (qs != null && (qs.getStatus() == QuestStatus.START || qs.getStatus() == QuestStatus.REWARD
						|| qs.getStatus() == QuestStatus.COMPLETE))
					return false;
			}
		}
		return true;
	}

	/** 检查玩家是否已接取列出的任务。 / Check, if the player has acquired listed quests */
	private boolean checkAcquiredQuests(QuestStateList qsl) {
		if (acquired != null && acquired.size() > 0) {
			for (Integer questId : acquired) {
				QuestState qs = qsl.getQuestState(questId);
				if (qs == null || qs.getStatus() == QuestStatus.NONE || qs.getStatus() == QuestStatus.LOCKED) {
					return false;
				}
			}
		}
		return true;
	}

	private boolean checkEquippedItems(Player player, boolean warn) {
		if (!warn)
			return true;
		if (equipped != null && equipped.size() > 0) {
			for (int itemId : equipped) {
				if (!player.getEquipment().getEquippedItemIds().contains(itemId)) {
					int requiredItemNameId = DataManager.ITEM_DATA.getItemTemplate(itemId).getNameId();
					PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE
							.STR_QUEST_ACQUIRE_ERROR_EQUIP_ITEM(new DescriptionId(requiredItemNameId)));
					return false;
				}
			}
		}
		return true;
	}

	/**
	 * 检查所有开始条件。
	 * Checks all start conditions.
	 *
	 * @param player 玩家 / player
	 * @param warn 不满足时是否发送警告消息 / whether to warn on failure
	 * @return 条件是否全部满足 / whether all conditions hold
	 */
	public boolean check(Player player, boolean warn) {
		QuestStateList qsl = player.getQuestStateList();
		return checkFinishedQuests(qsl) && checkUnfinishedQuests(qsl) && checkAcquiredQuests(qsl)
				&& checkNoAcquiredQuests(qsl) && checkEquippedItems(player, warn);
	}

	private boolean checkReward(int questId, int neededReward, int currentReward) {
		// 在欧比斯入场任务奖励正确前的临时例外任务。 / Temporary exceptions-quests till abyss entry quests work with correct reward
		if (neededReward != currentReward && questId != 2947 && questId != 1922) {
			return false;
		}
		return true;
	}

	/** 返回已完成任务前置条件 / Returns the finished preconditions */
	public List<FinishedQuestCond> getFinishedPreconditions() {
		return finished;
	}
}
