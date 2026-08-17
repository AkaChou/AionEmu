package com.aionemu.gameserver.model.templates.item.actions;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.commons.database.dao.DAOManager;
import com.aionemu.gameserver.controllers.observer.ItemUseObserver;
import com.aionemu.gameserver.dao.ItemStoneListDAO;
import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.model.DescriptionId;
import com.aionemu.gameserver.model.TaskId;
import com.aionemu.gameserver.model.gameobjects.Item;
import com.aionemu.gameserver.model.gameobjects.PersistentState;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.items.IdianStone;
import com.aionemu.gameserver.model.items.RandomBonusResult;
import com.aionemu.gameserver.model.templates.item.bonuses.StatBonusType;
import com.aionemu.gameserver.network.aion.serverpackets.SM_INVENTORY_UPDATE_ITEM;
import com.aionemu.gameserver.network.aion.serverpackets.SM_ITEM_USAGE_ANIMATION;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * 伊迪安动作模板（静态数据/XML）。
 * XML template.
 */

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "IdianAction")
public class IdianAction extends AbstractItemAction {
	@XmlAttribute(name = "setId")
	protected int polishSetId;

	/**
	 * @return 是否允许执行。 / Whether act
	  */
	@Override
	public boolean canAct(Player player, Item parentItem, Item targetItem) {
		int idianKinah = 68647;
		if (parentItem == null || targetItem == null) {
			// 找不到该物品。 / The item cannot be found.
			PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_ITEM_COLOR_ERROR);
			return false;
		}
		if (parentItem.getItemTemplate().getLevel() > targetItem.getItemTemplate().getLevel()) {
			// 无法向所选物品镶嵌伊迪安。伊迪安等级过高。 / You cannot socket Idian to the selected item. The Idian's level is too high.
			PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_POLISH_WRONG_LEVEL);
			return false;
		}
		if (targetItem.hasRetuning()) {
			// 镶嵌伊迪安前须先调谐装备。 / You need to tune your equipment before socketing Idian.
			PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_POLISH_NEED_IDENTIFY);
			return false;
		}
		if (player.getInventory().getKinah() < idianKinah) {
			PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_NOT_ENOUGH_MONEY);
			return false;
		}
		if (player.getInventory().getKinah() >= idianKinah) {
			player.getInventory().decreaseKinah(idianKinah);
		}
		return !player.isAttackMode() && targetItem.getItemTemplate().isWeapon()
				&& targetItem.getItemTemplate().isCanIdian();
	}

	/** 执行 / act. */
	@Override
	public void act(final Player player, final Item parentItem, final Item targetItem) {
		final int parentItemId = parentItem.getItemId();
		final int parntObjectId = parentItem.getObjectId();
		final int parentNameId = parentItem.getNameId();
		PacketSendUtility.broadcastPacket(player,
				new SM_ITEM_USAGE_ANIMATION(player.getObjectId(), parentItem.getObjectId(), parentItemId, 3000, 0, 0),
				true);
		final ItemUseObserver observer = new ItemUseObserver() {
			/** 中止 / abort. */
			@Override
			public void abort() {
				if (player.getController().cancelTask(TaskId.ITEM_USE) == null) {
					player.getObserveController().removeObserver(this);
					return;
				}
				player.removeItemCoolDown(parentItem.getItemTemplate().getUseLimits().getDelayId());
				// 已取消 %0 的伊迪安镶嵌。 / Canceled %0 Idian socketing.
				PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_POLISH_CANCELED(targetItem.getNameId()));
				PacketSendUtility.broadcastPacket(player,
						new SM_ITEM_USAGE_ANIMATION(player.getObjectId(), parntObjectId, parentItemId, 0, 2, 0), true);
				player.getObserveController().removeObserver(this);
			}
		};
		player.getObserveController().attach(observer);
		player.getController().scheduleTask(TaskId.ITEM_USE, new Runnable() {
			/** 运行 / run. */
			@Override
			public void run() {
				player.getObserveController().removeObserver(observer);
				PacketSendUtility.broadcastPacket(player,
						new SM_ITEM_USAGE_ANIMATION(player.getObjectId(), parntObjectId, parentItemId, 0, 1, 1), true);
				if (!player.getInventory().decreaseByObjectId(parntObjectId, 1)) {
					return;
				}
				RandomBonusResult bonus = DataManager.ITEM_RANDOM_BONUSES.getRandomModifiers(StatBonusType.POLISH,
						polishSetId);
				if (bonus == null) {
					PacketSendUtility.sendPacket(player,
							SM_SYSTEM_MESSAGE.STR_ENCHANT_ITEM_FAILED(new DescriptionId(parentNameId)));
					return;
				}
				// %0 的伊迪安已充满。 / %0's Idian is fully charged.
				PacketSendUtility.sendPacket(player,
						SM_SYSTEM_MESSAGE.STR_MSG_POLISH_SUCCEED(targetItem.getItemTemplate().getNameId()));
				IdianStone idianStone = targetItem.getIdianStone();
				if (idianStone != null) {
					idianStone.onUnEquip(player);
					targetItem.setIdianStone(null);
					idianStone.setPersistentState(PersistentState.DELETED);
					DAOManager.getDAO(ItemStoneListDAO.class).storeIdianStones(idianStone);
				}
				idianStone = new IdianStone(parentItemId, PersistentState.NEW, targetItem, bonus.getTemplateNumber(),
						1000000);
				targetItem.setIdianStone(idianStone);
				if (targetItem.isEquipped()) {
					idianStone.onEquip(player);
				}
				PacketSendUtility.sendPacket(player, new SM_INVENTORY_UPDATE_ITEM(player, targetItem));
			}
		}, 3000);
	}

	/** 返回 polish set id / Returns the polish set id */
	public int getPolishSetId() {
		return polishSetId;
	}
}
