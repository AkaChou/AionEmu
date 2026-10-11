package com.aionemu.gameserver.questEngine;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.aionemu.commons.utils.collections.IntArrayList;
import com.aionemu.gameserver.model.DescriptionId;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.Item;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.gameobjects.player.RequestResponseHandler;
import com.aionemu.gameserver.network.aion.serverpackets.SM_QUESTION_WINDOW;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.questEngine.tablelane.DataDrivenNativeRuntime;
import com.aionemu.gameserver.services.QuestService;
import com.aionemu.gameserver.services.item.ItemPacketService.ItemDeleteType;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * 任务物品销毁守卫：镜像原版 {@code User_DestroyItem} 的
 * 「进行中任务引用 → 确认放弃 → 删除」收口。
 * Quest-item destroy guard: mirrors the retail {@code User_DestroyItem} flow of "in-progress
 * references → confirm giveup → delete".
 * <p>判定与确认窗语义逐字保持原 {@code QuestEngine} 行为。</p>
 * <p>The verdict and confirmation semantics reproduce the former {@code QuestEngine} behavior
 * verbatim.</p>
 */
final class QuestItemDestroyGuard {

	private final QuestEventRegistry registry;

	QuestItemDestroyGuard(QuestEventRegistry registry) {
		this.registry = registry;
	}

	/**
	 * 销毁任务物品前的确认与收口（原版 `User_DestroyItem` + 确认回复 `0x249f1` 分支镜像）：
	 * 物品被玩家进行中的任务引用（原版 = item 模板静态 quest 列表）且全部不可放弃 ⇒ 拒绝并提示
	 * （1300604 = 原版 0x13d87c）；否则弹确认窗（150001）——确认 ⇒ 依次放弃相关任务
	 * （`User_DeleteQuest`）后删除物品；拒绝 ⇒ 提示重试（1300605 = 原版 0x13d87d）。
	 * The retail destroy flow: referencing in-progress quests gate a confirmation window; on confirm
	 * the quests are stopped (quest giveup) and the item deleted.
	 *
	 * @param player 玩家 / the player
	 * @param item   待销毁物品 / the item being destroyed
	 * @return true = 本方法认领销毁（已弹窗或已提示拒绝）；false = 无进行中任务引用，走普通销毁
	 */
	public boolean tryQuestItemDestroy(Player player, Item item) {
		if (player == null || item == null || item.getItemTemplate() == null) {
			return false;
		}
		int itemId = item.getItemTemplate().getTemplateId();
		List<Integer> referencing = activeQuestsReferencingItem(player, itemId);
		if (referencing.isEmpty()) {
			return false;
		}
		boolean anyAbandonable = false;
		for (int questId : referencing) {
			if (QuestService.canAbandon(player, questId)) {
				anyAbandonable = true;
				break;
			}
		}
		if (!anyAbandonable) {
			PacketSendUtility.sendPacket(player,
				SM_SYSTEM_MESSAGE.STR_QUEST_GIVEUP_WHEN_DELETE_QUEST_ITEM_IMPOSSIBLE(item.getItemName()));
			return true;
		}
		RequestResponseHandler handler = questItemDestroyResponder(player, itemId, item.getObjectId());
		if (!player.getResponseRequester().putRequest(SM_QUESTION_WINDOW.STR_QUEST_GIVEUP_WHEN_DELETE_QUEST_ITEM,
			handler)) {
			// 有在途问询：原版重试提示（1300605）；物品不删。
			// A pending request blocks the ask: the retail retry message, item kept.
			PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_QUEST_GIVEUP_WHEN_DELETE_QUEST_ITEM_RETRY);
			return true;
		}
		PacketSendUtility.sendPacket(player, new SM_QUESTION_WINDOW(
			SM_QUESTION_WINDOW.STR_QUEST_GIVEUP_WHEN_DELETE_QUEST_ITEM, 0, 0, new DescriptionId(item.getNameId())));
		return true;
	}

	/**
	 * 构造「销毁任务物品需先放弃任务」确认窗的应答处理器。
	 * Builds the responder for the quest-item destroy confirmation window.
	 * @param player 发起销毁的玩家 / the player requesting the destroy
	 * @param itemId 物品模板 id / the item template id
	 * @param objectId 物品对象 id / the item object id
	 * @return 确认/拒绝应答处理器 / the confirmation responder
	 */
	private RequestResponseHandler questItemDestroyResponder(Player player, int itemId, int objectId) {
		return new RequestResponseHandler(player) {
			@Override
			public void acceptRequest(Creature requester, Player responder) {
				Item current = responder.getInventory() == null ? null
					: responder.getInventory().getItemByObjId(objectId);
				if (current == null) {
					return;
				}
				for (int questId : activeQuestsReferencingItem(responder, itemId)) {
					QuestService.abandonQuest(responder, questId);
				}
				responder.getInventory().delete(current, ItemDeleteType.DISCARD);
			}

			@Override
			public void denyRequest(Creature requester, Player responder) {
				PacketSendUtility.sendPacket(responder,
					SM_SYSTEM_MESSAGE.STR_QUEST_GIVEUP_WHEN_DELETE_QUEST_ITEM_RETRY);
			}
		};
	}

	/**
	 * 玩家进行中（START/REWARD）且引用该物品的任务：typed 目录索引（{@code questItems} / {@code questItemRelated}）
	 * ∪ DD 原生行引用面（发/扣物品动作 + ItemPlay 载荷 + 接取行动作）。原版等价物 = item
	 * 模板静态 quest 列表（≤2 条）∩ 进行中状态。
	 * In-progress quests referencing the item (the typed indexes ∪ the DD row references).
	 *
	 * @param player 玩家 / the player
	 * @param itemId 物品模板 id / the item template id
	 * @return 按发现顺序的任务 id 列表 / the quest ids in discovery order
	 */
	public List<Integer> activeQuestsReferencingItem(Player player, int itemId) {
		if (player == null || player.getQuestStateList() == null || itemId <= 0) {
			return List.of();
		}
		Set<Integer> candidates = new LinkedHashSet<>();
		IntArrayList typed = registry.questItems.get(itemId);
		if (typed != null) {
			candidates.addAll(typed);
		}
		IntArrayList related = registry.questItemRelated.get(itemId);
		if (related != null) {
			candidates.addAll(related);
		}
		try {
			candidates.addAll(DataDrivenNativeRuntime.instance().questsReferencingItem(itemId));
		} catch (RuntimeException ignored) {
			// DD 面缺席（部分装配/测试）不影响 typed 面判定。
			// A missing DD face does not block the typed verdict.
		}
		List<Integer> active = new ArrayList<>();
		for (int questId : candidates) {
			QuestState state = player.getQuestStateList().getQuestState(questId);
			if (state != null && (state.getStatus() == QuestStatus.START || state.getStatus() == QuestStatus.REWARD)) {
				active.add(questId);
			}
		}
		return List.copyOf(active);
	}
}
