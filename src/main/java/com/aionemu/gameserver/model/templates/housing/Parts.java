package com.aionemu.gameserver.model.templates.housing;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlType;
import lombok.Getter;

/**
 * Parts 模板（静态数据/XML）。
 * XML template.
 *
 * @author Rolandas
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Parts", propOrder = { "fence", "garden", "frame", "outwall", "roof", "infloor", "inwall", "door" })
public class Parts {

	/**
	 * 获取 fence 属性值。
	 * Gets the value of the fence property
	 * @return possible object is {@link Integer }
	 */
	@Getter
	protected Integer fence;
	/**
	 * 获取 garden 属性值。
	 * Gets the value of the garden property
	 * @return possible object is {@link Integer }
	 */
	@Getter
	protected Integer garden;
	/**
	 * 获取 frame 属性值。
	 * Gets the value of the frame property
	 * @return possible object is {@link Integer }
	 */
	@Getter
	protected Integer frame;
	/**
	 * 获取 outwall 属性值。
	 * Gets the value of the outwall property
	 * @return possible object is {@link Integer }
	 */
	@Getter
	protected Integer outwall;
	/**
	 * 获取 roof 属性值。
	 * Gets the value of the roof property
	 * @return possible object is {@link Integer }
	 */
	@Getter
	protected Integer roof;
	/**
	 * 获取 infloor 属性值。
	 * Gets the value of the infloor property
	 */
	@Getter
	protected int infloor;
	/**
	 * 获取 inwall 属性值。
	 * Gets the value of the inwall property
	 */
	@Getter
	protected int inwall;
	/**
	 * 获取 door 属性值。
	 * Gets the value of the door property
	 */
	@Getter
	protected int door;
}
