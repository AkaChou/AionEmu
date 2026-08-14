package com.aionemu.gameserver.model.templates.housing;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;

/**
 * BuildingCapabilities 模板（静态数据/XML）。
 * XML template.
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

	/** 是否支持扩建 / Whether have addon */
	public boolean canHaveAddon() {
		return addon;
	}

	/** 返回徽章 ID / Returns the emblem id */
	public int getEmblemId() {
		return emblemId;
	}

	/**
	 * @return 是否可以更换地板 / whether change floor
	 */
	public boolean canChangeFloor() {
		return floor;
	}

	/**
	 * @return 是否可以更换房间 / whether change room
	 */
	public boolean canChangeRoom() {
		return room;
	}

	/**
	 * @return 是否可以更换室内装潢 / whether change interior
	 */
	public int canChangeInterior() {
		return interior;
	}

	/**
	 * @return 是否可以更换室外装潢 / whether change exterior
	 */
	public int canChangeExterior() {
		return exterior;
	}
}
