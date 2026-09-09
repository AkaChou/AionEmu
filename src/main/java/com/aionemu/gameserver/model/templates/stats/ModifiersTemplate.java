package com.aionemu.gameserver.model.templates.stats;

import java.util.List;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElements;
import jakarta.xml.bind.annotation.XmlRootElement;

import com.aionemu.gameserver.model.stats.calc.functions.StatFunction;
import lombok.Getter;

/**
 * 修正器模板（静态数据/XML）。
 * XML template.
 *
 * @author xavier
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name = "modifiers")
public class ModifiersTemplate {

	@XmlElements({
			@XmlElement(name = "sub", type = com.aionemu.gameserver.model.stats.calc.functions.StatSubFunction.class),
			@XmlElement(name = "add", type = com.aionemu.gameserver.model.stats.calc.functions.StatAddFunction.class),
			@XmlElement(name = "rate", type = com.aionemu.gameserver.model.stats.calc.functions.StatRateFunction.class),
			@XmlElement(name = "set", type = com.aionemu.gameserver.model.stats.calc.functions.StatSetFunction.class) })
	/** 获取修正器。 / Returns the modifiers. */
	@Getter
	private List<StatFunction> modifiers;

	/** 返回概率 / Returns the chance*/
	@Getter
	@XmlAttribute
	private float chance = 100;

	@XmlAttribute
	private int level;

	/** 获取等级。 / Returns the level. */
	public float getLevel() {
		return level;
	}
}
