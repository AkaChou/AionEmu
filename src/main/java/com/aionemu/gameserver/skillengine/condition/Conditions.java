package com.aionemu.gameserver.skillengine.condition;

import java.util.ArrayList;
import java.util.List;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElements;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.model.stats.calc.Stat2;
import com.aionemu.gameserver.model.stats.calc.functions.IStatFunction;
import com.aionemu.gameserver.skillengine.model.Effect;
import com.aionemu.gameserver.skillengine.model.Skill;

/**
 * 条件容器：聚合技能模板中声明的全部条件，并在技能/属性/效果路径上逐条校验。
 * Conditions container: aggregates all conditions declared in a skill template and validates them on skill/stat/effect paths.
 *
 * @author ATracer
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Conditions", propOrder = { "conditions" })
public class Conditions {

	@XmlElements({ @XmlElement(name = "abnormal", type = AbnormalStateCondition.class),
			@XmlElement(name = "target", type = TargetCondition.class),
			@XmlElement(name = "mp", type = MpCondition.class), @XmlElement(name = "hp", type = HpCondition.class),
			@XmlElement(name = "dp", type = DpCondition.class),
			@XmlElement(name = "playermove", type = PlayerMovedCondition.class),
			@XmlElement(name = "onfly", type = OnFlyCondition.class),
			@XmlElement(name = "weapon", type = WeaponCondition.class),
			@XmlElement(name = "noflying", type = NoFlyingCondition.class),
			@XmlElement(name = "shield", type = ShieldCondition.class),
			@XmlElement(name = "armor", type = ArmorCondition.class),
			@XmlElement(name = "charge", type = ChargeCondition.class),
			@XmlElement(name = "targetflying", type = TargetFlyingCondition.class),
			@XmlElement(name = "selfflying", type = SelfFlyingCondition.class),
			@XmlElement(name = "combatcheck", type = CombatCheckCondition.class),
			@XmlElement(name = "front", type = FrontCondition.class),
			@XmlElement(name = "chain", type = ChainCondition.class),
			@XmlElement(name = "back", type = BackCondition.class),
			@XmlElement(name = "form", type = FormCondition.class),
			@XmlElement(name = "idianchargeweapon", type = IdianChargeCondition.class) })
	protected List<Condition> conditions;

	/**
	 * 获取条件列表（实时引用，修改会反映到 JAXB 对象中）。
	 * Gets the conditions list (live reference; modifications are present inside the JAXB object).
	 *
	 * conditions list
	 */
	public List<Condition> getConditions() {
		if (conditions == null) {
			conditions = new ArrayList<Condition>();
		}
		return this.conditions;
	}

	/**
	 * 在技能施放路径上逐条校验全部条件。
	 * Validates all conditions on the skill cast path.
	 *
	 * @param skill 技能环境 / skill environment
	 * @return 全部通过则为 true / true if all pass
	 */
	public boolean validate(Skill skill) {
		if (conditions != null) {
			for (Condition condition : getConditions()) {
				if (!condition.validate(skill)) {
					return false;
				}
			}
		}
		return true;
	}

	/**
	 * 在属性计算路径上逐条校验全部条件。
	 * Validates all conditions on the stat calculation path.
	 *
	 * @param stat 属性对象 / stat object
	 * stat function
	 *
	 * @return 全部通过则为 true / true if all pass
	 */
	public boolean validate(Stat2 stat, IStatFunction statFunction) {
		if (conditions != null) {
			for (Condition condition : getConditions()) {
				if (!condition.validate(stat, statFunction)) {
					return false;
				}
			}
		}
		return true;
	}

	/**
	 * 在效果应用路径上逐条校验全部条件。
	 * Validates all conditions on the effect application path.
	 *
	 * effect environment
	 *
	 * @param effect
	 * @return 全部通过则为 true / true if all pass
	 */
	public boolean validate(Effect effect) {
		if (conditions != null) {
			for (Condition condition : getConditions()) {
				if (!condition.validate(effect)) {
					return false;
				}
			}
		}
		return true;
	}
}
