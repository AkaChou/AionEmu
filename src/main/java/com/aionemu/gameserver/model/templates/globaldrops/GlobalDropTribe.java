package com.aionemu.gameserver.model.templates.globaldrops;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.model.TribeClass;

/**
 * 全局掉落部落模板（静态数据/XML）。
 * Global drop tribe template (static data/XML).
 *
 * @author Wnkrz
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "GlobalDropTribe")
public class GlobalDropTribe {
	@XmlAttribute(name = "tribe", required = true)
	protected TribeClass tribe;

	/** 获取部落。 / Returns the tribe. */
	public TribeClass getTribe() {
		return tribe;
	}
}
