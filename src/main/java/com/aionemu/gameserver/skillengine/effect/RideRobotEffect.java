package com.aionemu.gameserver.skillengine.effect;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.controllers.observer.ActionObserver;
import com.aionemu.gameserver.controllers.observer.ObserverType;
import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.Item;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.templates.item.EquipType;
import com.aionemu.gameserver.model.templates.item.ItemTemplate;
import com.aionemu.gameserver.model.templates.robot.RobotInfo;
import com.aionemu.gameserver.network.aion.serverpackets.SM_USE_ROBOT;
import com.aionemu.gameserver.skillengine.model.Effect;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * 机甲骑乘效果：使玩家进入机甲形态；卸下武器时结束效果。
 * Robot ride effect: puts the player into robot form; ends when a weapon is unequipped.
 *
 * @author Rinzler (Encom)
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "RideRobotEffect")
public class RideRobotEffect extends EffectTemplate {

	/**
	 * 启用机甲、广播外观，并监听武器卸下以结束效果。
	 * Enables robot form, broadcasts appearance, and ends on weapon unequip.
	 *
	 * @param effect 运行时效果 / runtime effect
	 */
	@Override
	public void applyEffect(final Effect effect) {
		effect.addToEffectedController();
		Creature effected = effect.getEffected();
		Player player = (Player) effected;
		player.setUseRobot(true);
		PacketSendUtility.broadcastPacketAndReceive(player,
				new SM_USE_ROBOT(player, getRobotInfo(player).getRobotId()));
		player.setRobotId(getRobotInfo(player).getRobotId());
		ActionObserver observer = new ActionObserver(ObserverType.UNEQUIP) {
			@Override
			public void unequip(Item item, Player owner) {
				if (item.getEquipmentType() == EquipType.WEAPON) {
					effect.endEffect();
				}
			}
		};
		player.getObserveController().addObserver(observer);
		effect.setActionObserver(observer, position);
	}

	/**
	 * 退出机甲形态并移除卸装观察者。
	 * Exits robot form and removes the unequip observer.
	 *
	 * @param effect 运行时效果 / runtime effect
	 */
	@Override
	public void endEffect(Effect effect) {
		super.endEffect(effect);
		Creature effected = effect.getEffected();
		Player player = (Player) effected;
		if (player.isUseRobot()) {
			PacketSendUtility.broadcastPacket(player, new SM_USE_ROBOT(player, 0), true);
			player.setUseRobot(false);
			player.setRobotId(0);
		}
		ActionObserver observer = effect.getActionObserver(position);
		if (observer != null) {
			effect.getEffected().getObserveController().removeObserver(observer);
		}
	}

	/**
	 * 根据主手武器皮肤模板解析机甲信息。
	 * Resolves robot info from the main-hand weapon skin template.
	 *
	 * 玩家 / player
	 * robot info
	 */
	public RobotInfo getRobotInfo(Player player) {
		ItemTemplate template = player.getEquipment().getMainHandWeapon().getItemSkinTemplate();
		return DataManager.ROBOT_DATA.getRobotInfo(template.getRobotId());
	}
}
