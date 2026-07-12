package com.aionemu.gameserver.dataholders;

import java.util.List;

import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlTransient;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.model.templates.pet.PetBonusAttr;

import com.aionemu.commons.utils.collections.IntObjectHashMap;

/**
 * 宠物增益属性数据容器，按 buffId 与食物计数双键索引。
 * Pet bonus attribute data holder, dual-keyed by buff id and food count.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = { "petBonusattr" })
@XmlRootElement(name = "pet_bonusattrs")
public class PetBuffData {

	@XmlElement(name = "pet_bonusattr")
	protected List<PetBonusAttr> petBonusattr;

	@XmlTransient
	private IntObjectHashMap<PetBonusAttr> templates = new IntObjectHashMap<PetBonusAttr>();

	/**
	 * JAXB 反序列化完成后，按 buffId 与 foodCount 写入索引并释放列表。
	 * After JAXB unmarshalling, indexes by buff id and food count, then releases the list.
	 */
	void afterUnmarshal(Unmarshaller u, Object parent) {
		for (PetBonusAttr template : petBonusattr) {
			templates.put(template.getBuffId(), template);
			templates.put(template.getFoodCount(), template);
		}
		petBonusattr.clear();
		petBonusattr = null;
	}

	/**
	 * 返回索引条目数量（含双键）。
	 * Returns the number of index entries (including dual keys).
	 *
	 * entry count
	 */
	public int size() {
		return templates.size();
	}

	/**
	 * 按增益 ID 获取宠物增益属性。
	 * Returns the pet bonus attribute for the given buff id.
	 *
	 * buff id
	 *
	 * @param buffId
	 * @return 增益属性，不存在则为 null / bonus attribute or null
	 */
	public PetBonusAttr getPetBonusattr(int buffId) {
		return templates.get(buffId);
	}

	/**
	 * 按食物计数获取宠物增益属性。
	 * Returns the pet bonus attribute for the given food count.
	 *
	 * @param count 食物计数 / food count
	 * @return 增益属性，不存在则为 null / bonus attribute or null
	 */
	public PetBonusAttr getFoodCount(int count) {
		return templates.get(count);
	}
}
