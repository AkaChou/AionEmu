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

import com.aionemu.gameserver.model.templates.event.GameExperience;

import com.aionemu.commons.utils.collections.IntObjectHashMap;

/**
 * 游戏体验物品配置数据容器，按 ID 索引体验物品模板。
 * Game experience item configuration data holder, indexed by id.
 *
 * @author Rinzler (Encom)
 */
@XmlRootElement(name = "game_experience_items")
@XmlAccessorType(XmlAccessType.FIELD)
public class GameExperienceData {
	@XmlElement(name = "game_experience_item")
	private List<GameExperience> glist;

	@XmlTransient
	private IntObjectHashMap<GameExperience> experienceData = new IntObjectHashMap<GameExperience>();

	@XmlTransient
	private Map<Integer, GameExperience> experienceDataMap = new HashMap<Integer, GameExperience>(1);

	/**
	 * JAXB 反序列化完成后，将体验物品写入 ID 索引。
	 * After JAXB unmarshalling, indexes game experience items by id.
	 */
	void afterUnmarshal(Unmarshaller paramUnmarshaller, Object paramObject) {
		for (GameExperience gameExperience : glist) {
			experienceData.put(gameExperience.getId(), gameExperience);
			experienceDataMap.put(gameExperience.getId(), gameExperience);
		}
	}

	/**
	 * 返回体验物品模板数量。
	 * Returns the number of game experience templates.
	 *
	 * @return 游戏经验模板数量 / Returns the number of game experience templates.
	 */
	public int size() {
		return experienceData.size();
	}

	/**
	 * 按 ID 获取游戏体验物品模板。
	 * Returns the game experience template for the given id.
	 *
	 * @param id 体验物品 ID / game experience id
	 * @return 体验物品模板，不存在则为 null / game experience template, or null if absent
	 */
	public GameExperience getGameExperienceId(int id) {
		return experienceData.get(id);
	}

	/**
	 * 返回全部体验物品映射。
	 * Returns the map of all game experience items.
	 *
	 * @return ID 到体验物品的映射 / map of id to game experience
	 */
	public Map<Integer, GameExperience> getAll() {
		return experienceDataMap;
	}
}
