package com.aionemu.gameserver.model.templates.item.actions;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.commons.utils.Rnd;
import com.aionemu.gameserver.configs.main.EnchantsConfig;
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
 * EnchantStigma 动作模板（静态数据/XML）。
 * XML template.
 */

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "EnchantStigmaAction")
public class EnchantStigmaAction extends AbstractItemAction {
	@XmlAttribute(name = "count")
	private int count;

	@XmlAttribute(name = "min_level")
	private Integer min_level;

	@XmlAttribute(name = "max_level")
	private Integer max_level;

	@XmlAttribute(name = "stigma_only")
	private boolean stigma_only;

	@XmlAttribute(name = "chance")
	private float chance;

	/**
	 * @return 是否允许执行。 / Whether act
	  */
	@Override
	public boolean canAct(Player player, Item parentItem, Item targetItem) {
		if (parentItem == null || targetItem == null) {
			// 找不到该物品。 / The item cannot be found.
			PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_ITEM_COLOR_ERROR);
			return false;
		}
		if (targetItem.getEnchantLevel() >= 10) {
			// 你无法再强化 %0。 / You cannot enchant %0 any further.
			PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_ENCHANT_ITEM_IT_CAN_NOT_BE_ENCHANTED_MORE_TIME(targetItem.getNameId()));
			return false;
		}
		if (targetItem.isEquipped()) {
			PacketSendUtility.sendBrightYellowMessageOnCenter(player, "You can not enchant a stigma stone equipped.");
			return false;
		}
		if (player.getInventory().getKinah() < getStigmaByQuality(parentItem)) {
			PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_NOT_ENOUGH_MONEY);
			return false;
		}
		return true;
	}

	/** 执行 / act. */
	@Override
	public void act(final Player player, final Item parentItem, final Item targetItem) {
		if (!canAct(player, parentItem, targetItem)) {
			return;
		}
		final boolean isSuccess = Rnd.chance(EnchantsConfig.ENCHANT_STIGMA);
		final int parentItemId = parentItem.getItemId();
		final int parntObjectId = parentItem.getObjectId();
		final int parentNameId = parentItem.getNameId();
		final int nameId = targetItem.getNameId();
		PacketSendUtility.broadcastPacket(player, new SM_ITEM_USAGE_ANIMATION(player.getObjectId(), parentItem.getObjectId(), parentItemId, 3000, 0, 0), true);
		final ItemUseObserver observer = new ItemUseObserver() {
			/** 中止 / abort. */
			@Override
			public void abort() {
				if (player.getController().cancelTask(TaskId.ITEM_USE) == null) {
					player.getObserveController().removeObserver(this);
					return;
				}
				player.removeItemCoolDown(parentItem.getItemTemplate().getUseLimits().getDelayId());
				PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_ITEM_CANCELED(new DescriptionId(parentNameId)));
				// %0 的烙印之石强化已取消。 / Stigma enchantment of %0 has been cancelled.
				PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_STIGMA_ENCHANT_CANCEL(new DescriptionId(parentNameId)));
				PacketSendUtility.broadcastPacket(player, new SM_ITEM_USAGE_ANIMATION(player.getObjectId(), parntObjectId, parentItemId, 0, 2, 0), true);
				player.getObserveController().removeObserver(this);
			}
		};
		player.getObserveController().attach(observer);
		player.getController().scheduleTask(TaskId.ITEM_USE, new Runnable() {
			/** 运行 / run. */
			@Override
			public void run() {
				if (isSuccess) {
					player.getObserveController().removeObserver(observer);
					player.getInventory().decreaseKinah(getStigmaByQuality(parentItem));
					PacketSendUtility.broadcastPacket(player, new SM_ITEM_USAGE_ANIMATION(player.getObjectId(), parntObjectId, parentItemId, 0, 1, 1), true);
					PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_STIGMA_ENCHANT_SUCCESS(new DescriptionId(parentNameId)));
					player.getInventory().decreaseByObjectId(parentItem.getObjectId(), 1);
					targetItem.setEnchantLevel(targetItem.getEnchantLevel() + 1);
					targetItem.setPersistentState(PersistentState.UPDATE_REQUIRED);
					PacketSendUtility.sendPacket(player, new SM_INVENTORY_UPDATE_ITEM(player, targetItem));
					player.getInventory().setPersistentState(PersistentState.UPDATE_REQUIRED);
				} else {
					player.getObserveController().removeObserver(observer);
					player.getInventory().decreaseKinah(getStigmaByQuality(parentItem));
					PacketSendUtility.broadcastPacket(player, new SM_ITEM_USAGE_ANIMATION(player.getObjectId(), parntObjectId, parentItemId, 0, 2, 1), true);
					
					boolean hasStigmaProtection = false;
					if (player.getEffectController() != null) {
						hasStigmaProtection = player.getEffectController().hasEffectById(147141);
					}

					if (hasStigmaProtection) {
						int currentLevel = targetItem.getEnchantLevel();
						int newLevel = Math.max(0, currentLevel - 1);
						targetItem.setEnchantLevel(newLevel);
						
						PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_ITEM_AUTHORIZE_FAILED_NO_PENALTY(targetItem.getNameId()));
						
						PacketSendUtility.sendPacket(player, new SM_INVENTORY_UPDATE_ITEM(player, targetItem));
					} else {
						targetItem.setEnchantLevel(0);
						PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_STIGMA_ENCHANT_FAIL(new DescriptionId(parentNameId)));
						PacketSendUtility.sendPacket(player, new SM_INVENTORY_UPDATE_ITEM(player, targetItem));
					}
					
					targetItem.setPersistentState(PersistentState.UPDATE_REQUIRED);
					player.getInventory().setPersistentState(PersistentState.UPDATE_REQUIRED);
				}
			}
		}, 3000);
	}

	/** 按 quality 返回 stigma / Returns the stigma by quality */
	public static int getStigmaByQuality(Item item) {
		int price = 0;
		switch (item.getItemTemplate().getItemQuality()) {
		case RARE:
			price = 423;
			break;
		case LEGEND:
			price = 1271;
			break;
		case UNIQUE:
			price = 3813;
			break;
		default:
			break;
		}
		return price;
	}
}
