package com.aionemu.gameserver.skillengine.model;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;

/**
 * 充能阶段模板：关联技能 ID 与充能时间。
 * Charge stage template: linked skill id and charge time.
 *
 * @author Dr.Nism
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "charge")
public class ChargeTemplate {

	@XmlAttribute(name = "skill_id")
	private int skill_id;

	@XmlAttribute(name = "time")
	private int time;

	/**
	 * 获取充能阶段技能 ID。
	 * Gets charge-stage skill id.
	 *
	 */
	public int getSkillId() {
		return skill_id;
	}

	/**
	 * 获取充能时间。
	 * Gets charge time.
	 *
	 */
	public int getTime() {
		return time;
	}
}
