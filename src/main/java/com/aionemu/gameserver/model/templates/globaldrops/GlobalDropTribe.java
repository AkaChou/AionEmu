package com.aionemu.gameserver.model.templates.globaldrops;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.model.TribeClass;
import lombok.Getter;

/**
 * 全局掉落部落模板（静态数据/XML）。
 * Global drop tribe template (static data/XML).
 *
 * @author Wnkrz
 */
@Getter
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "GlobalDropTribe")
public class GlobalDropTribe {
	/** 获取部落。 / Returns the tribe. */
	@XmlAttribute(name = "tribe", required = true)
	protected TribeClass tribe;
}
