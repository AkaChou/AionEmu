package com.aionemu.gameserver.model.templates;

import java.util.List;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

import com.aionemu.gameserver.model.templates.expand.Expand;

/**
 * 背包扩展模板（静态数据/XML）。
 * Cube expand template (static data / XML).
 *
 * @author Simple
 */
@XmlRootElement(name = "cube_npc")
@XmlAccessorType(XmlAccessType.FIELD)
public class CubeExpandTemplate {

	@XmlElement(name = "expand", required = true)
	protected List<Expand> cubeExpands;

	@XmlAttribute(name = "id", required = true)
	private int Id;

	/** 返回 NPC ID / Returns the npc id */
	public int getNpcId() {
		return Id;
	}

	/** 是否包含。 / Contains. */
	public boolean contains(int level) {
		for (Expand expand : cubeExpands) {
			if (expand.getLevel() == level) {
				return true;
			}
		}
		return false;
	}

	/** 获取。 / Get. */
	public Expand get(int level) {
		for (Expand expand : cubeExpands) {
			if (expand.getLevel() == level) {
				return expand;
			}
		}
		return null;
	}
}
