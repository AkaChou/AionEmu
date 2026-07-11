package com.aionemu.gameserver.skillengine.model;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.model.Gender;
import com.aionemu.gameserver.model.Race;

/**
 * 动作时间表：按种族/性别分组的武器动作时间（am/af/em/ef）。
 * Motion time table: per race/gender weapon motion times (am/af/em/ef).
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "motion_time", propOrder = { "am", "af", "em", "ef" })
public class MotionTime {

	protected Times am;
	protected Times af;
	protected Times em;
	protected Times ef;

	@XmlAttribute(required = true)
	protected String name;

	/**
	 * 获取动作名称。
	 * Gets motion name.
	 *
	 * name
	 */
	public String getName() {
		return name;
	}

	/**
	 * 获取阿斯摩男性时间表。
	 * Gets Asmodian male times.
	 *
	 * times
	 */
	public Times getAm() {
		return am;
	}

	/**
	 * 设置阿斯摩男性时间表。
	 * Sets Asmodian male times.
	 *
	 * @param am 时间表 / times
	 */
	public void setAm(Times am) {
		this.am = am;
	}

	/**
	 * 获取阿斯摩女性时间表。
	 * Gets Asmodian female times.
	 *
	 * times
	 */
	public Times getAf() {
		return af;
	}

	/**
	 * 设置阿斯摩女性时间表。
	 * Sets Asmodian female times.
	 *
	 * @param af 时间表 / times
	 */
	public void setAf(Times af) {
		this.af = af;
	}

	/**
	 * 获取天族男性时间表。
	 * Gets Elyos male times.
	 *
	 * times
	 */
	public Times getEm() {
		return em;
	}

	/**
	 * 设置天族男性时间表。
	 * Sets Elyos male times.
	 *
	 * @param em 时间表 / times
	 */
	public void setEm(Times em) {
		this.em = em;
	}

	/**
	 * 获取天族女性时间表。
	 * Gets Elyos female times.
	 *
	 * times
	 */
	public Times getEf() {
		return ef;
	}

	/**
	 * 设置天族女性时间表。
	 * Sets Elyos female times.
	 *
	 * @param ef 时间表 / times
	 */
	public void setEf(Times ef) {
		this.ef = ef;
	}

	/**
	 * 设置动作名称。
	 * Sets motion name.
	 *
	 * name
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * 按种族与性别获取时间表。
	 * Gets times by race and gender.
	 *
	 * 阵营 / race
	 * gender
	 * @return 时间表，未知则 null / times or null
	 */
	public Times getTimes(Race race, Gender gender) {

		switch (race) {
		case ASMODIANS:
			if (gender == Gender.MALE) {
				return this.getAm();
			} else {
				return this.getAf();
			}
		case ELYOS:
			if (gender == Gender.MALE) {
				return this.getEm();
			} else {
				return this.getEf();
			}
		}
		return null;
	}

	/**
	 * 按种族、性别与武器获取动作时间。
	 * Gets motion time for race, gender and weapon.
	 *
	 * 阵营 / race
	 * gender
	 * weapon wrapper
	 * @return 时间，未知则 0 / time or 0
	 */
	public int getTimeForWeapon(Race race, Gender gender, WeaponTypeWrapper weapon) {

		switch (race) {
		case ASMODIANS:
			if (gender == Gender.MALE) {
				return this.getAm().getTimeForWeapon(weapon);
			} else {
				return this.getAf().getTimeForWeapon(weapon);
			}
		case ELYOS:
			if (gender == Gender.MALE) {
				return this.getEm().getTimeForWeapon(weapon);
			} else {
				return this.getEf().getTimeForWeapon(weapon);
			}
		}
		return 0;
	}
}
