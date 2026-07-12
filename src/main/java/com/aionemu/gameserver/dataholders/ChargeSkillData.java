package com.aionemu.gameserver.dataholders;

import java.util.List;
import java.util.Map;

import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

import com.aionemu.gameserver.skillengine.model.ChargeSkillTemplate;

import com.aionemu.commons.utils.collections.IntObjectHashMap;
import java.util.LinkedHashMap;

/**
 * 蓄力技能数据容器，按 ID、套装名及技能阶段多路索引。
 * Charge skill data holder, multi-indexed by id, set name, and skill stages.
 *
 * @author Dr.Nism [Ranastic]
 */
@XmlRootElement(name = "charge_skills")
@XmlAccessorType(XmlAccessType.FIELD)
public class ChargeSkillData {

	@XmlElement(name = "charge_skill")
	private List<ChargeSkillTemplate> chargeSkills;

	private IntObjectHashMap<ChargeSkillTemplate> ids = new IntObjectHashMap<ChargeSkillTemplate>();
	private final Map<String, ChargeSkillTemplate> setName = new LinkedHashMap<String, ChargeSkillTemplate>();
	private IntObjectHashMap<ChargeSkillTemplate> firstTemplates = new IntObjectHashMap<ChargeSkillTemplate>();
	private IntObjectHashMap<ChargeSkillTemplate> totalTemplates = new IntObjectHashMap<ChargeSkillTemplate>();

	/**
	 * JAXB 反序列化完成后，按 ID、套装名与各阶段技能 ID 建立多路索引。
	 * After JAXB unmarshalling, multi-indexes by id, set name, and stage skill ids.
	 */
	void afterUnmarshal(Unmarshaller u, Object parent) {
		for (ChargeSkillTemplate chargeSkill : chargeSkills) {
			ids.put(chargeSkill.getId(), chargeSkill);
			setName.put(chargeSkill.getChargeSetName(), chargeSkill);
			firstTemplates.put(chargeSkill.getFirstId(), chargeSkill);
			totalTemplates.put(chargeSkill.getFirstId(), chargeSkill);
			totalTemplates.put(chargeSkill.getSecondId(), chargeSkill);
			totalTemplates.put(chargeSkill.getThirdId(), chargeSkill);
		}
		chargeSkills = null;
	}

	/**
	 * 返回已加载的蓄力技能数量。
	 * Returns the number of loaded charge skills.
	 *
	 * template count
	 */
	public int size() {
		return ids.size();
	}

	/**
	 * 按 ID 获取蓄力技能模板。
	 * Returns the charge skill template for the given id.
	 *
	 * @param id 模板 ID / template id
	 * @return 模板，不存在则为 null / template or null
	 */
	public ChargeSkillTemplate getChargeSkillTemplateById(int id) {
		return ids.get(id);
	}

	/**
	 * 按套装名获取蓄力技能模板。
	 * Returns the charge skill template for the given set name.
	 *
	 * set name
	 *
	 * @param name
	 * @return 模板，不存在则为 null / template or null
	 */
	public ChargeSkillTemplate getChargeSkillTemplateBySetName(String name) {
		return setName.get(name);
	}

	/**
	 * 按第一阶段技能 ID 获取蓄力技能模板。
	 * Returns the charge skill template for the given first-stage skill id.
	 *
	 * @param skillId 第一阶段技能 ID / first-stage skill id
	 * @return 模板，不存在则为 null / template or null
	 */
	public ChargeSkillTemplate getChargeSkillTemplate1st(int skillId) {
		return firstTemplates.get(skillId);
	}

	/**
	 * 按任意阶段技能 ID 获取蓄力技能模板。
	 * Returns the charge skill template for any stage skill id.
	 *
	 * skill id
	 *
	 * @param skillId
	 * @return 模板，不存在则为 null / template or null
	 */
	public ChargeSkillTemplate getChargeSkillTemplateTotal(int skillId) {
		return totalTemplates.get(skillId);
	}
}
