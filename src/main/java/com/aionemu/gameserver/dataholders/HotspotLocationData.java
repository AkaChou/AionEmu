package com.aionemu.gameserver.dataholders;

import java.io.File;
import java.util.List;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

import com.aionemu.commons.utils.collections.IntObjectHashMap;
import com.aionemu.gameserver.model.templates.teleport.HotspotlocationTemplate;

/**
 * 热点传送位置配置数据容器，按地点 ID 索引热点模板。
 * Hotspot location configuration data holder, indexed by location id.
 *
 * @author Rinzler (Encom)
 */
@XmlRootElement(name = "hotspot_location")
@XmlAccessorType(XmlAccessType.FIELD)
public class HotspotLocationData {
	@XmlElement(name = "hotspot_template")
	private List<HotspotlocationTemplate> hslist;

	private IntObjectHashMap<HotspotlocationTemplate> lochslistData = new IntObjectHashMap<HotspotlocationTemplate>();

	/**
	 * 从 compact 定义文件加载热点位置。
	 * Loads hotspot locations from a compact definition file.
	 */
	public static HotspotLocationData load(File file) {
		if (file == null || !file.isFile()) {
			throw new IllegalStateException("Hotspot location file not found: " + file);
		}
		try {
			JAXBContext context = JAXBContext.newInstance(HotspotLocationData.class);
			return (HotspotLocationData) context.createUnmarshaller().unmarshal(file);
		} catch (Exception e) {
			throw new IllegalStateException("Failed to load hotspot locations from " + file.getPath(), e);
		}
	}

	/**
	 * JAXB 反序列化完成后，将热点位置写入 ID 索引。
	 * After JAXB unmarshalling, indexes hotspot locations by id.
	 */
	void afterUnmarshal(Unmarshaller u, Object parent) {
		if (hslist == null) {
			return;
		}
		for (HotspotlocationTemplate loc : hslist) {
			lochslistData.put(loc.getLocId(), loc);
		}
	}

	/**
	 * 返回热点位置模板数量。
	 * Returns the number of hotspot location templates.
	 */
	public int size() {
		return lochslistData.size();
	}

	/**
	 * 按 ID 获取热点位置模板。
	 * Returns the hotspot location template for the given id.
	 *
	 * @param id 地点 ID / location id
	 * @return 热点位置模板，不存在则为 null / hotspot location template, or null if absent
	 */
	public HotspotlocationTemplate getHotspotlocationTemplate(int id) {
		return lochslistData.get(id);
	}
}
