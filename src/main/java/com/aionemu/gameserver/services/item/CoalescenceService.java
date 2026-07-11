package com.aionemu.gameserver.services.item;

import lombok.extern.slf4j.Slf4j;
import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.springframework.beans.factory.ObjectProvider;

import com.aionemu.commons.utils.Rnd;
import com.aionemu.gameserver.controllers.observer.ItemUseObserver;
import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.model.DescriptionId;
import com.aionemu.gameserver.model.TaskId;
import com.aionemu.gameserver.model.gameobjects.Item;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.templates.item.ItemTemplate;
import com.aionemu.gameserver.network.aion.serverpackets.SM_COALESCENCE_RESULT;
import com.aionemu.gameserver.network.aion.serverpackets.SM_ITEM_USAGE_ANIMATION;
import com.aionemu.gameserver.network.aion.serverpackets.SM_QUEST_ACTION;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.services.item.ItemPacketService.ItemDeleteType;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.audit.AuditLogger;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.QuestEngine;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.questEngine.model.QuestState;

/**
 * 物品融合服务：消耗核心与材料，随机产出同槽位大天使装备。
 * Item coalescence service — consumes core and materials, rolls same-slot Archdaeva gear.
 *
 * @author Ranastic
 */
@Slf4j
public class CoalescenceService {

	/** Spring ObjectProvider preferred over local singleton / Spring ObjectProvider preferred over local singleton */
	private static volatile ObjectProvider<CoalescenceService> instanceProvider;

	/**
	 * 执行融合：播放读条动画，消耗核心与材料，随机发放同槽位大天使装备，并按材料数判定奖励。
	 * Performs coalescence: plays cast animation, consumes core and materials, grants random same-slot Archdaeva gear, and rolls bonus by material count.
	 *
	 * @param player 玩家 / player
	 * @param core_item_object_id 核心物品对象 ID / core item object id
	 * @param material_item_object_id_collection 材料物品对象 ID 列表 / material item object ids
	 */
	public void letsCoalescence(final Player player, int core_item_object_id, final List<Integer> material_item_object_id_collection) {
		final Item core_item = player.getInventory().getItemByObjId(core_item_object_id);
		if (core_item.getEnchantLevel() == 25) {
			PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_CANT_ENCHANT_ITEM);
			return;
		}
		if (material_item_object_id_collection.size() == 0) {
			AuditLogger.info(player.getName(), player.getObjectId(), "Possible hack Coalescence. His material equals 0");
			return;
		}
		PacketSendUtility.broadcastPacket(player, new SM_ITEM_USAGE_ANIMATION(player.getObjectId(), core_item.getObjectId(), core_item.getItemId(), 4000, 23, 68), true);
		final ItemUseObserver observer = new ItemUseObserver() {
			@Override
			public void abort() {
				player.getController().cancelTask(TaskId.ITEM_USE);
				player.removeItemCoolDown(core_item.getItemTemplate().getUseLimits().getDelayId());
				PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_ITEM_CANCELED(new DescriptionId(core_item.getItemTemplate().getNameId())));
				PacketSendUtility.broadcastPacket(player, new SM_ITEM_USAGE_ANIMATION(player.getObjectId(), core_item.getObjectId(), core_item.getItemId(), 0, 2, 0), true);
				player.getObserveController().removeObserver(this);
			}
		};
		player.getObserveController().attach(observer);
		player.getController().addTask(TaskId.ITEM_USE, GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			@Override
			public void run() {
				player.getObserveController().removeObserver(observer);
				player.getInventory().delete(core_item, ItemDeleteType.COALESCENCE);
				for (int i = 0; i < material_item_object_id_collection.size(); i++) {
					final Item mats = player.getInventory().getItemByObjId(material_item_object_id_collection.get(i));
					player.getInventory().delete(mats, ItemDeleteType.COALESCENCE);
				}
				List<Integer> ids_collections = new ArrayList<Integer>();
				int item_id_taken = 0;
				int bonus_item_id_taken = 0;
				int bonus_item_count = 0;
				Map<Integer, ItemTemplate> item_templates = DataManager.ITEM_DATA.getAllItems();
				for (ItemTemplate item_template : item_templates.values()) {
					if (item_template.isArchdaeva() && item_template.getEquipmentType() == core_item.getEquipmentType() && (item_template.getLevel() >= 66 && item_template.getLevel() <= 74) && !item_template.getName().contains("n_m3_") && !item_template.getName().contains("npc_") && !item_template.getName().contains("Pvp_") && !item_template.getName().contains("dagger_") && !item_template.getName().contains("polearm_d_") && !item_template.getName().contains("polearm_a_") && !item_template.getName().contains("polearm_")) {
						ids_collections.add(item_template.getTemplateId());
					}
				}
				Collections.shuffle(ids_collections);
				item_id_taken = ids_collections.get(0);
				if (item_id_taken == 0) {
					return;
				}
				ItemService.addItem(player, item_id_taken, 1);
				float success = 15;
				if (material_item_object_id_collection.size() == 1) {
					success += 5;
				} else if (material_item_object_id_collection.size() == 2) {
					success += 10;
				} else if (material_item_object_id_collection.size() == 3) {
					success += 15;
				} else if (material_item_object_id_collection.size() == 4) {
					success += 20;
				} else if (material_item_object_id_collection.size() == 5) {
					success += 25;
				} else if (material_item_object_id_collection.size() == 6) {
					success += 30;
				}
				if (success >= 95) {
					success = 95;
				}
				boolean result_of_random = false;
				float random = Rnd.get(1, 1000) / 10f;
				if (random <= success) {
					result_of_random = true;
					Random rand = new Random();
					int[] bonus_item_id_collection = new int[] { 166100009, 166100010, 166100011 };
					bonus_item_id_taken = bonus_item_id_collection[rand.nextInt(bonus_item_id_collection.length)];
					bonus_item_count = Rnd.get(1, 200);
					ItemService.addItem(player, bonus_item_id_taken, bonus_item_count);
				}
				PacketSendUtility.broadcastPacket(player, new SM_ITEM_USAGE_ANIMATION(player.getObjectId(), core_item.getObjectId(), core_item.getItemId(), 0, 24, 0), true);
				DescriptionId resultItem = new DescriptionId(DataManager.ITEM_DATA.getItemTemplate(item_id_taken).getNameId());
				if (result_of_random) {
					DescriptionId bonusItem = new DescriptionId(DataManager.ITEM_DATA.getItemTemplate(bonus_item_id_taken).getNameId());
					PacketSendUtility.sendPacket(player, new SM_SYSTEM_MESSAGE(1403621, resultItem, bonusItem));
				} else {
					PacketSendUtility.sendPacket(player, new SM_SYSTEM_MESSAGE(1403620, resultItem));
				}
				PacketSendUtility.sendPacket(player, new SM_COALESCENCE_RESULT(core_item.getItemId(), core_item.getObjectId(), bonus_item_id_taken, bonus_item_count, result_of_random));
				updateQuestsOnCoalescenceComplete(player, core_item, result_of_random);
			}
		}, 4000));
	}

	/**
	 * 融合完成后推进相关任务（15542 / 25542）至可领奖状态。
	 * 25542) to reward status after coalescence completes. / 25542) to reward status after coalescence completes.
	 *
	 * 玩家 / player
	 * core item
	 * @param success 是否触发奖励 / whether bonus reward succeeded
	 */
	public void updateQuestsOnCoalescenceComplete(Player player, Item coreItem, boolean success) {
		if (player.getQuestStateList().hasQuest(15542)) {
			QuestState qs = player.getQuestStateList().getQuestState(15542);
			if (qs != null && qs.getStatus() == QuestStatus.START) {
				qs.setStatus(QuestStatus.REWARD);
				PacketSendUtility.sendPacket(player, new SM_QUEST_ACTION(15542, qs.getStatus(), qs.getQuestVars().getQuestVars()));
				player.getController().updateNearbyQuests();
			}
		}

		if (player.getQuestStateList().hasQuest(25542)) {
			QuestState qs = player.getQuestStateList().getQuestState(25542);
			if (qs != null && qs.getStatus() == QuestStatus.START) {
				qs.setStatus(QuestStatus.REWARD);
				PacketSendUtility.sendPacket(player, new SM_QUEST_ACTION(25542, qs.getStatus(), qs.getQuestVars().getQuestVars()));
				player.getController().updateNearbyQuests();
			}
		}
	}

	/**
	 * 获取服务单例，优先走 Spring ObjectProvider。
	 * Returns the service singleton, preferring Spring ObjectProvider when available.
	 *
	 * service instance
	 */
	public static CoalescenceService getInstance() {
		ObjectProvider<CoalescenceService> provider = instanceProvider;
		if (provider != null) {
			return provider.getIfAvailable(() -> NewSingletonHolder.INSTANCE);
		}
		return NewSingletonHolder.INSTANCE;
	}

	/**
	 * 注入 Spring ObjectProvider 以覆盖默认单例。
	 * Injects a Spring ObjectProvider to override the default singleton.
	 *
	 * provider
	 */
	public static void setInstanceProvider(ObjectProvider<CoalescenceService> provider) {
		instanceProvider = provider;
	}

	/**
	 * 本地默认单例持有者。
	 * Local default singleton holder.
	 */
	private static class NewSingletonHolder {
		private static final CoalescenceService INSTANCE = new CoalescenceService();
	}
}
