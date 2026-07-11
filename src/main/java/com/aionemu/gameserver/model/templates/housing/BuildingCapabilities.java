package com.aionemu.gameserver.model.templates.housing;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;

/**
 * BuildingCapabilities 模板（静态数据/XML）。
 * XML template. / XML template.
 *
 * @author Rolandas
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "")
@XmlRootElement(name = "caps")
public class BuildingCapabilities {

	@XmlAttribute(required = true)
	protected boolean addon;

	@XmlAttribute(required = true)
	protected int emblemId;

	@XmlAttribute(required = true)
	protected boolean floor;

	@XmlAttribute(required = true)
	protected boolean room;

	@XmlAttribute(required = true)
	protected int interior;

	@XmlAttribute(required = true)
	protected int exterior;

	/** Whether 有 addon / Whether have addon */
	public boolean canHaveAddon() {
		return addon;
	}

	/** 返回 emblem id / Returns the emblem id */
	public int getEmblemId() {
		return emblemId;
	}

	/**
	 * @return Whether change floor / Whether change floor
	 */
	public boolean canChangeFloor() {
		return floor;
	}

	/**
	 * @return Whether change room / Whether change room
	 */
	public boolean canChangeRoom() {
		return room;
	}

	/**
	 * @return Whether change interior / Whether change interior
	 */
	public int canChangeInterior() {
		return interior;
	}

	/**
	 * @return Whether change exterior / Whether change exterior
	 */
	public int canChangeExterior() {
		return exterior;
	}
}
