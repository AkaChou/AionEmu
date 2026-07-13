package com.aionemu.gameserver.skillengine.effect;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_TARGET_SELECTED;
import com.aionemu.gameserver.network.aion.serverpackets.SM_TARGET_UPDATE;
import com.aionemu.gameserver.skillengine.model.Effect;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * 目标变更效果：对玩家强制切换或清空当前目标（部分技能有概率切向施法者）。
 * Target-change effect: forces a player to retarget or clear target (some skills may retarget the effector).
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "TargetChangeEffect")
public class TargetChangeEffect extends EffectTemplate {
	@XmlAttribute(name = "target_effector")
	protected boolean targetEffector;

	/**
	 * 本效果无即时结算逻辑。
	 * No instant apply logic for this effect.
	 *
	 * @param effect 运行时效果 / runtime effect
	 */
	@Override
	public void applyEffect(Effect effect) {
	}

	/**
	 * 启动时变更玩家目标：特定技能概率切向施法者，否则清空目标。
	 * On start, changes player target: listed skills may retarget effector, otherwise clear target.
	 *
	 * @param effect 运行时效果 / runtime effect
	 */
	@Override
	public void startEffect(Effect effect) {
		Creature effected = effect.getEffected();
		if (effected instanceof Player player) {
			effected.setTarget(targetEffector ? effect.getEffector() : null);
			PacketSendUtility.sendPacket(player, new SM_TARGET_SELECTED(player));
			PacketSendUtility.broadcastPacket(effected, new SM_TARGET_UPDATE(player));
		}
	}
}
