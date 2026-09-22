package com.aionemu.gameserver.model.templates.zorshivdredgion;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;
import lombok.Getter;

/**
 * 佐希夫无畏舰模板（静态数据/XML）。
 * XML template.
 * @author Rinzler (Encom)
 */

@Getter
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "zorshiv_dredgion")
public class ZorshivDredgionTemplate {
	/** 返回 ID / Returns the id */
	@XmlAttribute(name = "id")
	protected int id;

	@XmlAttribute(name = "name")
	protected String nameId;

	/** 获取名称。 / Returns the name. */
	public String getName() {
		return nameId;
	}
}
