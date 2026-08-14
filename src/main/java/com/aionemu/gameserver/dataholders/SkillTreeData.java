package com.aionemu.gameserver.dataholders;

import java.util.ArrayList;
import java.util.List;

import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

import com.aionemu.gameserver.model.PlayerClass;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.skillengine.model.SkillLearnTemplate;

import com.aionemu.commons.utils.collections.IntObjectHashMap;

/**
 * 技能树数据容器，按职业/阵营/等级哈希与技能 ID 双索引学习模板。
 * Skill tree data holder, dual-indexing learn templates by class/race/level hash and skill id.
 *
 * @author ATracer
 */
@XmlRootElement(name = "skill_tree")
@XmlAccessorType(XmlAccessType.FIELD)
public class SkillTreeData {

	@XmlElement(name = "skill")
	private List<SkillLearnTemplate> skillTemplates;

	private final IntObjectHashMap<ArrayList<SkillLearnTemplate>> templates = new IntObjectHashMap<ArrayList<SkillLearnTemplate>>();
	private final IntObjectHashMap<ArrayList<SkillLearnTemplate>> templatesById = new IntObjectHashMap<ArrayList<SkillLearnTemplate>>();

	/**
	 * JAXB 反序列化完成后，将学习模板写入双索引并释放列表。
	 * After JAXB unmarshalling, indexes learn templates into both maps and releases the list.
	 */
	void afterUnmarshal(Unmarshaller u, Object parent) {
		for (SkillLearnTemplate template : skillTemplates) {
			addTemplate(template);
		}
		skillTemplates = null;
	}

	private void addTemplate(SkillLearnTemplate template) {
		Race race = template.getRace();
		if (race == null) {
			race = Race.PC_ALL;
		}

		int hash = makeHash(template.getClassId().ordinal(), race.ordinal(), template.getMinLevel());
		ArrayList<SkillLearnTemplate> value = templates.get(hash);
		if (value == null) {
			value = new ArrayList<SkillLearnTemplate>();
			templates.put(hash, value);
		}
		value.add(template);

		value = templatesById.get(template.getSkillId());
		if (value == null) {
			value = new ArrayList<SkillLearnTemplate>();
			templatesById.put(template.getSkillId(), value);
		}
		value.add(template);
	}

	/**
	 * 返回按职业/阵营/等级哈希索引的学习模板映射。
	 * Returns the learn-template map indexed by class/race/level hash.
	 *
	 * @return 哈希到模板列表的映射 / map of hash to template list
	 */
	public IntObjectHashMap<ArrayList<SkillLearnTemplate>> getTemplates() {
		return templates;
	}

	/**
	 * 查找可学习技能：职业+阵营专用、职业通用、全职业通用三类。
	 * Finds learnable skills: class+race specific, class-only, and general (all class/race).
	 *
	 * @param playerClass 职业 / player class
	 * @param level 等级 / level
	 * @param race 阵营 / race
	 * @return 匹配的学习模板数组 / matching learn templates
	 */
	public SkillLearnTemplate[] getTemplatesFor(PlayerClass playerClass, int level, Race race) {
		List<SkillLearnTemplate> newSkills = new ArrayList<SkillLearnTemplate>();

		List<SkillLearnTemplate> classRaceSpecificTemplates = templates
				.get(makeHash(playerClass.ordinal(), race.ordinal(), level));
		List<SkillLearnTemplate> classSpecificTemplates = templates
				.get(makeHash(playerClass.ordinal(), Race.PC_ALL.ordinal(), level));
		List<SkillLearnTemplate> generalTemplates = templates
				.get(makeHash(PlayerClass.ALL.ordinal(), Race.PC_ALL.ordinal(), level));

		if (classRaceSpecificTemplates != null) {
			newSkills.addAll(classRaceSpecificTemplates);
		}
		if (classSpecificTemplates != null) {
			newSkills.addAll(classSpecificTemplates);
		}
		if (generalTemplates != null) {
			newSkills.addAll(generalTemplates);
		}
		return newSkills.toArray(new SkillLearnTemplate[newSkills.size()]);
	}

	/**
	 * 按技能 ID 返回全部学习模板。
	 * Returns all learn templates for the given skill id.
	 *
	 * @param skillId 技能 ID / skill id
	 * @return 学习模板数组 / learn template array
	 */
	public SkillLearnTemplate[] getTemplatesForSkill(int skillId) {
		List<SkillLearnTemplate> searchSkills = new ArrayList<SkillLearnTemplate>();

		List<SkillLearnTemplate> byId = templatesById.get(skillId);
		if (byId != null) {
			searchSkills.addAll(byId);
		}
		return searchSkills.toArray(new SkillLearnTemplate[searchSkills.size()]);
	}

	/**
	 * 判断技能是否出现在技能树中（可学习）。
	 * Returns whether the skill appears in the skill tree (is learnable).
	 *
	 * @param skillId 技能 ID / skill id
	 * @return 是否可学习 / whether it is a learned skill
	 */
	public boolean isLearnedSkill(int skillId) {
		return templatesById.get(skillId) != null;
	}

	/**
	 * 返回全部学习模板条目总数。
	 * Returns the total number of learn-template entries.
	 *
	 * @return 模板条目总数 / total entry count
	 */
	public int size() {
		int size = 0;
		for (Integer key : templates.keys()) {
			size += templates.get(key).size();
		}
		return size;
	}

	private static int makeHash(int classId, int race, int level) {
		int result = classId << 10;
		result = (result | race) << 10;
		return result | level;
	}
}
