package com.aionemu.gameserver.dataholders;

import java.util.List;

import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlTransient;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.model.templates.portal.PortalLoc;

import com.aionemu.commons.utils.collections.IntObjectHashMap;

/**
 * 传送门坐标数据容器，按坐标 ID 索引 PortalLoc。
 * Portal location data holder, indexing PortalLoc by location id.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = { "portalLoc" })
@XmlRootElement(name = "portal_locs")
public class PortalLocData {

	@XmlElement(name = "portal_loc")
	protected List<PortalLoc> portalLoc;

	@XmlTransient
	private IntObjectHashMap<PortalLoc> portalLocs = new IntObjectHashMap<PortalLoc>();

	/**
	 * JAXB 反序列化完成后，将坐标写入 ID 索引并释放列表。
	 * After JAXB unmarshalling, indexes locations by id and releases the list.
	 */
	void afterUnmarshal(Unmarshaller unmarshaller, Object parent) {
		for (PortalLoc loc : portalLoc) {
			portalLocs.put(loc.getLocId(), loc);
		}

		portalLoc.clear();
		portalLoc = null;
	}

	/**
	 * 返回已加载的传送坐标数量。
	 * Returns the number of loaded portal locations.
	 *
	 * location count
	 */
	public int size() {
		return portalLocs.size();
	}

	/**
	 * 按坐标 ID 获取传送门坐标。
	 * Returns the portal location for the given location id.
	 *
	 * location id
	 *
	 * @param locId @return 传送坐标，不存在则为 null / portal location or null
	 */
	public PortalLoc getPortalLoc(int locId) {
		return portalLocs.get(locId);
	}
}
