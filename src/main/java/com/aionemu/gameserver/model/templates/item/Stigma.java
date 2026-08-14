package com.aionemu.gameserver.model.templates.item;

import java.util.ArrayList;
import java.util.List;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

/**
 * 烙印之石模板：技能、所需技能与碎片消耗。
 * Stigma template: skills, required skills and shard cost.
 *
 * @author Rinzler
 */

@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name = "Stigma")
public class Stigma {
	@XmlElement(name = "require_skill")
	protected List<RequireSkill> requireSkill;

	@XmlAttribute
	protected List<String> skill;

	@XmlAttribute
	protected int shard;

	/** 返回技能 / Returns the skills */
	public List<StigmaSkill> getSkills() {
		List<StigmaSkill> list = new ArrayList<StigmaSkill>();
		for (String st : skill) {
			String[] array = st.split(":");
			list.add(new StigmaSkill(Integer.parseInt(array[0]), Integer.parseInt(array[1])));
		}
		return list;
	}

	/** 返回技能 ID 列表（仅 ID）/ Returns the skill ids only */
	public List<Integer> getSkillIdOnly() {
		List<Integer> ids = new ArrayList<Integer>();
		List<String> skill = this.skill;
		if (skill.size() != 1) {
			String[] tempArray = new String[0];
			for (String parts : skill) {
				tempArray = parts.split(":");
				ids.add(Integer.parseInt(tempArray[1]));
			}
			return ids;
		}
		for (String st : this.skill) {
			String[] array = st.split(":");
			ids.add(Integer.parseInt(array[1]));
		}
		return ids;
	}

	/** 返回烙印之石碎片 / Returns the shard */
	public int getShard() {
		return shard;
	}

	/** 返回所需技能 / Returns the require skill */
	public List<RequireSkill> getRequireSkill() {
		if (requireSkill == null) {
			requireSkill = new ArrayList<RequireSkill>();
		}
		return this.requireSkill;
	}

	public static class StigmaSkill {
		private int skillId;
		private int skillLvl;

		public StigmaSkill(int skillLvl, int skillId) {
			this.skillId = skillId;
			this.skillLvl = skillLvl;
		}

		/** 返回技能等级 / Returns the skill lvl */
		public int getSkillLvl() {
			return this.skillLvl;
		}

		/** 返回技能 ID / Returns the skill id */
		public int getSkillId() {
			return this.skillId;
		}
	}
}
