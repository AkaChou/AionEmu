package com.aionemu.gameserver.model.templates.item.actions;

import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.controllers.observer.ItemUseObserver;
import com.aionemu.gameserver.model.DescriptionId;
import com.aionemu.gameserver.model.TaskId;
import com.aionemu.gameserver.model.gameobjects.Item;
import com.aionemu.gameserver.model.gameobjects.PersistentState;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_INVENTORY_UPDATE_ITEM;
import com.aionemu.gameserver.network.aion.serverpackets.SM_ITEM_USAGE_ANIMATION;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * Wrapping 动作模板（静态数据/XML）。
 * XML template. / XML template.
 *
 * @author Ranastic
 */

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "WrappingAction")
public class WrappingAction extends AbstractItemAction {
	@XmlAttribute
	UseTarget target;

	/**
	 * @return 是否 act / 是否 act。 / Whether act / Whether act
	 */
	@Override
	public boolean canAct(Player player, Item parentItem, Item targetItem) {
		if (target.equals(UseTarget.WEAPON) && !targetItem.getItemTemplate().isWeapon()) {
			return false;
		}
		if (target.equals(UseTarget.ARMOR) && !targetItem.getItemTemplate().isArmor()) {
			return false;
		}
		return targetItem.getWrappableCount() < targetItem.getItemTemplate().getWrappableCount()
				&& !targetItem.isEquipped();
	}

	/** 执行 / act. */
	@Override
	public void act(final Player player, final Item parentItem, final Item targetItem) {
		final int parentItemId = parentItem.getItemId();
		final int parntObjectId = parentItem.getObjectId();
		final int nameId = parentItem.getNameId();
		PacketSendUtility.broadcastPacket(player,
				new SM_ITEM_USAGE_ANIMATION(player.getObjectId(), parentItem.getObjectId(), parentItemId, 3000, 0, 0),
				true);
		final ItemUseObserver observer = new ItemUseObserver() {
			/** 中止 / abort. */
			@Override
			public void abort() {
				player.getController().cancelTask(TaskId.ITEM_USE);
				player.removeItemCoolDown(parentItem.getItemTemplate().getUseLimits().getDelayId());
				PacketSendUtility.sendPacket(player, new SM_SYSTEM_MESSAGE(1402015, new DescriptionId(nameId)));
				PacketSendUtility.broadcastPacket(player,
						new SM_ITEM_USAGE_ANIMATION(player.getObjectId(), parntObjectId, parentItemId, 0, 2, 0), true);
				player.getObserveController().removeObserver(this);
			}
		};
		player.getObserveController().attach(observer);
		player.getController().addTask(TaskId.ITEM_USE, GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			/** 运行 / run. */
			@Override
			public void run() {
				player.getObserveController().removeObserver(observer);
				PacketSendUtility.broadcastPacket(player,
						new SM_ITEM_USAGE_ANIMATION(player.getObjectId(), parntObjectId, parentItemId, 0, 1, 1), true);
				if (!player.getInventory().decreaseByObjectId(parntObjectId, 1)) {
					return;
				}
				int wrappableCount = targetItem.getWrappableCount();
				if (wrappableCount >= targetItem.getItemTemplate().getWrappableCount()) {
					return;
				}
				if (targetItem.isEquipped()) {
					PacketSendUtility.sendPacket(player, new SM_SYSTEM_MESSAGE(1402020));
					return;
				}
				if (targetItem.isTradeable(player)) {
					PacketSendUtility.sendPacket(player, new SM_SYSTEM_MESSAGE(1402022));
					return;
				}
				if (targetItem.getItemTemplate().getItemQuality() != targetItem.getItemTemplate().getItemQuality()) {
					PacketSendUtility.sendPacket(player, new SM_SYSTEM_MESSAGE(1402018, new DescriptionId(nameId)));
					return;
				}
				if (targetItem.getWrappableCount() > targetItem.getItemTemplate().getWrappableCount()) {
					PacketSendUtility.sendPacket(player, new SM_SYSTEM_MESSAGE(1402015, new DescriptionId(nameId)));
					return;
				}
				targetItem.setPacked(true);
				targetItem.setWrappableCount(++wrappableCount);
				targetItem.setPersistentState(PersistentState.UPDATE_REQUIRED);
				PacketSendUtility.sendPacket(player, new SM_INVENTORY_UPDATE_ITEM(player, targetItem));
				PacketSendUtility.sendPacket(player, new SM_SYSTEM_MESSAGE(1402031, new DescriptionId(nameId)));
			}
		}, 3000));
	}
}
