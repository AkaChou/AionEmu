package com.aionemu.gameserver.dataholders;

import java.util.ArrayList;
import java.util.List;

import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlTransient;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.model.templates.spawns.HouseSpawn;
import com.aionemu.gameserver.model.templates.spawns.HouseSpawns;

import com.aionemu.commons.utils.collections.IntObjectHashMap;

/**
 * 房屋 NPC 刷怪配置数据容器，按地址索引房屋刷怪点。
 * House NPC spawn configuration data holder, indexed by house address.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = { "houseSpawnsData" })
@XmlRootElement(name = "house_npcs")
public class HouseNpcsData {

	@XmlElement(name = "house")
	protected List<HouseSpawns> houseSpawnsData;

	@XmlTransient
	private IntObjectHashMap<List<HouseSpawn>> houseSpawnsByAddressId = new IntObjectHashMap<List<HouseSpawn>>();

	/**
	 * 返回房屋刷怪配置列表；若尚未初始化则创建空列表。
	 * Returns the house spawn configuration list; creates an empty list if not yet initialized.
	 *
	 * @return 房屋刷怪列表 / house spawns list
	 */
	public List<HouseSpawns> getHouseSpawns() {
		if (houseSpawnsData == null) {
			houseSpawnsData = new ArrayList<HouseSpawns>();
		}
		return houseSpawnsData;
	}

	/**
	 * JAXB 反序列化完成后，按房屋地址建立刷怪点索引。
	 * After JAXB unmarshalling, indexes spawn points by house address.
	 */
	void afterUnmarshal(Unmarshaller u, Object parent) {
		for (HouseSpawns houseSpawns : getHouseSpawns()) {
			houseSpawnsByAddressId.put(houseSpawns.getAddress(), houseSpawns.getSpawns());
		}
	}

	/**
	 * 按房屋地址获取刷怪点列表。
	 * Returns the spawn point list for the given house address.
	 *
	 * @param address 房屋地址 / house address
	 * @return 刷怪点列表，不存在则为 null / spawn list, or null if absent
	 */
	public List<HouseSpawn> getSpawnsByAddress(int address) {
		return houseSpawnsByAddressId.get(address);
	}

	/**
	 * 返回估算的刷怪点总数（地址数 × 3）。
	 * Returns the estimated total spawn count (address count × 3).
	 *
	 * @return 估算刷怪点数量 / estimated spawn count
	 */
	public int size() {
		return houseSpawnsByAddressId.size() * 3;
	}
}
