package com.aionemu.gameserver.model.templates.globaldrops;

import java.util.ArrayList;
import java.util.List;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;

/**
 * 全局掉落 Worlds 模板（静态数据/XML）。
 * XML template. / XML template.
 *
 * @author Wnkrz
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "GlobalDropWorlds")
public class GlobalDropWorlds {
	@XmlElement(name = "gd_world")
	protected List<GlobalDropWorld> gdWorlds;

	/** 返回 global drop worlds / Returns the global drop worlds */
	public List<GlobalDropWorld> getGlobalDropWorlds() {
		if (gdWorlds == null) {
			gdWorlds = new ArrayList<GlobalDropWorld>();
		}
		return this.gdWorlds;
	}
}
