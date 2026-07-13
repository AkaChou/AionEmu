package com.aionemu.gameserver.skillengine.effect;

import java.util.HashMap;
import java.util.Map;

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
	 * 技能冷却重置/缩减效果：缩短指定 delay ID 区间内的冷却时间。
	 * Skill cooltime reset effect: reduces cooldown for a configured delay-id range.
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
	@XmlAttribute
	protected Boolean percent;

	/**
	 * 遍历 firstCd–secondCd，按 value/delta 缩减剩余冷却并同步客户端。
	 * Walks firstCd–secondCd, reduces remaining cooldown by value/delta, and syncs the client.
	 */
	@Override
	public void applyEffect(Effect effect) {
		Creature effected = effect.getEffected();
		HashMap<Integer, Long> resetSkillCoolDowns = new HashMap<>();
		Map<Integer, Long> skillCoolDowns = effected.getSkillCoolDowns();
		if (skillCoolDowns == null) {
			return;
		}
		long now = System.currentTimeMillis();
		int resetAmount = getResetAmount();
		boolean percentReset = isPercentReset();
		for (Map.Entry<Integer, Long> entry : skillCoolDowns.entrySet()) {
			int delayId = entry.getKey();
			if (delayId < firstCd || delayId > secondCd) {
				continue;
			}
			long remaining = entry.getValue() - now;
			if (remaining <= 0) {
				continue;
			}
			long baseTime = effected.getSkillCoolDownBase(delayId);
			long original = baseTime > 0 ? entry.getValue() - baseTime : remaining;
			long reuseTime = now + calculateRemaining(remaining, original, resetAmount, percentReset);
			effected.setSkillCoolDown(delayId, reuseTime);
			resetSkillCoolDowns.put(delayId, reuseTime);
		}
		if (effected instanceof Player player && !resetSkillCoolDowns.isEmpty()) {
			PacketSendUtility.sendPacket(player, new SM_SKILL_COOLDOWN(player, resetSkillCoolDowns, false));
		}
	}

	int getResetAmount() {
		return percent == null && delta > 0 ? delta : value;
	}

	boolean isPercentReset() {
		return percent != null ? percent : delta > 0;
	}

	static long calculateRemaining(long remaining, long original, int amount, boolean percent) {
		long reductionAmount = percent ? original * Math.min(100, Math.max(0, amount)) / 100 : Math.max(0, amount);
		return Math.max(0, remaining - reductionAmount);
	}
}
