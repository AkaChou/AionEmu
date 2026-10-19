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
import com.aionemu.gameserver.network.aion.serverpackets.SM_ITEM_USAGE_ANIMATION;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.services.EnchantService;
import com.aionemu.gameserver.services.item.ItemPacketService;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * 烙印之石强化动作模板，完成时先同步结果属性再通知强化窗口。
 * Stigma enchant action template, syncing result attributes before notifying the enchant window.
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

	/**
	 * 执行烙印强化，按既有保护规则应用结果，并在刷新物品后发送完成通知。
	 * Enchants a stigma using the existing protection rules, refreshing the item before sending completion.
	 */
	@Override
	public void act(final Player player, final Item parentItem, final Item targetItem) {
		if (!canAct(player, parentItem, targetItem)) {
			return;
		}
		final float successRate = EnchantsConfig.ENCHANT_STIGMA;
		EnchantService.sendUpgradeChance(player, targetItem, "item.upgrade.stigma", successRate);
		final boolean isSuccess = Rnd.chance(successRate);
		final int parentItemId = parentItem.getItemId();
		final int parentObjectId = parentItem.getObjectId();
		final int parentNameId = parentItem.getNameId();
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
				PacketSendUtility.broadcastPacket(player, new SM_ITEM_USAGE_ANIMATION(player.getObjectId(), parentObjectId, parentItemId, 0, 2, 0), true);
				player.getObserveController().removeObserver(this);
			}
		};
		player.getObserveController().attach(observer);
		player.getController().scheduleTask(TaskId.ITEM_USE, () -> {
			player.getObserveController().removeObserver(observer);
			player.getInventory().decreaseKinah(getStigmaByQuality(parentItem));
			SM_SYSTEM_MESSAGE resultMessage;
			if (isSuccess) {
				player.getInventory().decreaseByObjectId(parentObjectId, 1);
				targetItem.setEnchantLevel(targetItem.getEnchantLevel() + 1);
				resultMessage = SM_SYSTEM_MESSAGE.STR_MSG_STIGMA_ENCHANT_SUCCESS(new DescriptionId(parentNameId));
			} else if (player.getEffectController() != null
				&& player.getEffectController().hasEffectById(147141)) {
				targetItem.setEnchantLevel(Math.max(0, targetItem.getEnchantLevel() - 1));
				resultMessage = SM_SYSTEM_MESSAGE.STR_MSG_ITEM_AUTHORIZE_FAILED_NO_PENALTY(targetItem.getNameId());
			} else {
				targetItem.setEnchantLevel(0);
				resultMessage = SM_SYSTEM_MESSAGE.STR_MSG_STIGMA_ENCHANT_FAIL(new DescriptionId(parentNameId));
			}
			targetItem.setPersistentState(PersistentState.UPDATE_REQUIRED);
			player.getInventory().setPersistentState(PersistentState.UPDATE_REQUIRED);
			ItemPacketService.sendEnchantResult(player, parentItem, targetItem, isSuccess);
			PacketSendUtility.sendPacket(player, resultMessage);
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
