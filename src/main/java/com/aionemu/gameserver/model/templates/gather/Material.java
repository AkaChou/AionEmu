package com.aionemu.gameserver.model.templates.gather;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;

/**
 * 材料模板（静态数据/XML）。
 * XML template.
 *
 * @author ATracer
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Material")
public class Material implements Comparable<Material> {

	@XmlAttribute
	protected String name;
	@XmlAttribute
	protected int itemid;
	@XmlAttribute
	protected int nameid;
	@XmlAttribute
	protected int rate;

	/**
	 * 获取 value 的名称 property。 / Gets the value of the name property
	 *
	 * @return possible object is {@link String }
	 */
	public String getName() {
		return name;
	}

	/**
	 * @return the itemid
	 */
	public int getItemid() {
		return itemid;
	}

	 /**
	  * 获取 nameid 属性值。
	  * Gets the value of the nameid property
	  * @return possible object is {@link Integer }
	  */
	public int getNameid() {
		return nameid * 2 + 1;
	}

	 /**
	  * 获取 rate 属性值。
	  * Gets the value of the rate property
	  * @return possible object is {@link Integer }
	  */
	public int getRate() {
		return rate;
	}

	/** 比较。 / Compares to another instance. */
	@Override
	public int compareTo(Material o) {
		return o.rate - rate;
	}
}
