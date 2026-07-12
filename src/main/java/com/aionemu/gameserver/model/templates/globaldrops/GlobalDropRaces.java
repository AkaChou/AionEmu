package com.aionemu.gameserver.model.templates.globaldrops;

import java.util.ArrayList;
import java.util.List;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;

/**
 * 全局掉落 Races 模板（静态数据/XML）。
 * XML template.
 *
 * @author Wnkrz
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "GlobalDropRaces")
public class GlobalDropRaces {
	@XmlElement(name = "gd_race")
	protected List<GlobalDropRace> gdRaces;

	/** 返回全局掉落种族 / Returns the global drop races*/
	public List<GlobalDropRace> getGlobalDropRaces() {
		if (gdRaces == null) {
			gdRaces = new ArrayList<GlobalDropRace>();
		}
		return this.gdRaces;
	}
}
