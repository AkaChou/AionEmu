package com.aionemu.gameserver.model.templates.pet;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;

/**
 * 宠物 Doping 条目模板（静态数据/XML）。
 * XML template. / XML template.
 *
 * @author Rolandas
 */
@XmlType(name = "dope")
@XmlAccessorType(XmlAccessType.NONE)
public class PetDopingEntry {

	@XmlAttribute(name = "id", required = true)
	private short id;

	@XmlAttribute(name = "usedrink", required = true)
	private boolean usedrink;

	@XmlAttribute(name = "usefood", required = true)
	private boolean usefood;

	@XmlAttribute(name = "usescroll", required = true)
	private int usescroll;

	/**
	 * @return the id
	 */
	public short getId() {
		return id;
	}

	/**
	 * @return the usedrink
	 */
	public boolean isUseDrink() {
		return usedrink;
	}

	/**
	 * @return the usefood
	 */
	public boolean isUseFood() {
		return usefood;
	}

	/**
	 * @return the usescroll
	 */
	public int getScrollsUsed() {
		return usescroll;
	}
}
