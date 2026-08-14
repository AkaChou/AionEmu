package com.aionemu.gameserver.dataholders;

import java.util.List;

import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElements;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlTransient;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.model.templates.housing.PlaceableHouseObject;

import com.aionemu.commons.utils.collections.IntObjectHashMap;

/**
 * 房屋可放置物件配置数据容器，按模板 ID 索引各类房屋物件。
 * Housing placeable object configuration data holder, indexed by template id.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = { "housingObjects" })
@XmlRootElement(name = "housing_objects")
public class HousingObjectData {
	@XmlElements({
			@XmlElement(name = "postbox", type = com.aionemu.gameserver.model.templates.housing.HousingPostbox.class),
			@XmlElement(name = "use_item", type = com.aionemu.gameserver.model.templates.housing.HousingUseableItem.class),
			@XmlElement(name = "move_item", type = com.aionemu.gameserver.model.templates.housing.HousingMoveableItem.class),
			@XmlElement(name = "chair", type = com.aionemu.gameserver.model.templates.housing.HousingChair.class),
			@XmlElement(name = "picture", type = com.aionemu.gameserver.model.templates.housing.HousingPicture.class),
			@XmlElement(name = "passive", type = com.aionemu.gameserver.model.templates.housing.HousingPassiveItem.class),
			@XmlElement(name = "npc", type = com.aionemu.gameserver.model.templates.housing.HousingNpc.class),
			@XmlElement(name = "storage", type = com.aionemu.gameserver.model.templates.housing.HousingStorage.class),
			@XmlElement(name = "jukebox", type = com.aionemu.gameserver.model.templates.housing.HousingJukeBox.class),
			@XmlElement(name = "moviejukebox", type = com.aionemu.gameserver.model.templates.housing.HousingMovieJukeBox.class),
			@XmlElement(name = "emblem", type = com.aionemu.gameserver.model.templates.housing.HousingEmblem.class) })
	protected List<PlaceableHouseObject> housingObjects;

	@XmlTransient
	protected IntObjectHashMap<PlaceableHouseObject> objectTemplatesById = new IntObjectHashMap<PlaceableHouseObject>();

	/**
	 * JAXB 反序列化完成后，按模板 ID 建立索引并释放原始列表。
	 * After JAXB unmarshalling, indexes objects by template id and releases the raw list.
	 */
	void afterUnmarshal(Unmarshaller u, Object parent) {
		if (housingObjects == null) {
			return;
		}
		for (PlaceableHouseObject obj : housingObjects) {
			objectTemplatesById.put(obj.getTemplateId(), obj);
		}
		housingObjects.clear();
		housingObjects = null;
	}

	/**
	 * 返回房屋物件模板数量。
	 * Returns the number of housing object templates.
	 *
	 * @return 房屋物件模板数量 / Returns the number of housing object templates.
	 */
	public int size() {
		return objectTemplatesById.size();
	}

	/**
	 * 按模板 ID 获取可放置房屋物件。
	 * Returns the placeable housing object for the given template id.
	 *
	 * @param templateId 物件模板 ID / object template id
	 * @return 房屋物件模板，不存在则为 null / housing object template, or null if absent
	 */
	public PlaceableHouseObject getTemplateById(int templateId) {
		return objectTemplatesById.get(templateId);
	}
}
