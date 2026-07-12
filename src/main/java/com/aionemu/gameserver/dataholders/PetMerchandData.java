package com.aionemu.gameserver.dataholders;

import java.util.List;

import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlTransient;

import com.aionemu.gameserver.model.templates.pet.PetMerchandEntry;

import com.aionemu.commons.utils.collections.IntObjectHashMap;

/**
 * 宠物商人数据容器，按 ID 索引 PetMerchandEntry。
 * Pet merchant data holder, indexing PetMerchandEntry by id.
 *
 * @author Rinzler
 */
@XmlRootElement(name = "merchands")
@XmlAccessorType(XmlAccessType.FIELD)
public class PetMerchandData {
	@XmlElement(name = "merchand")
	private List<PetMerchandEntry> list;

	@XmlTransient
	private IntObjectHashMap<PetMerchandEntry> merchandsById = new IntObjectHashMap<PetMerchandEntry>();

	public PetMerchandData() {
	}

	public PetMerchandData(List<PetMerchandEntry> entries) {
		for (PetMerchandEntry entry : entries) {
			merchandsById.put(entry.getId(), entry);
		}
	}

	/**
	 * JAXB 反序列化完成后，将商人条目写入 ID 索引并释放列表。
	 * After JAXB unmarshalling, indexes merchant entries by id and releases the list.
	 */
	void afterUnmarshal(Unmarshaller u, Object parent) {
		for (PetMerchandEntry merch : list) {
			merchandsById.put(merch.getId(), merch);
		}
		list.clear();
		list = null;
	}

	/**
	 * 返回已加载的商人条目数量。
	 * Returns the number of loaded merchant entries.
	 *
	 * entry count
	 */
	public int size() {
		return merchandsById.size();
	}

	/**
	 * 按 ID 获取宠物商人模板。
	 * Returns the pet merchant template for the given id.
	 *
	 * @param id 商人 ID / merchant id
	 * @return 商人条目，不存在则为 null / merchant entry or null
	 */
	public PetMerchandEntry getMerchandTemplate(int id) {
		return merchandsById.get(id);
	}
}
