package com.aionemu.gameserver.skillengine.model;

import java.util.List;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;

/**
 * 充能技能模板：一至三阶段技能、最小充能与加成类型。
 * Charge skill template: stage skills, min charge and bonus type.
 *
 * @author Dr.Nism
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "charge_skill")
public class ChargeSkillTemplate {

	@XmlAttribute(name = "id")
	private int id;

	@XmlAttribute(name = "charge_set_name")
	private String charge_set_name;

	@XmlAttribute(name = "first")
	private int first;

	@XmlAttribute(name = "second")
	private int second;

	@XmlAttribute(name = "third")
	private int third;

	@XmlAttribute(name = "min_charge")
	private int min_charge;

	@XmlElement(name = "charge")
	private List<ChargeTemplate> charges;

	@XmlAttribute(name = "bonus_type", required = true)
	protected BonusChargeType type = BonusChargeType.NONE;

	/**
	 * 获取充能技能 ID。
	 * Gets charge skill id.
	 *
	 */
	public int getId() {
		return id;
	}

	/**
	 * 获取充能集合名称。
	 * Gets charge set name.
	 *
	 */
	public String getChargeSetName() {
		return charge_set_name;
	}

	/**
	 * 获取第一阶段技能 ID。
	 * Gets first-stage skill id.
	 *
	 */
	public int getFirstId() {
		return first;
	}

	/**
	 * 获取第二阶段技能 ID。
	 * Gets second-stage skill id.
	 *
	 */
	public int getSecondId() {
		return second;
	}

	/**
	 * 获取第三阶段技能 ID。
	 * Gets third-stage skill id.
	 *
	 */
	public int getThirdId() {
		return third;
	}

	/**
	 * 获取最小充能值。
	 * Gets minimum charge value.
	 *
	 */
	public int getMinCharge() {
		return min_charge;
	}

	/**
	 * 获取充能阶段列表。
	 * Gets charge stage list.
	 *
	 */
	public List<ChargeTemplate> getCharges() {
		return charges;
	}

	/**
	 * 获取充能加成类型。
	 * Gets bonus charge type.
	 *
	 */
	public BonusChargeType getBonusChargeType() {
		return type;
	}
}
