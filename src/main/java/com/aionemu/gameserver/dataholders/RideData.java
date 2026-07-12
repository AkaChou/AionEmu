package com.aionemu.gameserver.dataholders;

import java.util.List;

import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlTransient;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.model.templates.ride.RideInfo;

import com.aionemu.commons.utils.collections.IntObjectHashMap;

/**
 * 坐骑数据容器，按 NPC ID 索引 RideInfo。
 * Ride data holder, indexing RideInfo by NPC id.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = { "rides" })
@XmlRootElement(name = "rides")
public class RideData {

	@XmlElement(name = "ride_info")
	private List<RideInfo> rides;

	@XmlTransient
	private IntObjectHashMap<RideInfo> rideInfos;

	/**
	 * JAXB 反序列化完成后，将坐骑信息写入 NPC ID 索引并释放列表。
	 * After JAXB unmarshalling, indexes ride info by NPC id and releases the list.
	 */
	void afterUnmarshal(Unmarshaller u, Object parent) {
		rideInfos = new IntObjectHashMap<RideInfo>();

		for (RideInfo info : rides) {
			rideInfos.put(info.getNpcId(), info);
		}
		rides.clear();
		rides = null;
	}

	/**
	 * 按 NPC ID 获取坐骑信息。
	 * Returns the ride info for the given NPC id.
	 *
	 * npc id
	 *
	 * @param npcId
	 * @return 坐骑信息，不存在则为 null / ride info or null
	 */
	public RideInfo getRideInfo(int npcId) {
		return (RideInfo) rideInfos.get(npcId);
	}

	/**
	 * 返回已加载的坐骑数量。
	 * Returns the number of loaded rides.
	 *
	 * ride count
	 */
	public int size() {
		return rideInfos.size();
	}
}
