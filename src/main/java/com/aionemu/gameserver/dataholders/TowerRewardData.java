package com.aionemu.gameserver.dataholders;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlTransient;

import com.aionemu.gameserver.model.templates.tower_reward.TowerStageRewardTemplate;

import com.aionemu.commons.utils.collections.IntObjectHashMap;

/**
 * 永恒之塔阶段奖励数据容器，按楼层索引奖励模板。
 * Tower stage-reward data holder, indexing reward templates by floor.
 *
 * @author Wnkrz
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "tower_reward_templates")
public class TowerRewardData {
	@XmlElement(name = "tower_reward_template")
	private List<TowerStageRewardTemplate> TowerReward;

	@XmlTransient
	private IntObjectHashMap<TowerStageRewardTemplate> templates = new IntObjectHashMap<TowerStageRewardTemplate>();

	@XmlTransient
	private Map<Integer, TowerStageRewardTemplate> templatesMap = new HashMap<Integer, TowerStageRewardTemplate>();

	/**
	 * JAXB 反序列化完成后，将奖励模板按楼层索引并释放列表。
	 * After JAXB unmarshalling, indexes reward templates by floor and clears the list.
	 */
	void afterUnmarshal(Unmarshaller u, Object parent) {
		for (TowerStageRewardTemplate template : TowerReward) {
			templates.put(template.getFloor(), template);
			templatesMap.put(template.getFloor(), template);
		}
		TowerReward.clear();
		TowerReward = null;
	}

	/**
	 * 返回已加载的奖励模板数量。
	 * Returns the number of loaded reward templates.
	 *
	 * template count
	 */
	public int size() {
		return templates.size();
	}

	/**
	 * 按楼层获取塔奖励模板。
	 * Returns the tower reward template for the given floor id.
	 *
	 * floor id
	 *
	 * @param towerId
	 * @return 奖励模板，不存在则为 null / reward template or null
	 */
	public TowerStageRewardTemplate getTowerReward(int towerId) {
		return templates.get(towerId);
	}

	/**
	 * 返回全部塔奖励模板映射。
	 * Returns the full map of tower reward templates.
	 *
	 * @return 奖励模板映射 / reward template map
	 */
	public Map<Integer, TowerStageRewardTemplate> getAll() {
		return templatesMap;
	}
}
