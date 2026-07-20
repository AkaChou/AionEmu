package com.aionemu.gameserver.skillengine.effect;


import com.aionemu.boot.i18n.I18n;
import lombok.extern.slf4j.Slf4j;
import java.util.ArrayList;
import java.util.List;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.stats.calc.functions.IStatFunction;
import com.aionemu.gameserver.model.stats.calc.functions.StatAddFunction;
import com.aionemu.gameserver.model.stats.calc.functions.StatRateFunction;
import com.aionemu.gameserver.model.stats.calc.functions.StatSetFunction;
import com.aionemu.gameserver.model.stats.container.CreatureGameStats;
import com.aionemu.gameserver.model.stats.container.StatEnum;
import com.aionemu.gameserver.network.aion.serverpackets.SM_ATTACK_STATUS.TYPE;
import com.aionemu.gameserver.skillengine.change.Change;
import com.aionemu.gameserver.skillengine.condition.Conditions;
import com.aionemu.gameserver.skillengine.model.Effect;

/**
 * Buff 效果基类：将 change 列表转为属性修正并挂到受影响者。
 * Buff effect base: converts the change list into stat modifiers on the effected.
 *
 * @author ATracer
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "BuffEffect")
@Slf4j
public abstract class BuffEffect extends EffectTemplate {

	@XmlAttribute
	protected boolean maxstat;


	/**
	 * 将效果加入受影响者的效果控制器。
	 * Adds the effect to the effected creature's effect controller.
	 *
	 * @param effect 运行时效果 / runtime effect
	 */
	@Override
	public void applyEffect(Effect effect) {
		effect.addToEffectedController();
	}

	/**
	 * 效果结束时移除属性修正。
	 * Removes stat modifiers when the effect ends.
	 *
	 * @param effect 运行时效果 / runtime effect
	 */
	@Override
	public void endEffect(Effect effect) {
		Creature effected = effect.getEffected();
		effected.getGameStats().endEffect(effect);
	}

	/**
	 * 效果开始时应用属性修正；maxstat 时回满 HP/MP。
	 * Applies stat modifiers on start; fills HP/MP when maxstat is set.
	 *
	 * @param effect 运行时效果 / runtime effect
	 */
	@Override
	public void startEffect(Effect effect) {
		if (change == null) {
			return;
		}
		Creature effected = effect.getEffected();
		CreatureGameStats<? extends Creature> cgs = effected.getGameStats();

		List<IStatFunction> modifiers = getModifiers(effect);

		if (modifiers.size() > 0) {
			cgs.addEffect(effect, modifiers);
		}

		if (maxstat) {
			effected.getLifeStats().increaseHp(TYPE.HP, effected.getGameStats().getMaxHp().getCurrent());
			effected.getLifeStats().increaseMp(TYPE.HEAL_MP, effected.getGameStats().getMaxMp().getCurrent());
		}
	}

	/**
	 * 根据 change 配置构建属性修正函数列表。
	 * Builds the list of stat modifier functions from the change config.
	 *
	 * @param effect 运行时效果 / runtime effect
	 * @return 属性修正列表 / list of stat modifiers
	 */
	protected List<IStatFunction> getModifiers(Effect effect) {
		int skillId = effect.getSkillId();
		int skillLvl = effect.getSkillLevel();

		List<IStatFunction> modifiers = new ArrayList<IStatFunction>();

		for (Change changeItem : change) {
			if (changeItem.getStat() == null) {
				log.warn(I18n.get("log.82bc249ba5a2", skillId));
				continue;
			}

			int valueWithDelta = changeItem.getValue() + changeItem.getDelta() * skillLvl;

			Conditions conditions = changeItem.getConditions();
			switch (changeItem.getFunc()) {
			case ADD:
				modifiers.add(
						new StatAddFunction(changeItem.getStat(), valueWithDelta, true).withConditions(conditions));
				break;
			case PERCENT:
				if (changeItem.getStat() == StatEnum.ABNORMAL_RESISTANCE_ALL) {
					for (StatEnum stat : AR_ALL_RESISTANCE_STATS) {
						modifiers.add(new StatRateFunction(stat, valueWithDelta, true).withConditions(conditions));
					}
				} else if (changeItem.getStat() == StatEnum.STUNLIKE_RESISTANCE) {
					for (StatEnum stat : STUNLIKE_RESISTANCE_STATS) {
						modifiers.add(new StatRateFunction(stat, valueWithDelta, true).withConditions(conditions));
					}
				} else {
					modifiers.add(
							new StatRateFunction(changeItem.getStat(), valueWithDelta, true).withConditions(conditions));
				}
				break;
			case REPLACE:
				modifiers.add(
						new StatSetFunction(changeItem.getStat(), valueWithDelta, true).withConditions(conditions));
				break;
			}
		}
		return modifiers;
	}

	/**
	 * 周期动作占位（Buff 默认无周期逻辑）。
	 * Periodic action stub (buffs have no default tick logic).
	 *
	 * @param effect 运行时效果 / runtime effect
	 */
	@Override
	public void onPeriodicAction(Effect effect) {
	}
}
