package com.aionemu.gameserver.dataholders;

import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;

import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlTransient;

import com.aionemu.gameserver.skillengine.model.SkinSkillTemplate;

import com.aionemu.commons.utils.collections.IntObjectHashMap;

/**
 * 皮肤技能数据容器，按 ID 与组名双索引 SkinSkillTemplate。
 * Skin skill data holder, dual-indexing SkinSkillTemplate by id and group name.
 *
 * @author Ranastic
 */
@XmlRootElement(name = "skin_skills")
@XmlAccessorType(XmlAccessType.FIELD)
public class SkinSkillData {

	@XmlElement(name = "skin_skill")
	private List<SkinSkillTemplate> tlist;

	@XmlTransient
	private IntObjectHashMap<SkinSkillTemplate> skinSkillData = new IntObjectHashMap<SkinSkillTemplate>();

	private final Map<String, SkinSkillTemplate> string = new LinkedHashMap<String, SkinSkillTemplate>();

	/**
	 * JAXB 反序列化完成后，按 ID 与大写组名写入双索引。
	 * After JAXB unmarshalling, indexes by id and upper-case group name.
	 */
	void afterUnmarshal(Unmarshaller paramUnmarshaller, Object paramObject) {
		for (SkinSkillTemplate skinSkill : tlist) {
			skinSkillData.put(skinSkill.getId(), skinSkill);
			string.put(skinSkill.getGroup().toUpperCase(), skinSkill);
		}
	}

	/**
	 * 返回已加载的皮肤技能数量。
	 * Returns the number of loaded skin skills.
	 *
	 * template count
	 */
	public int size() {
		return skinSkillData.size();
	}

	/**
	 * 按 ID 获取皮肤技能模板。
	 * Returns the skin skill template for the given id.
	 *
	 * @param id 皮肤技能 ID / skin skill id
	 * @return 模板，不存在则为 null / template or null
	 */
	public SkinSkillTemplate getSkinSkillById(int id) {
		return skinSkillData.get(id);
	}

	/**
	 * 按组名获取皮肤技能模板。
	 * Returns the skin skill template for the given group name.
	 *
	 * group name
	 *
	 * @param name
	 * @return 模板，不存在则为 null / template or null
	 */
	public SkinSkillTemplate getSkinSkillByGroupName(String name) {
		return string.get(name);
	}
}
