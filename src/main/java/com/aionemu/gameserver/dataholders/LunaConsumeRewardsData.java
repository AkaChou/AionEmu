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

import com.aionemu.gameserver.model.templates.luna.LunaConsumeRewardsTemplate;

import com.aionemu.commons.utils.collections.IntObjectHashMap;

/**
 * 露娜消费奖励数据容器，按奖励 ID 与累计消费点数索引。
 * Luna consume-reward data holder, indexing templates by reward id and cumulative consume points.
 *
 * @author Ranastic
 */
@XmlRootElement(name = "luna_consume_rewards")
@XmlAccessorType(XmlAccessType.FIELD)
public class LunaConsumeRewardsData {

	@XmlElement(name = "luna_consume_reward")
	private List<LunaConsumeRewardsTemplate> lunaList;

	@XmlTransient
	private IntObjectHashMap<LunaConsumeRewardsTemplate> lunaData = new IntObjectHashMap<LunaConsumeRewardsTemplate>();

	@XmlTransient
	private IntObjectHashMap<LunaConsumeRewardsTemplate> lunaConsumeCountData = new IntObjectHashMap<LunaConsumeRewardsTemplate>();

	@XmlTransient
	private Map<Integer, LunaConsumeRewardsTemplate> lunaDataMap = new HashMap<Integer, LunaConsumeRewardsTemplate>(1);

	/**
	 * JAXB 反序列化完成后，按 ID 与累计点数建立索引。
	 * After JAXB unmarshalling, indexes templates by id and cumulative point count.
	 */
	void afterUnmarshal(Unmarshaller paramUnmarshaller, Object paramObject) {
		for (LunaConsumeRewardsTemplate lunaConsume : lunaList) {
			lunaData.put(lunaConsume.getId(), lunaConsume);
			lunaConsumeCountData.put(lunaConsume.getSumCount(), lunaConsume);
			lunaDataMap.put(lunaConsume.getId(), lunaConsume);
		}
	}

	/**
	 * 返回已加载的露娜消费奖励数量。
	 * Returns the number of loaded Luna consume rewards.
	 *
	 * template count
	 */
	public int size() {
		return lunaData.size();
	}

	/**
	 * 按奖励 ID 获取露娜消费奖励模板。
	 * Returns the Luna consume-reward template for the given id.
	 *
	 * @param id 奖励 ID / reward id
	 * @return 奖励模板或 null / reward template or null
	 */
	public LunaConsumeRewardsTemplate getLunaConsumeRewardsId(int id) {
		return lunaData.get(id);
	}

	/**
	 * 按累计消费点数获取露娜消费奖励模板。
	 * Returns the Luna consume-reward template for the given cumulative point total.
	 *
	 * @param point 累计消费点数 / cumulative consume points
	 * @return 奖励模板或 null / reward template or null
	 */
	public LunaConsumeRewardsTemplate getLunaConsumeRewardsBypoint(int point) {
		return lunaConsumeCountData.get(point);
	}

	/**
	 * 返回全部露娜消费奖励映射。
	 * Returns the full Luna consume-reward map.
	 *
	 * @return ID 到奖励模板的映射 / map of id to reward template
	 */
	public Map<Integer, LunaConsumeRewardsTemplate> getAll() {
		return lunaDataMap;
	}
}
