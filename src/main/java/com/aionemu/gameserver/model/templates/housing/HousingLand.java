package com.aionemu.gameserver.model.templates.housing;

import java.util.List;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElementWrapper;
import jakarta.xml.bind.annotation.XmlType;

/**
 * 住房 Land 模板（静态数据/XML）。
 * XML template.
 *
 * @author Rolandas
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Land", propOrder = { "addresses", "buildings", "sale", "fee", "caps" })
public class HousingLand {

	@XmlElementWrapper(name = "addresses", required = true)
	@XmlElement(name = "address")
	protected List<HouseAddress> addresses;

	@XmlElementWrapper(name = "buildings", required = true)
	@XmlElement(name = "building")
	protected List<Building> buildings;

	@XmlElement(required = true)
	protected Sale sale;

	@XmlElement(required = true)
	protected long fee;

	@XmlElement(required = true)
	protected BuildingCapabilities caps;

	@XmlAttribute(name = "sign_nosale", required = true)
	protected int signNosale;

	@XmlAttribute(name = "sign_sale", required = true)
	protected int signSale;

	@XmlAttribute(name = "sign_waiting", required = true)
	protected int signWaiting;

	@XmlAttribute(name = "sign_home", required = true)
	protected int signHome;

	@XmlAttribute(name = "manager_npc", required = true)
	protected int managerNpc;

	@XmlAttribute(name = "teleport_npc", required = true)
	protected int teleportNpc;

	@XmlAttribute(required = true)
	protected int id;

	/** 返回 addresses / Returns the addresses */
	public List<HouseAddress> getAddresses() {
		return addresses;
	}

	/** 返回 buildings / Returns the buildings */
	public List<Building> getBuildings() {
		return buildings;
	}

	/** 返回 default building / Returns the default building */
	public Building getDefaultBuilding() {
		for (Building building : buildings) {
			if (building.isDefault()) {
				return building;
			}
		}
		return buildings.get(0); // 兜底返回第一个建筑 / fallback to the first building
	}

	/** 返回 sale options / Returns the sale options */
	public Sale getSaleOptions() {
		return sale;
	}

	/** 返回 maintenance fee / Returns the maintenance fee */
	public long getMaintenanceFee() {
		return fee;
	}

	/** 返回 capabilities / Returns the capabilities */
	public BuildingCapabilities getCapabilities() {
		return caps;
	}

	/** 返回 nosale sign npc id / Returns the nosale sign npc id */
	public int getNosaleSignNpcId() {
		return signNosale;
	}

	/** 返回 sale sign npc id / Returns the sale sign npc id */
	public int getSaleSignNpcId() {
		return signSale;
	}

	/** 设置 sign sale / Sets the sign sale */
	public void setSignSale(int value) {
		this.signSale = value;
	}

	/** 返回 waiting sign npc id / Returns the waiting sign npc id */
	public int getWaitingSignNpcId() {
		return signWaiting;
	}

	/** 返回 home sign npc id / Returns the home sign npc id */
	public int getHomeSignNpcId() {
		return signHome;
	}

	/** 返回 manager npc id / Returns the manager npc id */
	public int getManagerNpcId() {
		return managerNpc;
	}

	/** 返回传送 NPCID / Returns the teleport npc id */
	public int getTeleportNpcId() {
		return teleportNpc;
	}

	/** 返回 ID / Returns the id */
	public int getId() {
		return id;
	}

	/** 返回哈希码。 / Returns hash code. */
	@Override
	public int hashCode() {
		return id;
	}
}
