package com.aionemu.gameserver.dataholders;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

import com.aionemu.gameserver.model.templates.event.AtreianPassport;

import com.aionemu.commons.utils.collections.IntObjectHashMap;

/**
 * 阿特里亚通行证数据容器，按 ID 索引通行证模板。
 * Atreian passport data holder, indexing passport templates by id.
 *
 * @author Alcapwnd
 */
@XmlRootElement(name = "atreian_passports")
@XmlAccessorType(XmlAccessType.FIELD)
public class AtreianPassportData {

	@XmlElement(name = "atreian_passport")
	private List<AtreianPassport> tlist;
	/** 通行证模板索引 / passport template index */
	private IntObjectHashMap<AtreianPassport> passportData = new IntObjectHashMap<AtreianPassport>();
	private Map<Integer, AtreianPassport> passportDataMap = new HashMap<Integer, AtreianPassport>(1);

	/**
	 * JAXB 反序列化完成后，将列表写入双索引映射。
	 * After JAXB unmarshalling, populates both index maps from the list.
	 */
	void afterUnmarshal(Unmarshaller u, Object parent) {
		for (AtreianPassport id : tlist) {
			passportData.put(id.getId(), id);
			passportDataMap.put(id.getId(), id);
		}
	}

	/**
	 * 返回已加载的通行证数量。
	 * Returns the number of loaded passports.
	 *
	 * @return 已加载的通行证数量 / Returns the number of loaded passports.
	 */
	public int size() {
		return passportData.size();
	}

	/**
	 * 按 ID 获取通行证模板。
	 * Returns the passport template for the given id.
	 *
	 * @param id 通行证 ID / passport id
	 * @return 模板，不存在则为 null / template or null
	 */
	public AtreianPassport getAtreianPassportId(int id) {
		return passportData.get(id);
	}

	/**
	 * 返回全部通行证映射。
	 * Returns the full passport map.
	 *
	 * @return ID 到模板的映射 / map of id to template
	 */
	public Map<Integer, AtreianPassport> getAll() {
		return passportDataMap;
	}
}
