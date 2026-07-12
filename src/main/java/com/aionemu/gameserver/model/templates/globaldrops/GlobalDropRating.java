package com.aionemu.gameserver.model.templates.globaldrops;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.model.templates.npc.NpcRating;

/**
 * 全局掉落 Rating 模板（静态数据/XML）。
 * XML template.
 *
 * @author Wnkrz
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "GlobalDropRating")
public class GlobalDropRating {
	@XmlAttribute(name = "rating", required = true)
	protected NpcRating rating;

	/** 返回 rating / Returns the rating */
	public NpcRating getRating() {
		return rating;
	}
}
