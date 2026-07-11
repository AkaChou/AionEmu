package com.aionemu.gameserver.dataholders;

import java.util.List;

import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

import com.aionemu.gameserver.model.templates.pet.PetTemplate;

import com.aionemu.commons.utils.collections.IntObjectHashMap;

/**
 * 宠物模板数据容器，持有并按 ID 提供全部 {@link PetTemplate}。
 * Pet template data holder that stores and serves all {@link PetTemplate} instances by id.
 *
 * @author IlBuono
 */
@XmlRootElement(name = "pets")
@XmlAccessorType(XmlAccessType.FIELD)
public class PetData {

	@XmlElement(name = "pet")
	private List<PetTemplate> pets;

	/** 按宠物 ID 索引的模板映射 / map of pet templates by id */
	private IntObjectHashMap<PetTemplate> petData = new IntObjectHashMap<PetTemplate>();

	/**
	 * JAXB 反序列化完成后，将宠物模板写入 ID 索引并释放列表。
	 * After JAXB unmarshalling, indexes pet templates by id and releases the list.
	 */
	void afterUnmarshal(Unmarshaller u, Object parent) {
		for (PetTemplate pet : pets) {
			petData.put(pet.getId(), pet);
		}
		pets.clear();
		pets = null;
	}

	/**
	 * 返回已加载的宠物模板数量。
	 * Returns the number of loaded pet templates.
	 *
	 * template count
	 */
	public int size() {
		return petData.size();
	}

	/**
	 * 按宠物 ID 获取宠物模板。
	 * Returns the pet template for the given id.
	 *
	 * @param id 宠物 ID / pet id
	 * @return 宠物模板，不存在则为 null / pet template or null
	 */
	public PetTemplate getPetTemplate(int id) {
		return petData.get(id);
	}
}
