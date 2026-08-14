package com.aionemu.gameserver.dataholders;

import java.util.List;

import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlTransient;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.model.templates.stats.AbsoluteStatsTemplate;
import com.aionemu.gameserver.model.templates.stats.ModifiersTemplate;

import com.aionemu.commons.utils.collections.IntObjectHashMap;

/**
 * 绝对属性数据容器，按属性集 ID 索引修正模板。
 * Absolute stats data holder, indexing modifier templates by stat-set id.
 *
 * @author Rolandas
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = { "absoluteStats" })
@XmlRootElement(name = "absolute_stats")
public class AbsoluteStatsData {

	@XmlElement(name = "stats_set", required = true)
	protected List<AbsoluteStatsTemplate> absoluteStats;

	@XmlTransient
	private IntObjectHashMap<ModifiersTemplate> absoluteStatsData = new IntObjectHashMap<ModifiersTemplate>();

	/**
	 * JAXB 反序列化完成后，将属性集索引到修正模板映射并释放列表。
	 * After JAXB unmarshalling, indexes modifiers by stat-set id and clears the list.
	 */
	void afterUnmarshal(Unmarshaller u, Object parent) {
		for (AbsoluteStatsTemplate stats : absoluteStats) {
			absoluteStatsData.put(stats.getId(), stats.getModifiers());
		}
		absoluteStats.clear();
		absoluteStats = null;
	}

	/**
	 * 按属性集 ID 获取修正模板。
	 * Returns the modifiers template for the given stat-set id.
	 *
	 * @param statSetId 属性套装 ID / stat-set id
	 * @return 修正模板，不存在则为 null / modifiers template or null
	 */
	public ModifiersTemplate getTemplate(int statSetId) {
		return absoluteStatsData.get(statSetId);
	}

	/**
	 * 返回已加载的属性集数量。
	 * Returns the number of loaded stat sets.
	 *
	 * @return 已加载的属性套装数量 / Returns the number of loaded stat sets.
	 */
	public int size() {
		return absoluteStatsData.size();
	}
}
