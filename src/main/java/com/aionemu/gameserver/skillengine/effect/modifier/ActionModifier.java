package com.aionemu.gameserver.skillengine.effect.modifier;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.skillengine.change.Func;
import com.aionemu.gameserver.skillengine.model.Effect;

/**
 * 行动修正器基类：按条件对伤害/数值进行加减或倍率修正。
 * Base action modifier: conditionally adjusts damage/values by add or multiply.
 *
 * @author ATracer
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ActionModifier")
public abstract class ActionModifier {

	@XmlAttribute
	protected int delta;
	@XmlAttribute(required = true)
	protected int value;
	@XmlAttribute
	protected Func mode = Func.ADD;

	/**
	 * 计算修正值。
	 * Computes the modifier value.
	 *
	 * @param effect 运行中效果 / Runtime effect
	 * @return 修正量 / Modifier amount
	 */
	public abstract int analyze(Effect effect);

	/**
	 * 检查修正条件是否成立。
	 * Checks whether the modifier condition holds.
	 *
	 * @param effect 运行中效果 / Runtime effect
	 * @return 条件成立则为 true / True if applicable
	 */
	public abstract boolean check(Effect effect);

	/**
	 * 获取运算模式（加/乘等）。
	 * Returns the arithmetic mode (add/multiply, etc.).
	 *
	 * @return 运算模式 / Function mode
	 */
	public Func getFunc() {
		return mode;
	}
}
