package com.aionemu.gameserver.model.templates.housing;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlType;

/**
 * Parts 模板（静态数据/XML）。
 * XML template. / XML template.
 *
 * @author Rolandas
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Parts", propOrder = { "fence", "garden", "frame", "outwall", "roof", "infloor", "inwall", "door" })
public class Parts {

	protected Integer fence;
	protected Integer garden;
	protected Integer frame;
	protected Integer outwall;
	protected Integer roof;
	protected int infloor;
	protected int inwall;
	protected int door;

	 /**
	  * 获取 fence 属性值。
	  * Gets the value of the fence property
	  * @return possible object is {@link Integer }
	  */
	public Integer getFence() {
		return fence;
	}

	 /**
	  * 获取 garden 属性值。
	  * Gets the value of the garden property
	  * @return possible object is {@link Integer }
	  */
	public Integer getGarden() {
		return garden;
	}

	 /**
	  * 获取 frame 属性值。
	  * Gets the value of the frame property
	  * @return possible object is {@link Integer }
	  */
	public Integer getFrame() {
		return frame;
	}

	 /**
	  * 获取 outwall 属性值。
	  * Gets the value of the outwall property
	  * @return possible object is {@link Integer }
	  */
	public Integer getOutwall() {
		return outwall;
	}

	 /**
	  * 获取 roof 属性值。
	  * Gets the value of the roof property
	  * @return possible object is {@link Integer }
	  */
	public Integer getRoof() {
		return roof;
	}

	 /**
	  * 获取 infloor 属性值。
	  * Gets the value of the infloor property
	  */
	public int getInfloor() {
		return infloor;
	}

	 /**
	  * 获取 inwall 属性值。
	  * Gets the value of the inwall property
	  */
	public int getInwall() {
		return inwall;
	}

	 /**
	  * 获取 door 属性值。
	  * Gets the value of the door property
	  */
	public int getDoor() {
		return door;
	}
}
