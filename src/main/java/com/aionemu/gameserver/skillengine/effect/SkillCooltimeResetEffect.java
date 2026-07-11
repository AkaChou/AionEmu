package com.aionemu.gameserver.skillengine.effect;

import java.util.HashMap;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SKILL_COOLDOWN;
import com.aionemu.gameserver.skillengine.model.Effect;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * 技能冷却重置/缩减效果：缩短指定技能 ID 区间内的冷却时间。
 * Skill cooltime reset effect: reduces cooldown for skills in a configured id range.
 *
 * @author Dr.Nism
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "SkillCooltimeResetEffect")
public class SkillCooltimeResetEffect extends EffectTemplate {

	@XmlAttribute(name = "first_cd", required = true)
	protected int firstCd;
	@XmlAttribute(name = "second_cd", required = true)
	protected int secondCd;

	/**
	 * 遍历 firstCd–secondCd，按 value/delta 缩减剩余冷却并同步客户端。
	 * Walks firstCd–secondCd, reduces remaining cooldown by value/delta, and syncs the client.
	 */
	@Override
	public void applyEffect(Effect effect) {
		Creature effected = effect.getEffected();
		HashMap<Integer, Long> resetSkillCoolDowns = new HashMap<>();
		for (int i = firstCd; i <= secondCd; i++) {
			long delay = effected.getSkillCoolDown(i) - System.currentTimeMillis();
			if (delay <= 0) {
				continue;
			}
			if (delta > 0) {
				delay -= delay * (delta / 100);
			} else {
				delay -= value;
			}
			effected.setSkillCoolDown(i, delay + System.currentTimeMillis());
			resetSkillCoolDowns.put(i, delay + System.currentTimeMillis());
		}
		if (effected instanceof Player) {
			if (resetSkillCoolDowns.size() > 0) {
				Player player = (Player) effected;
				PacketSendUtility.sendPacket(player, new SM_SKILL_COOLDOWN(player, resetSkillCoolDowns, false));
			}
		}
	}
}
