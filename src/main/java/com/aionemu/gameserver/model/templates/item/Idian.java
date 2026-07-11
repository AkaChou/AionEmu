package com.aionemu.gameserver.model.templates.item;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;

/**
 * 伊迪安模板（静态数据/XML）。
 * XML template. / XML template.
 *
 * @author Rolandas
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Idian")
public class Idian {

	@XmlAttribute(name = "defend")
	private int burnDefend;

	@XmlAttribute(name = "attack")
	private int burnAttack;

	/** 返回 burn attack / Returns the burn attack */
	public int getBurnAttack() {
		return burnAttack;
	}

	/** 返回 burn defend / Returns the burn defend */
	public int getBurnDefend() {
		return burnDefend;
	}
}
