/*
 *
 *  Encom is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Lesser Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  Encom is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Lesser Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser Public License
 *  along with Encom.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.aionemu.gameserver.model.templates.item.actions;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.model.DescriptionId;
import com.aionemu.gameserver.model.TaskId;
import com.aionemu.gameserver.model.gameobjects.Item;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.gameobjects.player.motion.Motion;
import com.aionemu.gameserver.network.aion.serverpackets.SM_ITEM_USAGE_ANIMATION;
import com.aionemu.gameserver.network.aion.serverpackets.SM_MOTION;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.ThreadPoolManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*
 * @Rework: MATTY
 */

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "AnimationAddAction")
public class AnimationAddAction extends AbstractItemAction {

	private static final Logger log = LoggerFactory.getLogger(AnimationAddAction.class);

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

	@Override
	public boolean canAct(Player player, Item parentItem, Item targetItem) {
		if (parentItem == null) {
			PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_ITEM_COLOR_ERROR);
			return false;
		}
		return true;
	}

	@Override
	public void act(final Player player, final Item parentItem, Item targetItem) {
		player.getController().cancelUseItem();

		// Проверяем, нужно ли проигрывать анимацию (если у игрока нет ни одной из эмоций)
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

		// Проигрываем анимацию, если нужно
		if (shouldPlayAnimation) {
			PacketSendUtility.sendPacket(player, new SM_ITEM_USAGE_ANIMATION(player.getObjectId(), parentItem.getObjectId(), parentItem.getItemTemplate().getTemplateId(), 1000, 0, 0));
		}

		player.getController().addTask(TaskId.ITEM_USE, ThreadPoolManager.getInstance().schedule(new Runnable() {
			@Override
			public void run() {
				// Флаг, чтобы определить, была ли добавлена хотя бы одна новая анимация
				boolean anyMotionAdded = false;

				// Проверяем, нужно ли добавить анимацию idle и не изучена ли она уже
				if (idle != null) {
					// log.warn("Проверяем, есть ли idle motion с id {}:", idle);
					if (!player.getMotions().hasMotion(idle)) {
						// log.warn("Idle motion с id {} отсутствует, добавляем", idle);
						addMotion(player, idle);
						anyMotionAdded = true;
					} else {
						// log.warn("Idle motion с id {} уже есть, не добавляем", idle);
					}
				}
				// Проверяем, нужно ли добавить анимацию run и не изучена ли она уже
				if (run != null) {
					// log.warn("Проверяем, есть ли run motion с id {}:", run);
					if (!player.getMotions().hasMotion(run)) {
						// log.warn("run motion с id {} отсутствует, добавляем", run);
						addMotion(player, run);
						anyMotionAdded = true;
					} else {
						// log.warn("run motion с id {} уже есть, не добавляем", run);
					}
				}
				// Проверяем, нужно ли добавить анимацию jump и не изучена ли она уже
				if (jump != null) {
					// log.warn("Проверяем, есть ли jump motion с id {}:", jump);
					if (!player.getMotions().hasMotion(jump)) {
						// log.warn("jump motion с id {} отсутствует, добавляем", jump);
						addMotion(player, jump);
						anyMotionAdded = true;
					} else {
						// log.warn("jump motion с id {} уже есть, не добавляем", jump);
					}
				}
				// Проверяем, нужно ли добавить анимацию rest и не изучена ли она уже
				if (rest != null) {
					// log.warn("Проверяем, есть ли rest motion с id {}:", rest);
					if (!player.getMotions().hasMotion(rest)) {
						// log.warn("rest motion с id {} отсутствует, добавляем", rest);
						addMotion(player, rest);
						anyMotionAdded = true;
					} else {
						// log.warn("rest motion с id {} уже есть, не добавляем", rest);
					}
				}
				// Проверяем, нужно ли добавить анимацию shop и не изучена ли она уже
				if (shop != null) {
					// log.warn("Проверяем, есть ли shop motion с id {}:", shop);
					if (!player.getMotions().hasMotion(shop)) {
						// log.warn("shop motion с id {} отсутствует, добавляем", shop);
						addMotion(player, shop);
						anyMotionAdded = true;
					} else {
						// log.warn("shop motion с id {} уже есть, не добавляем", shop);
					}
				}

				// Отправляем пакет SM_ITEM_USAGE_ANIMATION, SM_MOTION и сообщение об изучении только в том случае, если была добавлена хотя бы одна новая анимация и уменьшаем кол-во предметов.
				if (anyMotionAdded) {
					PacketSendUtility.broadcastPacketAndReceive(player, new SM_ITEM_USAGE_ANIMATION(player.getObjectId(), parentItem.getObjectId(), parentItem.getItemId(), 0, 1, 0));
					PacketSendUtility.broadcastPacket(player, new SM_MOTION(player.getObjectId(), player.getMotions().getActiveMotions()), false);
					PacketSendUtility.sendPacket(player, new SM_SYSTEM_MESSAGE(1300423, new DescriptionId(parentItem.getItemTemplate().getNameId())));
					 if (player.getInventory().decreaseItemCount(parentItem, 1) != 0)
						return;
				}
			}
		}, 1000));
	}

	private void addMotion(Player player, int motionId) {
		Motion motion = new Motion(motionId, minutes == null ? 0 : (int) (System.currentTimeMillis() / 1000) + minutes * 60, true);
		player.getMotions().add(motion, true);
		PacketSendUtility.sendPacket(player, new SM_MOTION((short) motion.getId(), motion.getRemainingTime()));
	}
}