package com.aionemu.gameserver.skillengine.effect;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_RESURRECT;
import com.aionemu.gameserver.skillengine.model.Effect;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * 定点复活效果：在施法者位置复活已死亡玩家。
 * Positional resurrect: revives a dead player at the caster's position.
 *
 * @author Sippolo
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ResurrectPositionalEffect")
public class ResurrectPositionalEffect extends ResurrectEffect {

	/**
	 * 在施法者位置执行定点复活。
	 * Performs positional resurrect at the caster location.
	 */
	@Override
	public void applyEffect(Effect effect) {
		Player effector = (Player) effect.getEffector();
		Player effected = (Player) effect.getEffected();

		effected.setPlayerResActivate(true);
		effected.setResurrectionSkill(skillId);
		PacketSendUtility.sendPacket(effected, new SM_RESURRECT(effect.getEffector(), effect.getSkillId()));
		effected.setResPosState(true);
		effected.setResPosX(effector.getX());
		effected.setResPosY(effector.getY());
		effected.setResPosZ(effector.getZ());
	}

	/**
	 * 计算本效果是否成功命中/生效，并写入效果上下文。
	 * Calculates whether this effect succeeds and writes into the effect context.
	 */
	@Override
	public void calculate(Effect effect) {
		if ((effect.getEffector() instanceof Player) && (effect.getEffected() instanceof Player)
				&& (effect.getEffected().getLifeStats().isAlreadyDead())) {
			super.calculate(effect, null, null);
		}
	}
}
