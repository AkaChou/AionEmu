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
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.model.templates.housing.Building;
import com.aionemu.gameserver.model.templates.housing.HousePart;

/**
 * 房屋部件配置数据容器，按部件 ID 与标签索引房屋部件。
 * House part configuration data holder, indexed by part id and tags.
 * @author Rolandas
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = { "houseParts" })
@XmlRootElement(name = "house_parts")
public class HousePartsData {

	@XmlElement(name = "house_part")
	protected List<HousePart> houseParts;

	@XmlTransient
	Map<Integer, HousePart> partsById = new HashMap<>();

	/**
	 * JAXB 反序列化完成后，按部件 ID 建立索引并释放原始列表。
	 * After JAXB unmarshalling, indexes parts by id, then releases the raw list.
	 */
	void afterUnmarshal(Unmarshaller u, Object parent) {
		if (houseParts == null) {
			return;
		}
		for (HousePart part : houseParts) {
			partsById.put(part.getId(), part);
		}

		houseParts.clear();
		houseParts = null;
	}

	/**
	 * 按部件 ID 获取房屋部件。
	 * Returns the house part for the given part id.
	 * @param partId 部件 ID / part id
	 * @return 房屋部件，不存在则为 null / house part, or null if absent
	 */
	public HousePart getPartById(int partId) {
		return partsById.get(partId);
	}

	/**
	 * 返回房屋部件数量。
	 * Returns the number of house parts.
	 * @return 房屋部件数量 / Returns the number of house parts.
	 */
	public int size() {
		return partsById.size();
	}
}
