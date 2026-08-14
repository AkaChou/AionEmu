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

	/** 材料名称。 / Material name. */
	@XmlAttribute
	protected String name;
	/** 物品 ID。 / Item id. */
	@XmlAttribute
	protected int itemid;
	/** 名称 ID。 / Name id. */
	@XmlAttribute
	protected int nameid;
	/** 掉落率。 / Drop rate. */
	@XmlAttribute
	protected int rate;

	/**
	 * 获取名称属性值。 / Gets the value of the name property
	 *
	 * @return 可能的返回对象 / possible object is {@link String }
	 */
	public String getName() {
		return name;
	}

	/**
	 * 获取物品 ID。
	 * Gets the item id.
	 *
	 * @return 物品 ID / the itemid
	 */
	public int getItemid() {
		return itemid;
	}

	 /**
	  * 获取 nameid 属性值。
	  * Gets the value of the nameid property
	  * @return 可能的返回对象 / possible object is {@link Integer }
	  */
	public int getNameid() {
		return nameid * 2 + 1;
	}

	 /**
	  * 获取 rate 属性值。
	  * Gets the value of the rate property
	  * @return 可能的返回对象 / possible object is {@link Integer }
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
