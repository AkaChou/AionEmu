package com.aionemu.gameserver.model.templates.globaldrops;

import java.util.ArrayList;
import java.util.List;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;

/**
 * 全局掉落 Npcs 模板（静态数据/XML）。
 * XML template.
 *
 * @author Wnkrz
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "GlobalDropNpcs")
public class GlobalDropNpcs {
	@XmlElement(name = "gd_npc")
	protected List<GlobalDropNpc> gdNpcs;

	/** 返回 global drop npcs / Returns the global drop npcs */
	public List<GlobalDropNpc> getGlobalDropNpcs() {
		if (gdNpcs == null) {
			gdNpcs = new ArrayList<GlobalDropNpc>();
		}
		return this.gdNpcs;
	}
}
