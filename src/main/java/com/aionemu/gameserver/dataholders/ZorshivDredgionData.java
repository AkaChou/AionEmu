package com.aionemu.gameserver.dataholders;

import java.util.List;

import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlTransient;

import com.aionemu.gameserver.model.templates.zorshivdredgion.ZorshivDredgionTemplate;
import com.aionemu.gameserver.model.zorshivdredgion.ZorshivDredgionLocation;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 佐尔希夫无舰数据容器，按位置 ID 索引运行时地点。
 * Zorshiv Dredgion data holder, indexing runtime locations by location id.
 *
 * @author Rinzler (Encom)
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "zorshiv_dredgion")
public class ZorshivDredgionData {
	@XmlElement(name = "zorshiv_location")
	private List<ZorshivDredgionTemplate> zorshivDredgionTemplates;

	@XmlTransient
	private Map<Integer, ZorshivDredgionLocation> zorshivDredgion = new LinkedHashMap<Integer, ZorshivDredgionLocation>();

	/**
	 * JAXB 反序列化完成后，将模板转为运行时地点并按 ID 建索引。
	 * After JAXB unmarshalling, converts templates to runtime locations and indexes them by id.
	 */
	void afterUnmarshal(Unmarshaller u, Object parent) {
		for (ZorshivDredgionTemplate template : zorshivDredgionTemplates) {
			zorshivDredgion.put(template.getId(), new ZorshivDredgionLocation(template));
		}
	}

	/**
	 * 返回已加载的佐尔希夫龙舰地点数量。
	 * Returns the number of loaded Zorshiv Dredgion locations.
	 *
	 * location count
	 */
	public int size() {
		return zorshivDredgion.size();
	}

	/**
	 * 返回全部佐尔希夫龙舰地点映射。
	 * Returns the full Zorshiv Dredgion location map.
	 *
	 * location map
	 */
	public Map<Integer, ZorshivDredgionLocation> getZorshivDredgionLocations() {
		return zorshivDredgion;
	}
}
