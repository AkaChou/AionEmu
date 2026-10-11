package com.aionemu.gameserver.model.templates.item;

import java.util.ArrayList;
import java.util.List;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import lombok.Getter;

/**
 * 烙印之石模板：技能、所需技能与碎片消耗。
 * Stigma template: skills, required skills and shard cost.
 * @author Rinzler
 */

@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name = "Stigma")
public class Stigma {
	@XmlElement(name = "require_skill")
	protected List<RequireSkill> requireSkill;

	@XmlAttribute
	protected List<String> skill;

	/**
	 * -- GETTER --
	 * 返回烙印之石碎片 / Returns the shard
	 */
	@Getter
	@XmlAttribute
	protected int shard;

	/** 返回技能 / Returns the skills */
	public List<StigmaSkill> getSkills() {
		List<StigmaSkill> list = new ArrayList<>();
		for (String st : skill) {
			String[] array = st.split(":");
			list.add(new StigmaSkill(Integer.parseInt(array[0]), Integer.parseInt(array[1])));
		}
		return list;
	}

	/** 返回所需技能 / Returns the require skill */
	public List<RequireSkill> getRequireSkill() {
		if (requireSkill == null) {
			requireSkill = new ArrayList<>();
		}
		return this.requireSkill;
	}

	/**
	 * @param skillId  -- GETTER --
	 *                 返回技能 ID / Returns the skill id
	 * @param skillLvl -- GETTER --
	 *                 返回技能等级 / Returns the skill lvl
	 */
	public record StigmaSkill(int skillLvl, int skillId) {
	}
}
