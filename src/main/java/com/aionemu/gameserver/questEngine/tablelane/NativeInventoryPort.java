package com.aionemu.gameserver.questEngine.tablelane;

import java.util.List;
import java.util.Map;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.templates.quest.QuestItems;
import com.aionemu.gameserver.services.item.ItemService;

/**
 * 原生任务车道的背包端口：原版表声明的工作物品发放（{@code give_item*}）、扣除（{@code remove_item*}）
 * 与交付门的持有量查询（{@code item_check}）的**唯一出口**。
 * <p>
 * 原版 cab520（接取侧 20000 = SetQuestAcquired + GiveItem(0x410)）与 cabb10（中继步 = SetQuestProgress
 * + GiveItem(0x410) + RemoveItem(0x1d0)）的调用面在 Java 侧收敛到这里；处理器本身不直接触碰背包。
 * The inventory port of the native quest lane: the single exit for retail declared work-item grants
 * ({@code give_item*}), removals ({@code remove_item*}) and the hand-in hold check ({@code item_check}).
 * The retail cab520 accept side (20000 = SetQuestAcquired + GiveItem) and the cabb10 relay steps
 * (SetQuestProgress + GiveItem + RemoveItem) both funnel through this port.
 */
public interface NativeInventoryPort {

	/** 玩家当前持有的物品数量。 / The player's current hold count. */
	long count(Player player, int itemId);

	/** 发放物品（原版 GiveItem）。 / Grants the item (retail GiveItem). */
	void give(Player player, int itemId, int count);

	/** 扣除物品；数量不足返回 false 且不做任何变更。 / Removes the item; insufficient count fails without mutation. */
	boolean remove(Player player, int itemId, int count);

	/** 生产实现（在线玩家背包 + ItemService）。 / The live implementation. */
	static NativeInventoryPort live() {
		return ItemServiceInventoryPort.INSTANCE;
	}
}

/** 生产实现：ItemService 的任务物品发放 / 扣除与在线背包查询。 / Live ItemService-backed implementation. */
final class ItemServiceInventoryPort implements NativeInventoryPort {

	static final ItemServiceInventoryPort INSTANCE = new ItemServiceInventoryPort();

	private ItemServiceInventoryPort() {
	}

	@Override
	public long count(Player player, int itemId) {
		return player.getInventory().getItemCountByItemId(itemId);
	}

	@Override
	public void give(Player player, int itemId, int count) {
		if (count <= 0) {
			return;
		}
		ItemService.addQuestItems(player, List.of(new QuestItems(itemId, count)));
	}

	@Override
	public boolean remove(Player player, int itemId, int count) {
		if (count <= 0) {
			return true;
		}
		return ItemService.decreaseItems(player, Map.of(itemId, (long) count));
	}
}
