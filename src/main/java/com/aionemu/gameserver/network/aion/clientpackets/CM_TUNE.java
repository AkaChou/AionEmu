package com.aionemu.gameserver.network.aion.clientpackets;

import com.aionemu.commons.utils.Rnd;
import com.aionemu.gameserver.controllers.observer.ItemUseObserver;
import com.aionemu.gameserver.model.DescriptionId;
import com.aionemu.gameserver.model.TaskId;
import com.aionemu.gameserver.model.gameobjects.Item;
import com.aionemu.gameserver.model.gameobjects.PersistentState;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.items.storage.Storage;
import com.aionemu.gameserver.model.templates.item.ItemTemplate;
import com.aionemu.gameserver.model.templates.item.actions.RetuningAction;
import com.aionemu.gameserver.network.aion.AionClientPacket;
import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.serverpackets.SM_INVENTORY_UPDATE_ITEM;
import com.aionemu.gameserver.network.aion.serverpackets.SM_ITEM_USAGE_ANIMATION;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * 客户端装备鉴定/调谐请求包；支持鉴定卷轴或直接随机开孔。
 * Client packet for item identify/tune; supports retuning scrolls or direct random socketing.
 *
 * @author Ranastic
 */
public class CM_TUNE extends AionClientPacket {
	private int tuningScrollId;
	private static int itemObjectId;

	/**
	 * packet opcode
	 * @param state 连接状态 / connection state
	 * @param restStates 其余允许状态 / additional allowed states
	 */
	public CM_TUNE(int opcode, AionConnection.State state, AionConnection.State... restStates) {
		super(opcode, state, restStates);
	}

	@Override
	protected void readImpl() {
		itemObjectId = readD();
		tuningScrollId = readD();
	}

	@Override
	protected void runImpl() {
		final Player player = getConnection().getActivePlayer();
		if (player == null)
			return;
		Storage inventory = player.getInventory();
		Item fitem = inventory.getItemByObjId(itemObjectId);
		// 修复：支持已装备物品的鉴定 / Fix: support identifying equipped items
		if (fitem == null && tuningScrollId != 0) {
			fitem = player.getEquipment().getEquippedItemByObjId(itemObjectId);
			if (fitem == null)
				return;
		}
		final Item item = fitem;
		if (item == null) {
			return;
		}
		if (tuningScrollId != 0) {
			final Item tuningItem = inventory.getItemByObjId(tuningScrollId);
			if (tuningItem == null) {
				return;
			}
			RetuningAction action = tuningItem.getItemSkinTemplate().getActions().getTuningAction();
			if (action != null && action.canAct(player, tuningItem, item)) {
				action.act(player, tuningItem, item);
			}
			return;
		}
		if (item.getOptionalSocket() != -1) {
			return;
		}
		// 修复：使用固定的动画 ID（166200022 神话装备鉴定卷轴）让客户端正确播放鉴定动画
		// 直接使用物品本身的 ID 可能没有对应的动画定义
		// Fix: use a fixed animation ID (166200022 Mythic identify scroll) so the client plays the identify animation
		// correctly; the item's own ID may have no matching animation definition
		final int itemId = 166200022;
		final ItemTemplate template = item.getItemTemplate();
		final int nameId = template.getNameId();
		PacketSendUtility.broadcastPacket(player,
				new SM_ITEM_USAGE_ANIMATION(player.getObjectId(), item.getObjectId(), itemId, 3000, 0, 0), true);
		final ItemUseObserver observer = new ItemUseObserver() {
			@Override
			public void abort() {
				if (player.getController().cancelTask(TaskId.ITEM_USE) == null) {
					player.getObserveController().removeObserver(this);
					return;
				}
				player.removeItemCoolDown(template.getUseLimits().getDelayId());
				PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_ITEM_CANCELED(new DescriptionId(nameId)));
				// 修复：取消时使用 endState = 3（取消）而不是 2（失败）
				// Fix: use endState = 3 (cancel) on abort instead of 2 (fail)
				PacketSendUtility.broadcastPacket(player,
						new SM_ITEM_USAGE_ANIMATION(player.getObjectId(), itemObjectId, itemId, 0, 3, 0), true);
				player.getObserveController().removeObserver(this);
			}
		};
		player.getObserveController().attach(observer);
		player.getController().scheduleTask(TaskId.ITEM_USE, new Runnable() {
			@Override
			public void run() {
				if (item.getOptionalSocket() != -1) {
					return;
				}
				player.getObserveController().removeObserver(observer);
				PacketSendUtility.broadcastPacket(player,
						new SM_ITEM_USAGE_ANIMATION(player.getObjectId(), itemObjectId, itemId, 0, 1, 1), true);
				item.setOptionalSocket(Rnd.get(0, item.getItemTemplate().getOptionSlotBonus()));
				/*
				 * if (item.getItemTemplate().getMaxEnchantBonus() > 0) {
				 * item.setEnchantBonus(Rnd.get(0,
				 * item.getItemTemplate().getMaxEnchantBonus())); }
				 */
				item.setRndBonus();
				item.setPersistentState(PersistentState.UPDATE_REQUIRED);
				PacketSendUtility.sendPacket(player, new SM_INVENTORY_UPDATE_ITEM(player, item));
				PacketSendUtility.sendPacket(player, new SM_SYSTEM_MESSAGE(1401626, new DescriptionId(nameId)));
			}
		}, 3000);
	}
}
