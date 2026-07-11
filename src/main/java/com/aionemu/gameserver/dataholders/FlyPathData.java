package com.aionemu.gameserver.dataholders;

import java.util.List;

import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

import com.aionemu.gameserver.model.templates.flypath.FlyPathEntry;

import com.aionemu.commons.utils.collections.ShortObjectHashMap;

/**
 * 飞行路径配置数据容器，按路径 ID 索引飞行路径条目。
 * Fly path configuration data holder, indexed by path id.
 *
 * @author KID
 */
@XmlRootElement(name = "flypath_template")
@XmlAccessorType(XmlAccessType.FIELD)
public class FlyPathData {
	@XmlElement(name = "flypath_location")
	private List<FlyPathEntry> list;

	private ShortObjectHashMap<FlyPathEntry> loctlistData = new ShortObjectHashMap<FlyPathEntry>();

	/**
	 * JAXB 反序列化完成后，将飞行路径写入 ID 索引。
	 * After JAXB unmarshalling, indexes fly path entries by id.
	 */
	void afterUnmarshal(Unmarshaller u, Object parent) {
		for (FlyPathEntry loc : list) {
			loctlistData.put(loc.getId(), loc);
		}
	}

	/**
	 * 返回飞行路径条目数量。
	 * Returns the number of fly path entries.
	 *
	 * path count
	 */
	public int size() {
		return loctlistData.size();
	}

	/**
	 * 按路径 ID 获取飞行路径模板。
	 * Returns the fly path template for the given path id.
	 *
	 * @param i 路径 ID / path id
	 * @return 飞行路径条目，不存在则为 null / fly path entry, or null if absent
	 */
	public FlyPathEntry getPathTemplate(byte i) {
		return loctlistData.get((short) i);
	}
}
