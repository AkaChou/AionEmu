package com.aionemu.gameserver.model.templates.rift;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;
import lombok.Getter;

/**
 * 裂隙模板（静态数据/XML）。
 * XML template.
 */

@Getter
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Rift")
public class RiftTemplate {
	/** 裂隙 ID / Rift id */
	@XmlAttribute(name = "id")
	protected int id;

	/** 世界 ID / World id */
	@XmlAttribute(name = "world")
	protected int world;

	/** 返回世界 ID / Returns the world id */
	public int getWorldId() {
		return this.world;
	}
}
