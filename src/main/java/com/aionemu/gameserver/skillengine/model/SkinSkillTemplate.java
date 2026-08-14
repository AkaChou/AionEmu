package com.aionemu.gameserver.skillengine.model;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlRootElement;

/**
 * 皮肤技能模板：外观/皮肤关联的技能元数据。
 * Skin skill template: skill metadata linked to appearance/skin.
 *
 * @author Ranastic
 */
@XmlRootElement(name = "skin_skill")
@XmlAccessorType(XmlAccessType.NONE)
public class SkinSkillTemplate {

	@XmlAttribute(name = "id", required = true)
	private int id;

	@XmlAttribute(name = "name")
	private String name = "";

	@XmlAttribute(name = "desc")
	private String desc = "";

	@XmlAttribute(name = "skill_group")
	private String skill_group;

	/**
	 * 获取技能 ID。
	 * Gets skill id.
	 *
	 */
	public int getId() {
		return id;
	}

	/**
	 * 获取名称。
	 * Gets name.
	 *
	 */
	public String getName() {
		return name;
	}

	/**
	 * 获取描述。
	 * Gets description.
	 *
	 */
	public String getDesc() {
		return desc;
	}

	/**
	 * 获取技能分组。
	 * Gets skill group.
	 *
	 */
	public String getGroup() {
		return skill_group;
	}
}
