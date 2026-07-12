package com.aionemu.gameserver.model.templates.outpost;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;

/**
 * 前哨模板（静态数据/XML）。
 * XML template.
 */

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Outpost")
public class OutpostTemplate {
	@XmlAttribute(name = "id")
	protected int id;

	@XmlAttribute(name = "world")
	protected int world;

	@XmlAttribute(name = "name")
	protected String nameId;

	@XmlAttribute(name = "artifact_id")
	protected int artifactId;

	/** 返回 ID / Returns the id */
	public int getId() {
		return this.id;
	}

	/** 返回世界 ID / Returns the world id */
	public int getWorldId() {
		return this.world;
	}

	/** 获取名称。 / Returns the name. */
	public String getName() {
		return nameId;
	}

	/** 返回 artifact id / Returns the artifact id */
	public int getArtifactId() {
		return artifactId;
	}
}
