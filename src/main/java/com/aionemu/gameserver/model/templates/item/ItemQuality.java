package com.aionemu.gameserver.model.templates.item;

import jakarta.xml.bind.annotation.XmlEnum;
import jakarta.xml.bind.annotation.XmlType;

/**
 * 物品 Quality 枚举。
 * Item Quality enumeration.
 */

@XmlType(name = "quality")
@XmlEnum
public enum ItemQuality {
	/** 垃圾 / Junk. */
	JUNK(0), // Junk - Grey
	/** 公共。 / Common. */
	COMMON(1), // Common - White
	/** 稀有 / Rare. */
	RARE(2), // Superior - Green
	/** 传颂 / Legend. */
	LEGEND(3), // Heroic - Blue
	/** 唯一 / Unique. */
	UNIQUE(4), // Fabled - Yellow
	/** 史诗 / Epic. */
	EPIC(5), // Eternal - Orange
	/** 神话 / Mythic. */
	MYTHIC(6); // Mythic - Purple

	private int qualityId;

	private ItemQuality(int qualityId) {
		this.qualityId = qualityId;
	}

	/** 返回 quality id / Returns the quality id */
	public int getQualityId() {
		return qualityId;
	}
}
