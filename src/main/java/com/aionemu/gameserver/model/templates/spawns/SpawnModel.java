package com.aionemu.gameserver.model.templates.spawns;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.model.TribeClass;

/**
 * 刷新点 Model 模板（静态数据/XML）。
 * XML template.
 */

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "SpawnModel")
public class SpawnModel {
	@XmlAttribute(name = "tribe")
	private TribeClass tribe;

	@XmlAttribute(name = "ai")
	private String ai;

	/** 获取部落。 / Returns the tribe. */
	public TribeClass getTribe() {
		return tribe;
	}

	/** 返回 ai / Returns the ai */
	public String getAi() {
		return ai;
	}
}
