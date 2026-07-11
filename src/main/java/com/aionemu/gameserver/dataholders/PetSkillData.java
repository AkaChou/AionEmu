package com.aionemu.gameserver.dataholders;

import java.util.List;

import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

import com.aionemu.gameserver.model.templates.petskill.PetSkillTemplate;

import com.aionemu.commons.utils.collections.IntArrayList;
import com.aionemu.commons.utils.collections.IntIntHashMap;
import com.aionemu.commons.utils.collections.IntObjectHashMap;

/**
 * 宠物技能数据容器，按指令技能与宠物 NPC ID 索引技能映射。
 * Pet skill data holder, indexing skill mappings by order skill and pet npc id.
 *
 * @author ATracer
 */
@XmlRootElement(name = "pet_skill_templates")
@XmlAccessorType(XmlAccessType.FIELD)
public class PetSkillData {

	@XmlElement(name = "pet_skill")
	private List<PetSkillTemplate> petSkills;

	/** 指令技能 → (宠物 NPC ID → 技能 ID) 映射 / order skill → (pet npc id → skill id) */
	private IntObjectHashMap<IntIntHashMap> petSkillData = new IntObjectHashMap<IntIntHashMap>();

	/** 宠物 NPC ID → 技能 ID 列表 / pet npc id → skill id list */
	private IntObjectHashMap<IntArrayList> petSkillsMap = new IntObjectHashMap<IntArrayList>();

	/**
	 * JAXB 反序列化完成后，构建指令技能与宠物技能索引。
	 * After JAXB unmarshalling, builds order-skill and pet-skill indexes.
	 */
	void afterUnmarshal(Unmarshaller u, Object parent) {
		for (PetSkillTemplate petSkill : petSkills) {
			IntIntHashMap orderSkillMap = petSkillData.get(petSkill.getOrderSkill());
			if (orderSkillMap == null) {
				orderSkillMap = new IntIntHashMap();
				petSkillData.put(petSkill.getOrderSkill(), orderSkillMap);
			}
			orderSkillMap.put(petSkill.getPetId(), petSkill.getSkillId());

			IntArrayList skillList = petSkillsMap.get(petSkill.getPetId());
			if (skillList == null) {
				skillList = new IntArrayList();
				petSkillsMap.put(petSkill.getPetId(), skillList);
			}
			skillList.add(petSkill.getSkillId());
		}
	}

	/**
	 * 返回指令技能索引条目数量。
	 * Returns the number of order-skill index entries.
	 *
	 * index count
	 */
	public int size() {
		return petSkillData.size();
	}

	/**
	 * 按指令技能与宠物 NPC ID 获取宠物技能 ID。
	 * Returns the pet skill id for the given order skill and pet npc id.
	 *
	 * order skill id
	 * pet npc id
	 * skill id
	 */
	public int getPetOrderSkill(int orderSkill, int petNpcId) {
		return petSkillData.get(orderSkill).get(petNpcId);
	}

	/**
	 * 判断指定宠物是否拥有某技能。
	 * Returns whether the given pet has the skill.
	 *
	 * pet npc id
	 * skill id
	 * whether the pet has the skill
	 */
	public boolean petHasSkill(int petNpcId, int skillId) {
		return petSkillsMap.get(petNpcId).contains(skillId);
	}
}
