package com.aionemu.gameserver.model.templates.item.actions;


import com.aionemu.boot.i18n.I18n;
import lombok.extern.slf4j.Slf4j;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.model.DescriptionId;
import com.aionemu.gameserver.model.TaskId;
import com.aionemu.gameserver.model.gameobjects.Item;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.gameobjects.player.motion.Motion;
import com.aionemu.gameserver.network.aion.serverpackets.SM_ITEM_USAGE_ANIMATION;
import com.aionemu.gameserver.network.aion.serverpackets.SM_MOTION;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * AnimationAdd 动作模板（静态数据/XML）。
 * XML template.
 */

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "AnimationAddAction")
@Slf4j
public class AnimationAddAction extends AbstractItemAction {


	@XmlAttribute
	protected Integer idle;
	@XmlAttribute
	protected Integer run;
	@XmlAttribute
	protected Integer jump;
	@XmlAttribute
	protected Integer rest;
	@XmlAttribute
	protected Integer shop;
	@XmlAttribute
	protected Integer minutes;

	/**
	 * @return 是否允许执行。 / Whether act
	  */
	@Override
	public boolean canAct(Player player, Item parentItem, Item targetItem) {
		if (parentItem == null) {
			PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_ITEM_COLOR_ERROR);
			return false;
		}
		return true;
	}

	/** 执行 / act. */
	@Override
	public void act(final Player player, final Item parentItem, Item targetItem) {
		player.getController().cancelUseItem();

		// 检查是否需要播放动画（若玩家尚未拥有任一动作）。 / Check whether the animation should play (if the player has none of the motions).
		boolean shouldPlayAnimation = false;
		if (idle != null && !player.getMotions().hasMotion(idle)) {
			shouldPlayAnimation = true;
		}
		if (run != null && !player.getMotions().hasMotion(run) && !shouldPlayAnimation) {
			shouldPlayAnimation = true;
		}
		if (jump != null && !player.getMotions().hasMotion(jump) && !shouldPlayAnimation) {
			shouldPlayAnimation = true;
		}
		if (rest != null && !player.getMotions().hasMotion(rest) && !shouldPlayAnimation) {
			shouldPlayAnimation = true;
		}
		if (shop != null && !player.getMotions().hasMotion(shop) && !shouldPlayAnimation) {
			shouldPlayAnimation = true;
		}

		// 如果需要则播放动画。 / Play the animation if needed.
		if (shouldPlayAnimation) {
			PacketSendUtility.sendPacket(player, new SM_ITEM_USAGE_ANIMATION(player.getObjectId(), parentItem.getObjectId(), parentItem.getItemTemplate().getTemplateId(), 1000, 0, 0));
		}

		player.getController().scheduleTask(TaskId.ITEM_USE, new Runnable() {
			/** 运行 / run. */
			@Override
			public void run() {
				// 标记是否新增了至少一种动画。 / Flag whether at least one new motion was added.
				boolean anyMotionAdded = false;

				// 检查是否需添加 idle 动画且尚未学习。 / Check whether the idle motion should be added and is not learned yet.
				if (idle != null) {
					// log.warn(I18n.get("log.79bf49d72aa1", idle));
					if (!player.getMotions().hasMotion(idle)) {
						// log.warn(I18n.get("log.7ce028aeb040", idle));
						addMotion(player, idle);
						anyMotionAdded = true;
					} else {
						// log.warn(I18n.get("log.3b733eb97e8b", idle));
					}
				}
				// 检查是否需添加 run 动画且尚未学习。 / Check whether the run motion should be added and is not learned yet.
				if (run != null) {
					// log.warn(I18n.get("log.168b6ac187b9", run));
					if (!player.getMotions().hasMotion(run)) {
						// log.warn(I18n.get("log.54263cc6614b", run));
						addMotion(player, run);
						anyMotionAdded = true;
					} else {
						// log.warn(I18n.get("log.d00cde9a83fd", run));
					}
				}
				// 检查是否需添加 jump 动画且尚未学习。 / Check whether the jump motion should be added and is not learned yet.
				if (jump != null) {
					// log.warn(I18n.get("log.4dced1c0e67e", jump));
					if (!player.getMotions().hasMotion(jump)) {
						// log.warn(I18n.get("log.a965418c76e3", jump));
						addMotion(player, jump);
						anyMotionAdded = true;
					} else {
						// log.warn(I18n.get("log.e24a81c09788", jump));
					}
				}
				// 检查是否需添加 rest 动画且尚未学习。 / Check whether the rest motion should be added and is not learned yet.
				if (rest != null) {
					// log.warn(I18n.get("log.d56e3b9a8d34", rest));
					if (!player.getMotions().hasMotion(rest)) {
						// log.warn(I18n.get("log.9f35e237297f", rest));
						addMotion(player, rest);
						anyMotionAdded = true;
					} else {
						// log.warn(I18n.get("log.a1bca3318f8c", rest));
					}
				}
				// 检查是否需添加 shop 动画且尚未学习。 / Check whether the shop motion should be added and is not learned yet.
				if (shop != null) {
					// log.warn(I18n.get("log.5debe69c6ae9", shop));
					if (!player.getMotions().hasMotion(shop)) {
						// log.warn(I18n.get("log.19438a928119", shop));
						addMotion(player, shop);
						anyMotionAdded = true;
					} else {
						// log.warn(I18n.get("log.f254adffec13", shop));
					}
				}

				// 仅在新增至少一种动画时才发送动画包与学习消息，并减少物品数量。 / Send the animation packet and learn message, and decrease the item count, only when at least one new motion was added.
				if (anyMotionAdded) {
					PacketSendUtility.broadcastPacketAndReceive(player, new SM_ITEM_USAGE_ANIMATION(player.getObjectId(), parentItem.getObjectId(), parentItem.getItemId(), 0, 1, 0));
					PacketSendUtility.broadcastPacket(player, new SM_MOTION(player.getObjectId(), player.getMotions().getActiveMotions()), false);
					PacketSendUtility.sendPacket(player, new SM_SYSTEM_MESSAGE(1300423, new DescriptionId(parentItem.getItemTemplate().getNameId())));
					 if (player.getInventory().decreaseItemCount(parentItem, 1) != 0)
						return;
				}
			}
		}, 1000);
	}

	private void addMotion(Player player, int motionId) {
		Motion motion = new Motion(motionId, minutes == null ? 0 : (int) (System.currentTimeMillis() / 1000) + minutes * 60, true);
		player.getMotions().add(motion, true);
		PacketSendUtility.sendPacket(player, new SM_MOTION((short) motion.getId(), motion.getRemainingTime()));
	}
}
