package com.aionemu.gameserver.skillengine.model;

import jakarta.xml.bind.annotation.XmlEnum;
import jakarta.xml.bind.annotation.XmlType;
import lombok.Getter;

/**
 * 烙印类型：无、基础、进阶。
 * Stigma type: none, basic or advanced.
 *
 * @author Cheatkiller
 */
@Getter
@XmlType(name = "StigmaType")
@XmlEnum
public enum StigmaType {

	/** 无 / None */
	NONE(0),
	/** 基础烙印 / Basic stigma */
	BASIC(1),
	/** 进阶烙印 / Advanced stigma */
	ADVANCED(2);

	/**
	 * 获取协议 ID。
	 * Gets protocol id.
	 *
	 */
	private final int id;

	StigmaType(int id) {
		this.id = id;
	}
}
