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

import com.aionemu.gameserver.model.templates.panel_cp.StoneCP;

import com.aionemu.commons.utils.collections.IntObjectHashMap;

/**
 * 面板 CP 石数据容器，按石 ID 索引 CP 石模板。
 * Panel CP-stone data holder, indexing stone templates by stone id.
 *
 * @author Rinzler (Encom)
 */
@XmlRootElement(name = "stones_cp")
@XmlAccessorType(XmlAccessType.FIELD)
public class StoneCpData {
	@XmlElement(name = "stone_cp")
	private List<StoneCP> stonelist;

	@XmlTransient
	private IntObjectHashMap<StoneCP> stoneData = new IntObjectHashMap<StoneCP>();

	@XmlTransient
	private Map<Integer, StoneCP> stoneDataMap = new HashMap<Integer, StoneCP>(1);

	/**
	 * JAXB 反序列化完成后，将 CP 石模板索引到两套映射中。
	 * After JAXB unmarshalling, indexes CP-stone templates into both maps.
	 */
	void afterUnmarshal(Unmarshaller paramUnmarshaller, Object paramObject) {
		for (StoneCP stoneCp : stonelist) {
			stoneData.put(stoneCp.getId(), stoneCp);
			stoneDataMap.put(stoneCp.getId(), stoneCp);
		}
	}

	/**
	 * 返回已加载的 CP 石数量。
	 * Returns the number of loaded CP stones.
	 *
	 * template count
	 */
	public int size() {
		return stoneData.size();
	}

	/**
	 * 按石 ID 获取 CP 石模板。
	 * Returns the CP-stone template for the given stone id.
	 *
	 * @param id 石 ID / stone id
	 * @return CP 石模板，不存在则为 null / CP-stone template or null
	 */
	public StoneCP getStoneCpId(int id) {
		return stoneData.get(id);
	}

	/**
	 * 返回全部 CP 石模板映射。
	 * Returns the full map of CP-stone templates.
	 *
	 * @return 石 ID → 模板映射 / stone-id to template map
	 */
	public Map<Integer, StoneCP> getAll() {
		return stoneDataMap;
	}
}
