package com.aionemu.gameserver.model.templates.item.actions;

import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.commons.network.util.ThreadPoolManager;
import com.aionemu.commons.utils.Rnd;
import com.aionemu.gameserver.controllers.observer.ItemUseObserver;
import com.aionemu.gameserver.model.TaskId;
import com.aionemu.gameserver.model.gameobjects.Item;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_ITEM_USAGE_ANIMATION;
import com.aionemu.gameserver.services.item.ItemService;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * Composition 动作模板（静态数据/XML）。
 * XML template. / XML template.
 */

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "CompositionAction")
public class CompositionAction extends AbstractItemAction {
	/**
	 * @return 是否 act / 是否 act。 / Whether act / Whether act
	 */
	@Override
	public boolean canAct(Player player, Item parentItem, Item targetItem) {
		return false;
	}

	/** 执行 / act. */
	@Override
	public void act(Player player, Item parentItem, Item targetItem) {
	}

	/**
	 * @return 是否 act / 是否 act。 / Whether act / Whether act
	 */
	public boolean canAct(Player player, Item tools, Item first, Item second) {
		if (!tools.getItemTemplate().isCombinationItem())
			return false;
		if (!first.getItemTemplate().isEnchantmentStone())
			return false;
		if (!second.getItemTemplate().isEnchantmentStone())
			return false;
		if (first.getItemCount() < 1 || second.getItemCount() < 1)
			return false;
		if (first.getItemTemplate().getLevel() > 95 || second.getItemTemplate().getLevel() > 95)
			return false;
		return true;
	}

	/** 执行 / act. */
	public void act(final Player player, final Item tools, final Item first, final Item second) {
		PacketSendUtility.sendPacket(player, new SM_ITEM_USAGE_ANIMATION(player.getObjectId(), tools.getObjectId(),
				tools.getItemTemplate().getTemplateId(), 3000, 0, 0));
		player.getController().cancelTask(TaskId.ITEM_USE);
		final ItemUseObserver observer = new ItemUseObserver() {
			/** 中止 / abort. */
			@Override
			public void abort() {
				player.getController().cancelTask(TaskId.ITEM_USE);
				PacketSendUtility.sendPacket(player, new SM_ITEM_USAGE_ANIMATION(player.getObjectId(),
						tools.getObjectId(), tools.getItemTemplate().getTemplateId(), 0, 2, 0));
				player.getObserveController().removeObserver(this);
			}
		};
		player.getObserveController().attach(observer);
		player.getController().addTask(TaskId.ITEM_USE, GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			/** 运行 / run. */
			@Override
			public void run() {
				player.getObserveController().removeObserver(observer);
				boolean result = player.getInventory().decreaseByObjectId(tools.getObjectId(), 1);
				boolean result1 = player.getInventory().decreaseByObjectId(first.getObjectId(), 1);
				boolean result2 = player.getInventory().decreaseByObjectId(second.getObjectId(), 1);
				if (result && result1 && result2) {
					ItemService.addItem(player,
							getItemId(
									calcLevel(first.getItemTemplate().getLevel(), second.getItemTemplate().getLevel())),
							1);
				}
				PacketSendUtility.sendPacket(player, new SM_ITEM_USAGE_ANIMATION(player.getObjectId(),
						tools.getObjectId(), tools.getItemTemplate().getTemplateId(), 0, 1, 0));
			}
		}, 3000));
	}

	private int calcLevel(int first, int second) {
		int value = ((first + second) / 2);
		if (value < 11) {
			value = Rnd.get(1, 20);
		} else {
			int random = Rnd.get(1, 10);
			int bit = Rnd.get(0, 1);
			value = (bit == 0 ? value - random : value + random);
		}
		return value;
	}

	/** 返回物品 ID / Returns the item id */
	public int getItemId(int value) {
		return 166000000 + value;
	}
}
