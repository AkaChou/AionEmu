package com.aionemu.gameserver.model.templates.globaldrops;

import java.util.ArrayList;
import java.util.List;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;

/**
 * 全局掉落 Tribes 模板（静态数据/XML）。
 * Global drop tribes template (static data/XML).
 *
 * @author Wnkrz
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "GlobalDropTribes")
public class GlobalDropTribes {
	@XmlElement(name = "gd_tribe")
	protected List<GlobalDropTribe> gdTribes;

	/** 返回全局掉落部落。 / Returns the global drop tribes. */
	public List<GlobalDropTribe> getGlobalDropTribes() {
		if (gdTribes == null) {
			gdTribes = new ArrayList<GlobalDropTribe>();
		}
		return this.gdTribes;
	}
}
