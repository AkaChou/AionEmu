package com.aionemu.gameserver.model.ai;

import java.util.List;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;

/**
 * Summons，用于 ai 相关逻辑。
 * Summons for ai logic.
 *
 * @author xTz
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Summons")
public class Summons {

	@XmlElement(name = "percentage")
	private List<Percentage> percentage;

	/** 返回 percentage / Returns the percentage */
	public List<Percentage> getPercentage() {
		return this.percentage;
	}
}
