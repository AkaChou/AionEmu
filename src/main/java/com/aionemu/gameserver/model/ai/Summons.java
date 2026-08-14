package com.aionemu.gameserver.model.ai;

import java.util.List;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;

/**
 * 召唤物配置集合：按百分比阈值组织召唤物队伍。
 * Summon configuration set: organizes summon groups by percentage thresholds.
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
