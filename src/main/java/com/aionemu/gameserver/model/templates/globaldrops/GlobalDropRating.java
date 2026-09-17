package com.aionemu.gameserver.model.templates.globaldrops;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.model.templates.npc.NpcRating;
import lombok.Getter;

/**
 * 全局掉落 Rating 模板（静态数据/XML）。
 * Global drop rating template (static data/XML).
 *
 * @author Wnkrz
 */
@Getter
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "GlobalDropRating")
public class GlobalDropRating {
	/** 返回 NPC 等级。 / Returns the rating. */
	@XmlAttribute(name = "rating", required = true)
	protected NpcRating rating;
}
