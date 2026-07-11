package com.aionemu.gameserver.model;

import jakarta.xml.bind.annotation.XmlEnum;

/**
 * 性别枚举。
 * Gender enumeration.
 *
 * @author SoulKeeper
 */
@XmlEnum
public enum Gender {
	/**
	 * 男性 / Males
	 */
	MALE(0),

	/**
	 * 女性 / Females
	 */
	FEMALE(1),

	/**
	 * 创建用占位 / Dummy for create
	 */
	DUMMY(8);

	/**
	 * 性别 ID / id of gender
	 */
	private int genderId;

	/**
	 * 构造方法。 / Constructor.
	 */
	private Gender(int genderId) {
		this.genderId = genderId;
	}

	/**
	 * 返回该性别 ID。
	 * Get id of this gender.
	 * 
	 * @return gender id
	 */
	public int getGenderId() {
		return genderId;
	}
}
