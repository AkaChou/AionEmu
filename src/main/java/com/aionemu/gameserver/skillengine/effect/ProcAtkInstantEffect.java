package com.aionemu.gameserver.skillengine.effect;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.model.DescriptionId;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_ATTACK_STATUS.LOG;
import com.aionemu.gameserver.network.aion.serverpackets.SM_ATTACK_STATUS.TYPE;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.skillengine.action.DamageType;
import com.aionemu.gameserver.skillengine.model.Effect;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * 触发即时攻击效果：按魔法伤害结算，并向施法者发送触发提示。
 * Proc instant attack effect: resolves magical damage and notifies the effector of the proc.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ProcAtkInstantEffect")
public class ProcAtkInstantEffect extends DamageEffect {
	@XmlAttribute(name = "checkprotector")
	private boolean checkProtector = true;
	@XmlAttribute(name = "weaponboost")
	private int weaponBoost;

	public boolean isCheckProtector() {
		return checkProtector;
	}

	public int getWeaponBoost() {
		return weaponBoost;
	}

	/**
	 * 对目标结算 PROCATKINSTANT 伤害，并在需要时通知施法者。
	 * Applies PROCATKINSTANT damage and optionally notifies the effector.
	 *
	 * @param effect 运行时效果 / runtime effect
	 */
	@Override
	public void applyEffect(Effect effect) {
		if (effect.getEffected() != effect.getEffector() && effect.getEffector() instanceof Player) {
			PacketSendUtility.sendPacket((Player) effect.getEffector(),
					new SM_SYSTEM_MESSAGE(1301062, new DescriptionId(effect.getSkillTemplate().getNameId())));
		}
		effect.getEffected().getController().onAttack(effect.getEffector(), effect.getSkillId(), TYPE.DAMAGE,
				effect.getReserved1(), false, LOG.PROCATKINSTANT);
	}

	/**
	 * 以魔法伤害类型计算触发攻击。
	 * Calculates the proc attack as magical damage.
	 *
	 * @param effect 运行时效果 / runtime effect
	 */
	@Override
	public void calculate(Effect effect) {
		super.calculate(effect, DamageType.MAGICAL);
	}
}
