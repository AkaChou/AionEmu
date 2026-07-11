package com.aionemu.gameserver.model.templates.abyss_bonus;

import java.util.Collections;
import java.util.List;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;

/**
 * 欧比斯队伍 Attr 模板（静态数据/XML）。
 * XML template. / XML template.
 */

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "AbyssGroupAttr")
public class AbyssGroupAttr {
	@XmlAttribute(name = "buff_id", required = true)
	protected int buffId;

	@XmlAttribute(name = "world")
	protected List<Integer> world;

	@XmlAttribute(name = "name", required = true)
	private String name;

	/** 返回增益 ID / Returns the buff id */
	public int getBuffId() {
		return buffId;
	}

	/** 设置 buff id / Sets the buff id */
	public void setBuffId(int value) {
		buffId = value;
	}

	/** 获取名称。 / Returns the name. */
	public String getName() {
		return name;
	}

	/** 返回世界 ID / Returns the world id */
	public List<Integer> getWorldId() {
		if (world == null) {
			world = Collections.emptyList();
		}
		return this.world;
	}
}
