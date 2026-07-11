package com.aionemu.gameserver.model.templates.globaldrops;

import java.util.ArrayList;
import java.util.List;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;

/**
 * 全局掉落 Zones 模板（静态数据/XML）。
 * XML template. / XML template.
 *
 * @author Wnkrz
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "GlobalDropZones")
public class GlobalDropZones {
	@XmlElement(name = "gd_zone")
	protected List<GlobalDropZone> gdZones;

	/** 返回 global drop zones / Returns the global drop zones */
	public List<GlobalDropZone> getGlobalDropZones() {
		if (gdZones == null) {
			gdZones = new ArrayList<GlobalDropZone>();
		}
		return this.gdZones;
	}
}
