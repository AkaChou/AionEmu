package com.aionemu.gameserver.dataholders;

import java.util.List;

import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlTransient;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.model.templates.revive_start_points.InstanceReviveStartPoints;

import com.aionemu.commons.utils.collections.IntObjectHashMap;

/**
 * 副本复活起始点数据容器，按世界 ID 索引。
 * Instance revive start-point data holder, indexed by world id.
 *
 * Created by Wnkrz on 27/08/2017.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = { "InstanceStartPoints" })
@XmlRootElement(name = "instance_revive_start_points")
public class ReviveInstanceStartPointsData {
	@XmlElement(name = "instance_revive_start_point")
	protected List<InstanceReviveStartPoints> InstanceStartPoints;

	@XmlTransient
	private IntObjectHashMap<InstanceReviveStartPoints> custom = new IntObjectHashMap<InstanceReviveStartPoints>();

	/**
	 * 按世界 ID 获取副本复活起始点。
	 * Returns the instance revive start point for the given world id.
	 *
	 * @param worldId 世界 ID / world id
	 * @return 复活起始点，不存在则为 null / revive start point or null
	 */
	public InstanceReviveStartPoints getReviveStartPoint(int worldId) {
		return custom.get(worldId);
	}

	/**
	 * JAXB 反序列化完成后，将起始点写入世界 ID 索引。
	 * After JAXB unmarshalling, indexes start points by world id.
	 */
	void afterUnmarshal(Unmarshaller u, Object parent) {
		for (InstanceReviveStartPoints it : InstanceStartPoints) {
			getCustomMap().put(it.getReviveWorld(), it);
		}
	}

	private IntObjectHashMap<InstanceReviveStartPoints> getCustomMap() {
		return custom;
	}

	/**
	 * 返回已加载的复活起始点数量。
	 * Returns the number of loaded revive start points.
	 *
	 * @return 起始点数量 / start-point count
	 */
	public int size() {
		return custom.size();
	}
}
