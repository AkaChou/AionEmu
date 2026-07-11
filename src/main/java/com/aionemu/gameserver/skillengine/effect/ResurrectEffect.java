package com.aionemu.gameserver.skillengine.effect;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_RESURRECT;
import com.aionemu.gameserver.skillengine.model.Effect;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * 复活效果：向已死亡玩家发送复活请求并设置复活技能。
 * Resurrect effect: sends a resurrect request to a dead player and sets the skill.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ResurrectEffect")
public class ResurrectEffect extends EffectTemplate {
	@XmlAttribute(name = "skill_id")
	protected int skillId;

	/**
	 * 向死亡玩家发送复活包并设置复活技能。
	 * Sends resurrect packet and sets the resurrection skill.
	 */
	@Override
	public void applyEffect(Effect effect) {
		Player effectedPlayer = (Player) effect.getEffected();
		effectedPlayer.setPlayerResActivate(true);
		effectedPlayer.setResurrectionSkill(skillId);
		PacketSendUtility.sendPacket(effectedPlayer, new SM_RESURRECT(effect.getEffector(), effect.getSkillId()));
	}

	/**
	 * 仅对已死亡玩家计算复活。
	 * Calculates resurrect only for already-dead players.
	 */
	@Override
	public void calculate(Effect effect) {
		if (effect.getEffected() instanceof Player && effect.getEffected().getLifeStats().isAlreadyDead()) {
			super.calculate(effect, null, null);
		}
	}
}
