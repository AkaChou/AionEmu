package com.aionemu.gameserver.dataholders;

import java.util.List;

import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlTransient;

import com.aionemu.gameserver.model.templates.arcadeupgrade.ArcadeTab;
import com.aionemu.gameserver.model.templates.arcadeupgrade.ArcadeTabItem;

import com.aionemu.commons.utils.collections.IntObjectHashMap;

/**
 * 街机升级数据容器，按页签 ID 索引奖品物品列表。
 * Arcade upgrade data holder, indexing prize item lists by tab id.
 *
 * Created by wanke on 17/02/2017.
 */
@XmlRootElement(name = "arcadelist")
@XmlAccessorType(XmlAccessType.FIELD)
public class ArcadeUpgradeData {
	@XmlElement(name = "tab")
	private List<ArcadeTab> arcadeTabTemplate;
	@XmlTransient
	private IntObjectHashMap<List<ArcadeTabItem>> arcadeItemList = new IntObjectHashMap<>();

	/**
	 * JAXB 反序列化完成后，按页签 ID 索引物品列表。
	 * After JAXB unmarshalling, indexes item lists by tab id.
	 */
	void afterUnmarshal(Unmarshaller u, Object parent) {
		arcadeItemList.clear();
		for (ArcadeTab template : arcadeTabTemplate) {
			arcadeItemList.put(template.getId(), template.getArcadeTabItems());
		}
	}

	/**
	 * 返回已加载的页签数量。
	 * Returns the number of loaded tabs.
	 *
	 * @return 已加载的选项卡数量 / Returns the number of loaded tabs.
	 */
	public int size() {
		return arcadeItemList.size();
	}

	/**
	 * 按页签 ID 获取物品列表。
	 * Returns the item list for the given tab id.
	 *
	 * @param id 页签 ID / tab id
	 * @return 物品列表，不存在则为 null / item list or null
	 */
	public List<ArcadeTabItem> getArcadeTabById(int id) {
		return arcadeItemList.get(id);
	}

	/**
	 * 返回全部街机页签模板列表。
	 * Returns all arcade tab templates.
	 *
	 * @return 页签模板列表 / tab template list
	 */
	public List<ArcadeTab> getArcadeTabs() {
		return arcadeTabTemplate;
	}
}
