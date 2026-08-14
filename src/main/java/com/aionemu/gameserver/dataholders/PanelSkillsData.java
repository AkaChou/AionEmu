package com.aionemu.gameserver.dataholders;

import java.util.List;

import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

import com.aionemu.gameserver.model.templates.panels.SkillPanel;

import com.aionemu.commons.utils.collections.IntObjectHashMap;

/**
 * 变身技能面板数据容器，按面板 ID 索引 SkillPanel。
 * Polymorph skill panel data holder, indexed by panel id.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "polymorph_panels")
public class PanelSkillsData {

	@XmlElement(name = "panel")
	protected List<SkillPanel> templates;
	private IntObjectHashMap<SkillPanel> skillPanels = new IntObjectHashMap<SkillPanel>();

	/**
	 * JAXB 反序列化完成后，将面板写入 ID 索引并释放列表。
	 * After JAXB unmarshalling, indexes panels by id and releases the list.
	 */
	void afterUnmarshal(Unmarshaller unmarshaller, Object parent) {
		for (SkillPanel panel : templates) {
			skillPanels.put(panel.getPanelId(), panel);
		}
		templates.clear();
		templates = null;
	}

	/**
	 * 按面板 ID 获取技能面板。
	 * Returns the skill panel for the given panel id.
	 *
	 * @param id 面板 ID / panel id
	 * @return 技能面板，不存在则为 null / skill panel or null
	 */
	public SkillPanel getSkillPanel(int id) {
		return (SkillPanel) skillPanels.get(id);
	}

	/**
	 * 返回已加载的技能面板数量。
	 * Returns the number of loaded skill panels.
	 *
	 * @return 已加载的技能面板数量 / Returns the number of loaded skill panels.
	 */
	public int size() {
		return skillPanels.size();
	}
}
