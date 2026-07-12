package com.aionemu.gameserver.model.stats.calc.functions;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.model.stats.calc.Stat2;
import com.aionemu.gameserver.model.stats.calc.StatOwner;
import com.aionemu.gameserver.model.stats.container.StatEnum;
import com.aionemu.gameserver.skillengine.condition.Conditions;

/**
 * 属性函数模型。
 * Stat Function model.
 *
 * @author ATracer
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "SimpleModifier")
public class StatFunction implements IStatFunction {

	@XmlAttribute(name = "name")
	protected StatEnum stat;
	@XmlAttribute
	private boolean bonus;
	@XmlAttribute
	protected int value;
	@XmlAttribute(name = "class_type")
	protected String classType;
	@XmlElement(name = "conditions")
	private Conditions conditions;

	public StatFunction() {
	}

	public StatFunction(StatEnum stat, int value, boolean bonus) {
		this.stat = stat;
		this.value = value;
		this.bonus = bonus;
	}

	/** 比较。 / Compares to another instance. */
	@Override
	public int compareTo(IStatFunction o) {
		int result = getPriority() - o.getPriority();
		if (result == 0) {
			return this.hashCode() - o.hashCode();
		}
		return result;
	}

	/** 获取职业类型。 / Returns the class type. */
	public String getClassType() {
		return classType;
	}

	/** 返回所有者 / Returns the owner*/
	@Override
	public StatOwner getOwner() {
		return null;
	}

	/** 获取名称。 / Returns the name. */
	@Override
	public final StatEnum getName() {
		return stat;
	}

	/** 是否加成。 / Whether Bonus. */
	@Override
	public final boolean isBonus() {
		return bonus;
	}

	/** 返回 priority / Returns the priority */
	@Override
	public int getPriority() {
		return 0x10;
	}

	/** 获取值。 / Returns the value. */
	@Override
	public int getValue() {
		return value;
	}

	/** 校验。 / Validate. */
	@Override
	public boolean validate(Stat2 stat, IStatFunction statFunction) {
		return conditions != null ? conditions.validate(stat, statFunction) : true;
	}

	/** 应用。 / Apply. */
	@Override
	public void apply(Stat2 stat) {
	}

	/** 返回字符串表示。 / Returns string representation. */
	@Override
	public String toString() {
		return this.getClass().getName() + " [stat=" + stat + ", bonus=" + bonus + ", value=" + value + ", priority="
				+ getPriority() + "]";
	}

	/** 带条件 / with Conditions. */
	public StatFunction withConditions(Conditions conditions) {
		this.conditions = conditions;
		return this;
	}

	/**
	 * @return Whether conditions
	 */
	public boolean hasConditions() {
		return conditions != null;
	}

	/**
	 * 创建 final 列表的 modifierscombiningbonuses 带 randombonuses。 / Creates a final list of modifiers combining bonuses with random bonuses
	 *
	 * @param modifiers  - can be null if do not exist
	 * @param rndBonuses - can be null if do not exist
	 * @return a list of modifiers, empty if none
	 */
	public static List<StatFunction> mergeRandomBonuses(List<StatFunction> modifiers, List<StatFunction> rndBonuses) {
		if (modifiers == null) {
			modifiers = new ArrayList<StatFunction>();
		}
		if (rndBonuses == null) {
			return modifiers;
		}
		List<StatFunction> allModifiers = new ArrayList<StatFunction>();
		EnumSet<StatEnum> rndNames = EnumSet.noneOf(StatEnum.class);

		for (IStatFunction func : rndBonuses) {
			rndNames.add(func.getName());
		}
		// 将值加入原始属性 / add values to original stats
		for (StatFunction modifier : modifiers) {
			if (!rndNames.contains(modifier.getName()) || !modifier.isBonus() || modifier.hasConditions()) {
				allModifiers.add(modifier);
				continue;
			}

			IStatFunction rndBonus = null;
			for (IStatFunction func : rndBonuses) {
				if (func.getName() == modifier.getName()) {
					rndBonus = func;
					rndNames.remove(func.getName());
					break;
				}
			}

			int finalValue = modifier.getValue() + rndBonus.getValue();

			if (modifier instanceof StatAddFunction) {
				if (finalValue != 0) {
					allModifiers.add(new StatAddFunction(modifier.getName(), finalValue, true));
				}
			} else if (modifier instanceof StatRateFunction) {
				if (finalValue != 0) {
					allModifiers.add(new StatRateFunction(modifier.getName(), finalValue, true));
				}
			} else {
				allModifiers.add(modifier);
			}
		}

		// 添加新属性值。 / add new stat values
		for (StatFunction modifier : rndBonuses) {
			if (rndNames.contains(modifier.getName())) {
				allModifiers.add(modifier);
			}
		}
		return allModifiers;
	}
}
