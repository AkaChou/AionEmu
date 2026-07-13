package com.aionemu.gameserver.dataholders;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.Set;

import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlTransient;

import com.aionemu.gameserver.skillengine.model.SkillTemplate;
import com.aionemu.gameserver.skillengine.model.ExclusiveAttribute;

import com.aionemu.commons.utils.collections.IntObjectHashMap;

/**
 * 技能模板数据容器，按技能 ID 与技能组索引 SkillTemplate。
 * Skill template data holder indexed by skill id and skill group.
 */
@XmlRootElement(name = "skill_data")
@XmlAccessorType(XmlAccessType.FIELD)
public class SkillData {

	@XmlElement(name = "skill_template")
	private List<SkillTemplate> skillTemplates;

	@XmlTransient
	private HashMap<Integer, ArrayList<Integer>> cooldownGroups;
	@XmlTransient
	private Set<Integer> persistentCooldownGroups;

	@XmlTransient
	private IntObjectHashMap<SkillTemplate> skillData = new IntObjectHashMap<SkillTemplate>();

	@XmlTransient
	private final Map<String, SkillTemplate> skillGroup = new LinkedHashMap<String, SkillTemplate>();
	@XmlTransient
	private Map<Integer, Set<String>> itemExclusiveAttributes = Map.of();
	@XmlTransient
	private Map<String, ExclusiveAttribute> exclusiveAttributes = Map.of();

	/**
	 * JAXB 反序列化完成后，重建技能 ID 与技能组索引。
	 * After JAXB unmarshalling, rebuilds the skill-id and skill-group indexes.
	 */
	void afterUnmarshal(Unmarshaller u, Object parent) {
		skillData.clear();
		skillGroup.clear();
		for (SkillTemplate st : skillTemplates) {
			skillData.put(st.getSkillId(), st);
			skillGroup.put(st.getStack().replace("SKILL_", ""), st);
		}
	}

	/**
	 * 按技能 ID 获取技能模板。
	 * Returns the skill template for the given skill id.
	 *
	 * skill id
	 *
	 * @param skillId
	 * @return 技能模板或 null / skill template or null
	 */
	public SkillTemplate getSkillTemplate(int skillId) {
		return skillData.get(skillId);
	}

	/**
	 * 返回已加载的技能模板数量。
	 * Returns the number of loaded skill templates.
	 *
	 * template count
	 */
	public int size() {
		return skillData.size();
	}

	/**
	 * 按技能组名获取技能模板。
	 * Returns the skill template for the given skill group name.
	 *
	 * @param name 技能组名 / skill group name
	 * @return 技能模板或 null / skill template or null
	 */
	public SkillTemplate getSkillTemplateByGroup(String name) {
		return skillGroup.get(name);
	}

	/**
	 * 返回技能组索引条目数量。
	 * Returns the number of skill-group index entries.
	 *
	 * group count
	 */
	public int sizeOfGroup() {
		return skillGroup.size();
	}

	/**
	 * 返回原始技能模板列表。
	 * Returns the raw skill template list.
	 *
	 * @return 技能模板列表 / skill template list
	 */
	public List<SkillTemplate> getSkillTemplates() {
		return skillTemplates;
	}

	/**
	 * 设置技能模板列表并立即重建索引。
	 * Sets the skill template list and rebuilds indexes immediately.
	 *
	 * @param skillTemplates 技能模板列表 / skill template list
	 */
	public void setSkillTemplates(List<SkillTemplate> skillTemplates) {
		this.skillTemplates = skillTemplates;
		afterUnmarshal(null, null);
	}

	/**
	 * 初始化冷却组：按 delayId 聚合技能 ID 列表。
	 * Initializes cooldown groups by aggregating skill ids under each delay id.
	 */
	public void initializeCooldownGroups() {
		cooldownGroups = new HashMap<Integer, ArrayList<Integer>>();
		persistentCooldownGroups = new HashSet<>();
		for (SkillTemplate skillTemplate : skillTemplates) {
			int delayId = skillTemplate.getDelayId();
			if (!cooldownGroups.containsKey(delayId)) {
				cooldownGroups.put(delayId, new ArrayList<Integer>());
			}
			cooldownGroups.get(delayId).add(skillTemplate.getSkillId());
			if (skillTemplate.isRemainCooltimeOnLogin()) {
				persistentCooldownGroups.add(delayId);
			}
		}
	}

	/**
	 * 返回共享同一 delayId 的技能 ID 列表；必要时懒初始化冷却组。
	 * Returns skill ids that share the given delay id; lazily initializes cooldown groups if needed.
	 *
	 * @param delayId 延迟/冷却组 ID / delay or cooldown group id
	 * skill id list
	 */
	public ArrayList<Integer> getSkillsForDelayId(int delayId) {
		if (cooldownGroups == null) {
			initializeCooldownGroups();
		}
		return cooldownGroups.get(delayId);
	}

	public boolean shouldPersistCooldown(int delayId) {
		if (persistentCooldownGroups == null) {
			initializeCooldownGroups();
		}
		return persistentCooldownGroups.contains(delayId);
	}

	/**
	 * 返回技能 ID 到模板的映射。
	 * Returns the skill-id to template map.
	 *
	 * @return 技能数据映射 / skill data map
	 */
	public IntObjectHashMap<SkillTemplate> getSkillData() {
		return skillData;
	}

	public void setExclusiveAttributes(Map<String, ExclusiveAttribute> exclusiveAttributes,
			Map<Integer, Set<String>> itemExclusiveAttributes) {
		this.exclusiveAttributes = exclusiveAttributes;
		this.itemExclusiveAttributes = itemExclusiveAttributes;
	}

	public ExclusiveAttribute getExclusiveAttribute(String name) {
		return name == null ? null : exclusiveAttributes.get(name);
	}

	public List<ExclusiveAttribute> getItemExclusiveAttributes(int itemId) {
		return itemExclusiveAttributes.getOrDefault(itemId, Set.of()).stream().map(exclusiveAttributes::get).toList();
	}

	public int applyExclusiveSkillReduction(int damage, Collection<Integer> itemIds, String skillAttributeName) {
		ExclusiveAttribute skill = getExclusiveAttribute(skillAttributeName);
		if (skill == null) {
			return damage;
		}
		int flat = 0;
		int percent = 0;
		for (int itemId : itemIds) {
			for (ExclusiveAttribute item : getItemExclusiveAttributes(itemId)) {
				if (item != null && item.tag().equals(skill.tag())) {
					flat += item.skillFlat();
					percent += item.skillPercent();
				}
			}
		}
		return Math.max(0, damage - flat) * Math.max(0, 100 - percent) / 100;
	}

	public int getExclusiveStatusImmune(Collection<Integer> itemIds, String skillAttributeName) {
		ExclusiveAttribute skill = getExclusiveAttribute(skillAttributeName);
		return skill == null ? 0 : itemIds.stream().flatMap(id -> getItemExclusiveAttributes(id).stream())
			.filter(item -> item != null && item.tag().equals(skill.tag()))
			.mapToInt(ExclusiveAttribute::statusImmune).sum();
	}
}
