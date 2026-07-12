package com.aionemu.gameserver.dataholders;

import java.util.List;

import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

import com.aionemu.gameserver.model.templates.SkillSkinTemplate;

import com.aionemu.commons.utils.collections.IntObjectHashMap;

/**
 * 技能皮肤数据容器，按皮肤 ID 索引 SkillSkinTemplate。
 * Skill skin data holder, indexing SkillSkinTemplate by skin id.
 */
@XmlRootElement(name = "skill_skins")
@XmlAccessorType(XmlAccessType.FIELD)
public class SkillSkinData {

	@XmlElement(name = "skill_skin")
	private List<SkillSkinTemplate> sst;
	private IntObjectHashMap<SkillSkinTemplate> skillskins;

	/**
	 * JAXB 反序列化完成后，将技能皮肤写入 ID 索引并释放列表。
	 * After JAXB unmarshalling, indexes skill skins by id and releases the list.
	 */
	void afterUnmarshal(Unmarshaller u, Object parent) {
		skillskins = new IntObjectHashMap<SkillSkinTemplate>();
		for (SkillSkinTemplate st : sst) {
			skillskins.put(st.getId(), st);
		}
		sst = null;
	}

	/**
	 * 按皮肤 ID 获取技能皮肤模板。
	 * Returns the skill skin template for the given skin id.
	 *
	 * skin id
	 *
	 * @param skinId
	 * @return 技能皮肤模板，不存在则为 null / skill skin template or null
	 */
	public SkillSkinTemplate getSkillSkinTemplate(int skinId) {
		return skillskins.get(skinId);
	}

	/**
	 * 返回已加载的技能皮肤数量。
	 * Returns the number of loaded skill skins.
	 *
	 * skin count
	 */
	public int size() {
		return skillskins.size();
	}
}
