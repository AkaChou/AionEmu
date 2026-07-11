package com.aionemu.gameserver.dataholders;


import com.aionemu.boot.i18n.I18n;
import java.util.List;

import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

import lombok.extern.slf4j.Slf4j;

import com.aionemu.gameserver.model.templates.npcskill.NpcSkillTemplates;

import com.aionemu.commons.utils.collections.IntObjectHashMap;

/**
 * NPC 技能模板数据容器，按 NPC ID 索引 {@link NpcSkillTemplates}。
 * NPC skill template data holder, indexing {@link NpcSkillTemplates} by npc id.
 *
 * @author ATracer
 */
@Slf4j
@XmlRootElement(name = "npc_skill_templates")
@XmlAccessorType(XmlAccessType.FIELD)
public class NpcSkillData {

	@XmlElement(name = "npcskills")
	private List<NpcSkillTemplates> npcSkills;

	/** 全部 NPC 技能模板映射 / map containing all npc skill templates */
	private IntObjectHashMap<NpcSkillTemplates> npcSkillData = new IntObjectHashMap<NpcSkillTemplates>();

	/**
	 * JAXB 反序列化完成后，按 NPC ID 建立索引；技能列表为空时记录错误日志。
	 * After JAXB unmarshalling, indexes templates by npc id; logs an error when skill list is null.
	 */
	void afterUnmarshal(Unmarshaller u, Object parent) {
		for (NpcSkillTemplates npcSkill : npcSkills) {
			npcSkillData.put(npcSkill.getNpcId(), npcSkill);

			if (npcSkill.getNpcSkills() == null) {
				log.error(I18n.get("log.8da628ab1d37"));
			}
		}
	}

	/**
	 * 返回已加载的 NPC 技能模板数量。
	 * Returns the number of loaded NPC skill templates.
	 *
	 * template count
	 */
	public int size() {
		return npcSkillData.size();
	}

	/**
	 * 按 NPC ID 获取技能模板列表。
	 * Returns the skill template list for the given npc id.
	 *
	 * npc id
	 *
	 * @param id @return 技能模板或 null / skill templates or null
	 */
	public NpcSkillTemplates getNpcSkillList(int id) {
		return npcSkillData.get(id);
	}
}
