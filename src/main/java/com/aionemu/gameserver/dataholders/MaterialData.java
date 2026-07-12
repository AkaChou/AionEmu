package com.aionemu.gameserver.dataholders;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlTransient;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.model.templates.materials.MaterialSkill;
import com.aionemu.gameserver.model.templates.materials.MaterialTemplate;

/**
 * 材质模板数据容器，按材质 ID 索引并缓存相关技能 ID。
 * Material template data holder, indexing by material id and caching related skill ids.
 *
 * @author Rolandas
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = { "materialTemplates" })
@XmlRootElement(name = "material_templates")
public class MaterialData {

	@XmlElement(name = "material")
	protected List<MaterialTemplate> materialTemplates;

	@XmlTransient
	Map<Integer, MaterialTemplate> materialsById = new HashMap<Integer, MaterialTemplate>();

	@XmlTransient
	Set<Integer> skillIds = new HashSet<Integer>();

	/**
	 * JAXB 反序列化完成后，按材质 ID 索引并收集技能 ID，随后释放列表。
	 * After JAXB unmarshalling, indexes by material id, collects skill ids, then clears the list.
	 */
	void afterUnmarshal(Unmarshaller u, Object parent) {
		if (materialTemplates == null)
			return;

		for (MaterialTemplate template : materialTemplates) {
			materialsById.put(template.getId(), template);
			if (template.getSkills() != null) {
				for (MaterialSkill skill : template.getSkills()) {
					skillIds.add(skill.getId());
				}
			}
		}

		materialTemplates.clear();
		materialTemplates = null;
	}

	/**
	 * 按材质 ID 获取材质模板。
	 * Returns the material template for the given material id.
	 *
	 * material id
	 *
	 * @param materialId
	 * @return 材质模板或 null / material template or null
	 */
	public MaterialTemplate getTemplate(int materialId) {
		return materialsById.get(materialId);
	}

	/**
	 * 判断给定技能 ID 是否为材质技能。
	 * Returns whether the given skill id is a material skill.
	 *
	 * skill id
	 *
	 * @param skillId
	 * @return 是材质技能则为 true / true if it is a material skill
	 */
	public boolean isMaterialSkill(int skillId) {
		return skillIds.contains(skillId);
	}

	/**
	 * 返回已加载的材质模板数量。
	 * Returns the number of loaded material templates.
	 *
	 * template count
	 */
	public int size() {
		return materialsById.size();
	}
}
