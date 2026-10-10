package com.aionemu.gameserver.model.templates.item.actions;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.commons.utils.Rnd;
import com.aionemu.gameserver.controllers.observer.ItemUseObserver;
import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.model.DescriptionId;
import com.aionemu.gameserver.model.TaskId;
import com.aionemu.gameserver.model.gameobjects.Item;
import com.aionemu.gameserver.model.gameobjects.PersistentState;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.items.RandomBonusResult;
import com.aionemu.gameserver.model.templates.item.bonuses.StatBonusType;
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
		// 动画与结算延时 5000ms（对齐真端 User_AddIdentifyItemTimer 的 5000ms 计时）。
		// Animation and settlement delay is 5000 ms (mirrors the 5000 ms timer in retail User_AddIdentifyItemTimer).
		PacketSendUtility.broadcastPacket(player,
				new SM_ITEM_USAGE_ANIMATION(player.getObjectId(), parentItem.getObjectId(), animationItemId, 5000, 0, 0),
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
				// 先校验再消耗，避免状态在动画期间变化时白白损失卷轴（对齐真端 User_IdentifyItem 的校验先于扣减）。
				// Validate before consuming so a mid-animation state change cannot waste the scroll
				// (mirrors retail User_IdentifyItem validating ahead of the decrement).
				int rndCount = targetItem.getRandomCount();
				if (rndCount >= targetItem.getItemTemplate().getRandomBonusCount() || targetItem.isEquipped()) {
					return;
				}
				if (!player.getInventory().decreaseByObjectId(parntObjectId, 1)) {
					return;
				}
				// 鉴定计数立即递增且取消不回滚（对齐真端 OnIdentifyItem 的 0x494 递增）。
				// The tune count increments immediately and is never rolled back on cancel
				// (mirrors the 0x494 increment in retail OnIdentifyItem).
				targetItem.setRandomCount(rndCount + 1);
				// 计数递增后立即标记持久化（对齐真端 OnIdentifyItem 递增 0x494 后即刻 UpdateDBRandomOption 落库）。
				// Flag persistence right after the increment (mirrors retail OnIdentifyItem persisting
				// the 0x494 counter via UpdateDBRandomOption immediately after incrementing).
				targetItem.setPersistentState(PersistentState.UPDATE_REQUIRED);
				// 重新鉴定完成提示（对齐真端 0x156327，参数为目标装备名而非卷轴名）。
				// Reidentify-succeed notice (mirrors retail 0x156327, parameterized by the target item's name).
				PacketSendUtility.sendPacket(player,
						new SM_SYSTEM_MESSAGE(1401639, new DescriptionId(targetItem.getNameId())));
				// 属性值暂存，等待客户端在预览对话框中确认（对齐真端 0x539/0xa7/0x53a 暂存）。
				// Attribute values are staged until the client confirms in the preview dialog
				// (mirrors the 0x539/0xa7/0x53a staging in retail).
				int newBonusNumber = 0;
				RandomBonusResult bonus = DataManager.ITEM_RANDOM_BONUSES.getRandomModifiers(StatBonusType.INVENTORY,
						targetItem.getItemTemplate().getRandomBonusId());
				if (bonus != null) {
					newBonusNumber = bonus.templateNumber();
				}
				targetItem.setPendingReidentify(Rnd.get(0, targetItem.getItemTemplate().getOptionSlotBonus()),
						Rnd.get(0, targetItem.getItemTemplate().getMaxEnchantBonus()), newBonusNumber);
				// 预览包（对齐真端 S_REIDENTIFY_PREVIEW / 0x122），携带暂存值供前后对比。
				// Preview packet (mirrors retail S_REIDENTIFY_PREVIEW / 0x122), carrying the staged values.
				PacketSendUtility.sendPacket(player,
						new SM_TUNE_RESULT(player, targetItem, parentItemId));
				// 真端预览包自带计数字段；本包精简，需补一次物品刷新让客户端更新鉴定次数显示。
				// Retail's preview packet carries the count itself; this slimmer packet needs an extra
				// inventory refresh so the client updates the displayed reidentify count.
				PacketSendUtility.sendPacket(player, new SM_INVENTORY_UPDATE_ITEM(player, targetItem));
			}
		}, 5000);
	}
}
