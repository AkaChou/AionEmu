package com.aionemu.gameserver.skillengine.effect;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.stats.container.StatEnum;
import com.aionemu.gameserver.network.aion.serverpackets.SM_ATTACK_STATUS.LOG;
import com.aionemu.gameserver.network.aion.serverpackets.SM_ATTACK_STATUS.TYPE;
import com.aionemu.gameserver.skillengine.model.Effect;
import com.aionemu.gameserver.skillengine.model.HealType;

/**
 * 治疗类效果基类：按 HP/MP/FP/DP 计算并应用治疗量，支持固定值与百分比。
 * Base class for heal effects: calculates and applies heal for HP/MP/FP/DP, fixed or percent.
 *
 * @author ATracer modified by Wakizashi, kecimis
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "AbstractHealEffect")
public abstract class AbstractHealEffect extends EffectTemplate {

	@XmlAttribute
	protected boolean percent;

	/**
	 * 计算最终治疗量（含治疗加成/削弱、疾病状态拦截），并写入效果预留值。
	 * Calculates final heal (boost/deboost, disease block) and stores it in reserved values.
	 *
	 * @param effect 运行时效果 / runtime effect
	 * heal type
	 */
	public void calculate(Effect effect, HealType healType) {
		if (!super.calculate(effect, null, null)) {
			return;
		}
		Creature effector = effect.getEffector();

		int valueWithDelta = value + delta * effect.getSkillLevel();
		int currentValue = getCurrentStatValue(effect);
		int maxCurValue = getMaxStatValue(effect);
		int possibleHealValue = 0;
		if (percent) {
			possibleHealValue = maxCurValue * valueWithDelta / 100;
		}
		else {
			possibleHealValue = valueWithDelta;
		}

		int finalHeal = possibleHealValue;

		if (healType == HealType.HP) {
			int baseHeal = possibleHealValue;
			if (effect.getItemTemplate() == null) {
				int boostHealAdd = effector.getGameStats().getStat(StatEnum.HEAL_BOOST, 0).getCurrent();
				// 应用百分比治疗增强加成（如特性技能） / Apply percent Heal Boost bonus (ex. Passive skills)
				int boostHeal = (effector.getGameStats().getStat(StatEnum.HEAL_BOOST, baseHeal).getCurrent() - boostHealAdd);
				// 应用治疗增强加成（如仁慈类技能） / Apply Add Heal Boost bonus (ex. Skills like Benevolence)
				boostHeal += boostHeal * boostHealAdd / 1000;
				finalHeal = effector.getGameStats().getStat(StatEnum.HEAL_SKILL_BOOST, boostHeal).getCurrent();
			}
			finalHeal = effector.getGameStats().getStat(StatEnum.HEAL_SKILL_DEBOOST, finalHeal).getCurrent();
		}

		if (finalHeal < 0) {
			finalHeal = currentValue > -finalHeal ? finalHeal : -currentValue;
		}
		else {
			finalHeal = maxCurValue - currentValue < finalHeal ? (maxCurValue - currentValue) : finalHeal;
		}

		if (healType == HealType.HP && effect.getEffected().getEffectController().isAbnormalSet(AbnormalState.DISEASE)) {
			finalHeal = 0;
		}

		effect.setReservedInt(position, finalHeal);
		effect.setReserved1(-finalHeal);
	}

	/**
	 * 将已计算的治疗量按类型应用到受影响者。
	 * Applies the calculated heal value to the effected creature by heal type.
	 *
	 * @param effect 运行时效果 / runtime effect
	 * heal type
	 */
	public void applyEffect(Effect effect, HealType healType) {
		Creature effected = effect.getEffected();
		int healValue = effect.getReservedInt(position);

		if (healValue == 0) {
			return;
		}

		switch (healType) {
			case HP:
				if (this instanceof ProcHealInstantEffect)// item heal, eg potions
				{
					effected.getLifeStats().increaseHp(TYPE.HP, healValue, 0, LOG.REGULAR);
				}
				else
				if (healValue > 0) {
					effected.getLifeStats().increaseHp(TYPE.REGULAR, healValue, 0, LOG.REGULAR);
				}
				else {
					effected.getLifeStats().reduceHp(-healValue, effected);
				}
				break;
			case MP:
				if (this instanceof ProcMPHealInstantEffect)// item heal, eg potions
				{
					effected.getLifeStats().increaseMp(TYPE.MP, healValue, 0, LOG.REGULAR);
				}
				else {
					effected.getLifeStats().increaseMp(TYPE.HEAL_MP, healValue, 0, LOG.REGULAR);
				}
				break;
			case FP:
				effected.getLifeStats().increaseFp(TYPE.FP, healValue);
				break;
			case DP:
				((Player) effected).getCommonData().addDp(healValue);
				break;
		}
	}

	/**
	 * 返回受影响者当前对应属性值。
	 * Returns the effected creature's current stat for this heal type.
	 *
	 * @param effect 运行时效果 / runtime effect
	 * @return 当前属性值 / current stat value
	 */
	protected abstract int getCurrentStatValue(Effect effect);

	/**
	 * 返回受影响者对应属性上限。
	 * Returns the effected creature's max stat for this heal type.
	 *
	 * @param effect 运行时效果 / runtime effect
	 * max stat value
	 */
	protected abstract int getMaxStatValue(Effect effect);
}
