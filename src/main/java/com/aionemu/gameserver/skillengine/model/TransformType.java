package com.aionemu.gameserver.skillengine.model;

import jakarta.xml.bind.annotation.XmlEnum;
import jakarta.xml.bind.annotation.XmlType;

/**
 * 变身类型：玩家、化身及多种形态。
 * Transform type: PC, avatar and form variants.
 */
@XmlType(name = "TransformType")
@XmlEnum
public enum TransformType {

	/** 无 / None */
	NONE(0),
	/** 玩家形态 / Player character form */
	PC(1),
	/** 化身 / Avatar */
	AVATAR(2),
	/** 形态 1 / Form 1 */
	FORM1(3),
	/** 形态 2 / Form 2 */
	FORM2(4),
	/** 形态 3 / Form 3 */
	FORM3(5),
	/** 形态 4 / Form 4 */
	FORM4(6),
	/** 形态 5 / Form 5 */
	FORM5(7);

	private int id;

	private TransformType(int id) {
		this.id = id;
	}

	/**
	 * 获取协议 ID。
	 * Gets protocol id.
	 *
	 */
	public int getId() {
		return id;
	}
}
