package com.aionemu.gameserver.dataholders;

import java.util.List;

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
 * @author Ranastic
 */
@XmlRootElement(name = "skin_skills")
@XmlAccessorType(XmlAccessType.FIELD)
public class SkinSkillData {

	@XmlElement(name = "skin_skill")
	private List<SkinSkillTemplate> tlist;

	@XmlTransient
	private final IntObjectHashMap<SkinSkillTemplate> skinSkillData = new IntObjectHashMap<>();

	/**
	 * JAXB 反序列化完成后，按 ID 建立索引。
	 * After JAXB unmarshalling, indexes by id.
	 */
	void afterUnmarshal(Unmarshaller paramUnmarshaller, Object paramObject) {
		for (SkinSkillTemplate skinSkill : tlist) {
			skinSkillData.put(skinSkill.getId(), skinSkill);
		}
	}

	/**
	 * 返回已加载的皮肤技能数量。
	 * Returns the number of loaded skin skills.
	 * @return 已加载的皮肤技能数量 / Returns the number of loaded skin skills.
	 */
	public int size() {
		return skinSkillData.size();
	}


}
