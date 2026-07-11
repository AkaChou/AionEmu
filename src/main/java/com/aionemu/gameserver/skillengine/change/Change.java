package com.aionemu.gameserver.skillengine.change;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.model.stats.container.StatEnum;
import com.aionemu.gameserver.skillengine.condition.Conditions;

/**
 * 属性变更模板：描述对某项属性的加减/百分比/替换修改及可选条件。
 * Stat change template: add/percent/replace modification of a stat with optional conditions.
 *
 * @author ATracer
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Change")
public class Change {

	/**
	 * 目标属性类型。
	 * Target stat type.
	 */
	@XmlAttribute(required = true)
	private StatEnum stat;

	/**
	 * 变更函数（加值/百分比/替换）。
	 * Change function (add/percent/replace).
	 */
	@XmlAttribute(required = true)
	private Func func;

	/**
	 * 基础变更值。
	 * Base change value.
	 */
	@XmlAttribute(required = true)
	private int value;

	/**
	 * 等级相关增量。
	 * Level-based delta.
	 */
	@XmlAttribute
	private int delta;

	/**
	 * 应用该变更的前置条件。
	 * Conditions required to apply this change.
	 */
	@XmlElement(name = "conditions")
	private Conditions conditions;

	/**
	 * 获取目标属性。
	 * Returns the target stat.
	 *
	 * stat enum
	 */
	public final StatEnum getStat() {
		return stat;
	}

	/**
	 * 获取变更函数。
	 * Returns the change function.
	 *
	 * change function
	 */
	public final Func getFunc() {
		return func;
	}

	/**
	 * 获取基础变更值。
	 * Returns the base change value.
	 *
	 * base value
	 */
	public final int getValue() {
		return value;
	}

	/**
	 * 获取等级增量。
	 * Returns the level delta.
	 *
	 * delta
	 */
	public final int getDelta() {
		return delta;
	}

	/**
	 * 获取应用条件。
	 * Returns the apply conditions.
	 *
	 * conditions
	 */
	public final Conditions getConditions() {
		return conditions;
	}
}
