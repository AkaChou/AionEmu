package com.aionemu.gameserver.dataholders;

import java.util.List;

import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlTransient;

import com.aionemu.gameserver.model.templates.pet.PetDopingEntry;

import com.aionemu.commons.utils.collections.ShortObjectHashMap;

/**
 * 宠物药剂（doping）数据容器，按 short ID 索引条目。
 * Pet doping data holder, indexing entries by short id.
 */
@XmlRootElement(name = "dopings")
@XmlAccessorType(XmlAccessType.FIELD)
public class PetDopingData {

	@XmlElement(name = "doping")
	private List<PetDopingEntry> list;

	@XmlTransient
	private ShortObjectHashMap<PetDopingEntry> dopingsById = new ShortObjectHashMap<PetDopingEntry>();

	/**
	 * JAXB 反序列化完成后，将药剂条目写入 ID 索引并释放列表。
	 * After JAXB unmarshalling, indexes doping entries by id and releases the list.
	 */
	void afterUnmarshal(Unmarshaller u, Object parent) {
		for (PetDopingEntry dope : list) {
			dopingsById.put(dope.getId(), dope);
		}
		list.clear();
		list = null;
	}

	/**
	 * 返回已加载的药剂条目数量。
	 * Returns the number of loaded doping entries.
	 *
	 * entry count
	 */
	public int size() {
		return dopingsById.size();
	}

	/**
	 * 按 ID 获取宠物药剂模板。
	 * Returns the pet doping template for the given id.
	 *
	 * @param id 药剂 ID / doping id
	 * @return 药剂条目，不存在则为 null / doping entry or null
	 */
	public PetDopingEntry getDopingTemplate(short id) {
		return dopingsById.get(id);
	}
}
