package com.aionemu.gameserver.dataholders;

import java.util.List;

import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlTransient;

import com.aionemu.gameserver.model.templates.luna_dice.LunaDiceItem;
import com.aionemu.gameserver.model.templates.luna_dice.LunaDiceTable;

import com.aionemu.commons.utils.collections.IntObjectHashMap;

/**
 * 露娜骰子奖励表数据容器，按表 ID 索引奖励物品列表。
 * Luna dice reward-table data holder, indexing reward item lists by table id.
 *
 * Created by Wnkrz on 26/07/2017.
 */
@XmlRootElement(name = "luna_dice")
@XmlAccessorType(XmlAccessType.FIELD)
public class LunaDiceData {

	@XmlElement(name = "table")
	private List<LunaDiceTable> lunaDiceTabTemplate;
	@XmlTransient
	private IntObjectHashMap<List<LunaDiceItem>> diceItemList = new IntObjectHashMap<>();

	/**
	 * JAXB 反序列化完成后，按表 ID 建立奖励物品列表索引。
	 * After JAXB unmarshalling, indexes reward item lists by table id.
	 */
	void afterUnmarshal(Unmarshaller u, Object parent) {
		diceItemList.clear();
		for (LunaDiceTable template : lunaDiceTabTemplate) {
			diceItemList.put(template.getId(), template.getLunaDiceTabItems());
		}
	}

	/**
	 * 返回已加载的骰子表数量。
	 * Returns the number of loaded dice tables.
	 *
	 * @return 已加载的骰子表数量 / Returns the number of loaded dice tables.
	 */
	public int size() {
		return diceItemList.size();
	}

	/**
	 * 按表 ID 获取骰子奖励物品列表。
	 * Returns the dice reward item list for the given table id.
	 *
	 * @param id 表 ID / table id
	 * @return 奖励物品列表或 null / reward item list or null
	 */
	public List<LunaDiceItem> getLunaDiceTabById(int id) {
		return diceItemList.get(id);
	}

	/**
	 * 返回全部骰子表模板列表。
	 * Returns the full list of dice table templates.
	 *
	 * @return 骰子表列表 / dice table list
	 */
	public List<LunaDiceTable> getLunaDiceTabs() {
		return lunaDiceTabTemplate;
	}
}
