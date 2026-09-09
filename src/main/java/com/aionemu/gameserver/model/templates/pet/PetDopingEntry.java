package com.aionemu.gameserver.model.templates.pet;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * 宠物兴奋剂条目模板（静态数据/XML）。
 * Pet doping entry template (static data / XML).
 *
 * @author Rolandas
 */
@XmlType(name = "dope")
@XmlAccessorType(XmlAccessType.NONE)
@NoArgsConstructor
@AllArgsConstructor
public class PetDopingEntry {

	/**
	 * @return 条目 ID / the id
	 */
	@Getter
	@XmlAttribute(name = "id", required = true)
	private short id;

	@XmlAttribute(name = "usedrink", required = true)
	private boolean usedrink;

	@XmlAttribute(name = "usefood", required = true)
	private boolean usefood;

	@XmlAttribute(name = "usescroll", required = true)
	private int usescroll;

	/**
	 * @return 是否使用饮品 / the usedrink
	 */
	public boolean isUseDrink() {
		return usedrink;
	}

	/**
	 * @return 是否使用食物 / the usefood
	 */
	public boolean isUseFood() {
		return usefood;
	}

	/**
	 * @return 卷轴使用数 / the usescroll
	 */
	public int getScrollsUsed() {
		return usescroll;
	}
}
