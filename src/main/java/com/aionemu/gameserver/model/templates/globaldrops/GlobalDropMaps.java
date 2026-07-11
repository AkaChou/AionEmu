package com.aionemu.gameserver.model.templates.globaldrops;

import java.util.ArrayList;
import java.util.List;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;

/**
 * 全局掉落 Maps 模板（静态数据/XML）。
 * XML template. / XML template.
 *
 * @author Wnkrz
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "GlobalDropMaps")
public class GlobalDropMaps {
	@XmlElement(name = "gd_map")
	protected List<GlobalDropMap> gdMaps;

	/** 返回 global drop maps / Returns the global drop maps */
	public List<GlobalDropMap> getGlobalDropMaps() {
		if (gdMaps == null) {
			gdMaps = new ArrayList<GlobalDropMap>();
		}
		return this.gdMaps;
	}
}
