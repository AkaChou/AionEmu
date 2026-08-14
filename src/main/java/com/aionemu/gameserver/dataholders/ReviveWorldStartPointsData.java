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

import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.templates.revive_start_points.WorldReviveStartPoints;

/**
 * 大世界复活起始点数据容器，按世界、阵营与等级匹配。
 * World revive start-point data holder, matched by world, race and level.
 *
 * Created by Wnkrz on 22/08/2017.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = { "WorldStartPoints" })
@XmlRootElement(name = "revive_world_start_points")
public class ReviveWorldStartPointsData {
	@XmlElement(name = "revive_world_start_point")
	protected List<WorldReviveStartPoints> WorldStartPoints;

	@XmlTransient
	protected List<WorldReviveStartPoints> StartPointsList = new ArrayList<WorldReviveStartPoints>();

	/**
	 * JAXB 反序列化完成后，将起始点复制到运行时列表并释放 XML 列表。
	 * After JAXB unmarshalling, copies start points into the runtime list and releases the XML list.
	 */
	void afterUnmarshal(Unmarshaller unmarshaller, Object parent) {
		for (WorldReviveStartPoints exit : WorldStartPoints) {
			StartPointsList.add(exit);
		}
		WorldStartPoints.clear();
		WorldStartPoints = null;
	}

	/**
	 * 按世界、阵营与玩家等级查找匹配的复活起始点。
	 * Finds a matching revive start point by world, race and player level.
	 *
	 * @param worldId 世界 ID / world id
	 * @param race 阵营 / race
	 * @param playerLevel 玩家等级 / player level
	 * @return 复活起始点，不匹配则为 null / revive start point or null
	 */
	public WorldReviveStartPoints getReviveStartPoint(int worldId, Race race, int playerLevel) {
		for (WorldReviveStartPoints revive : StartPointsList) {
			if (revive.getReviveWorld() == worldId
					&& (race.equals(revive.getRace()) || revive.getRace().equals(Race.PC_ALL))
					&& playerLevel >= revive.getMinlevel() && playerLevel <= revive.getMaxlevel()) {
				return revive;
			}
		}
		return null;
	}

	/**
	 * 返回已加载的复活起始点数量。
	 * Returns the number of loaded revive start points.
	 *
	 * @return 起始点数量 / start-point count
	 */
	public int size() {
		return StartPointsList.size();
	}
}
