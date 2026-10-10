package com.aionemu.gameserver.model.templates.item.actions;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.commons.utils.Rnd;
import com.aionemu.gameserver.controllers.observer.ItemUseObserver;
import com.aionemu.gameserver.model.DescriptionId;
import com.aionemu.gameserver.model.TaskId;
import com.aionemu.gameserver.model.gameobjects.Item;
import com.aionemu.gameserver.model.gameobjects.PersistentState;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_INVENTORY_UPDATE_ITEM;
import com.aionemu.gameserver.network.aion.serverpackets.SM_ITEM_USAGE_ANIMATION;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.network.aion.serverpackets.SM_TUNE_RESULT;
import com.aionemu.gameserver.utils.PacketSendUtility;


@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "RetuningAction")
public class RetuningAction extends AbstractItemAction {
	@XmlAttribute
	UseTarget target;

	/**
	 * 校验重鉴定条件：目标类型、使用等级、品质、可重鉴定次数与装备状态。
	 * Validates reidentify conditions: target type, required level, quality, remaining tune count and equip state.
	 *
	 * @param player 使用卷轴的玩家 / player using the scroll
	 * @param parentItem 重新鉴定卷轴 / retuning scroll
	 * @param targetItem 目标装备 / target equipment
	 * @return 允许执行则为 true / true when the action is allowed
	 */
	@Override
	public boolean canAct(Player player, Item parentItem, Item targetItem) {
		if (target == null || targetItem == null || parentItem == null) {
			return false;
		}
		// 类型匹配：卷轴用途与目标装备类别不符 / Type match: scroll target does not match the equipment category
		if (target.equals(UseTarget.WEAPON) && !targetItem.getItemTemplate().isWeapon()) {
			PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_ITEM_REIDENTIFY_WRONG_SELECT(
					parentItem.getItemName(), targetItem.getItemName()));
			return false;
		}
		if (target.equals(UseTarget.ARMOR) && !targetItem.getItemTemplate().isArmor()) {
			PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_ITEM_REIDENTIFY_WRONG_SELECT(
					parentItem.getItemName(), targetItem.getItemName()));
			return false;
		}
		// 使用等级：卷轴等级低于目标装备时拒绝 / Required level: reject when the scroll's level is lower than the target's
		if (parentItem.getItemTemplate().getLevel() < targetItem.getItemTemplate().getLevel()) {
			PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_ITEM_REIDENTIFY_WRONG_LEVEL(
					parentItem.getItemName(), targetItem.getItemName()));
			return false;
		}
		// 品质：卷轴品质低于目标装备品质时拒绝 / Quality: reject when the scroll's quality is lower than the target's
		if (parentItem.getItemTemplate().getItemQuality().getQualityId() < targetItem.getItemTemplate().getItemQuality()
				.getQualityId()) {
			PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_ITEM_REIDENTIFY_WRONG_QUALITY(
					parentItem.getItemName(), targetItem.getItemName()));
			return false;
		}
		// 可重鉴定次数：已达上限的装备无法再重新鉴定 / Remaining tune count: an item at its limit cannot be retuned
		if (targetItem.getRandomCount() >= targetItem.getItemTemplate().getRandomBonusCount()) {
			PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_ITEM_REIDENTIFY_CANNOT_REIDENTIFY(
					targetItem.getItemName()));
			return false;
		}
		if (targetItem.isEquipped()) {
			return false;
		}
		return true;
	}

	/** 执行 / act. */
	@Override
	public void act(final Player player, final Item parentItem, final Item targetItem) {
		// 修复：使用固定的动画 ID（166200022 神话装备鉴定卷轴）让客户端正确播放鉴定动画
		final int animationItemId = 166200022;
		final int parentItemId = parentItem.getItemId();
		final int parntObjectId = parentItem.getObjectId();
		final int nameId = parentItem.getNameId();
		PacketSendUtility.broadcastPacket(player,
				new SM_ITEM_USAGE_ANIMATION(player.getObjectId(), parentItem.getObjectId(), animationItemId, 3000, 0, 0),
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
				PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_ITEM_CANCELED(new DescriptionId(nameId)));
				// 修复：取消时使用 endState = 3（取消）而不是 2（失败）
				PacketSendUtility.broadcastPacket(player,
						new SM_ITEM_USAGE_ANIMATION(player.getObjectId(), parntObjectId, animationItemId, 0, 3, 0), true);
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
						new SM_ITEM_USAGE_ANIMATION(player.getObjectId(), parntObjectId, animationItemId, 0, 1, 1), true);
				if (!player.getInventory().decreaseByObjectId(parntObjectId, 1)) {
					return;
				}
				int rndCount = targetItem.getRandomCount();
				if (rndCount >= targetItem.getItemTemplate().getRandomBonusCount() || targetItem.isEquipped()) {
					return;
				}
				targetItem.setRandomStats(null);
				targetItem.setBonusNumber(0);
				targetItem.setRandomCount(++rndCount);
				targetItem.setOptionalSocket(Rnd.get(0, targetItem.getItemTemplate().getOptionSlotBonus()));
				targetItem.setRndBonus();
				targetItem.setPersistentState(PersistentState.UPDATE_REQUIRED);
				PacketSendUtility.sendPacket(player,
						new SM_TUNE_RESULT(player, targetItem, parentItemId));
				PacketSendUtility.sendPacket(player, new SM_INVENTORY_UPDATE_ITEM(player, targetItem));
				PacketSendUtility.sendPacket(player, new SM_SYSTEM_MESSAGE(1401639, new DescriptionId(nameId)));
			}
		}, 3000);
	}
}
