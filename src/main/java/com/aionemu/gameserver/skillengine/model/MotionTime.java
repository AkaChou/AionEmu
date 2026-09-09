package com.aionemu.gameserver.skillengine.model;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.model.Gender;
import com.aionemu.gameserver.model.Race;
import lombok.Getter;
import lombok.Setter;

/**
 * 动作时间表：按种族/性别分组的武器动作时间（am/af/em/ef）。
 * Motion time table: per race/gender weapon motion times (am/af/em/ef).
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "motion_time", propOrder = { "am", "af", "em", "ef" })
public class MotionTime {

	/**
	 * 获取阿斯摩男性时间表。
	 * Gets Asmodian male times.
	 *
	 */
	@Getter
	@Setter
	protected Times am;
	/**
	 * 获取阿斯摩女性时间表。
	 * Gets Asmodian female times.
	 *
	 */
	@Getter
	@Setter
	protected Times af;
	/**
	 * 获取天族男性时间表。
	 * Gets Elyos male times.
	 *
	 */
	@Getter
	@Setter
	protected Times em;
	/**
	 * 获取天族女性时间表。
	 * Gets Elyos female times.
	 *
	 */
	@Getter
	@Setter
	protected Times ef;

	/**
	 * 获取动作名称。
	 * Gets motion name.
	 *
	 */
	@Getter
	@Setter
	@XmlAttribute(required = true)
	protected String name;

	/**
	 * 按种族与性别获取时间表。
	 * Gets times by race and gender.
	 *
	 * 阵营 / race
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
