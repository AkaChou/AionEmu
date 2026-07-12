package com.aionemu.gameserver.dataholders;

import java.util.List;

import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlTransient;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.model.templates.robot.RobotInfo;

import com.aionemu.commons.utils.collections.IntObjectHashMap;

/**
 * 机器人（变形机体）数据容器，按机器人 ID 索引 RobotInfo。
 * Robot (transformation mech) data holder, indexing RobotInfo by robot id.
 *
 * @author Rinzler (Encom)
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = { "robots" })
@XmlRootElement(name = "robots")
public class RobotData {
	@XmlElement(name = "robot_info")
	private List<RobotInfo> robots;

	@XmlTransient
	private IntObjectHashMap<RobotInfo> robotInfos;

	/**
	 * JAXB 反序列化完成后，将机器人信息写入 ID 索引并释放列表。
	 * After JAXB unmarshalling, indexes robot info by id and releases the list.
	 */
	void afterUnmarshal(Unmarshaller u, Object parent) {
		robotInfos = new IntObjectHashMap<RobotInfo>();
		for (RobotInfo info : robots) {
			robotInfos.put(info.getRobotId(), info);
		}
		robots.clear();
		robots = null;
	}

	/**
	 * 按机器人/NPC ID 获取机器人信息。
	 * Returns the robot info for the given robot/NPC id.
	 *
	 * robot or npc id
	 *
	 * @param npcId
	 * @return 机器人信息，不存在则为 null / robot info or null
	 */
	public RobotInfo getRobotInfo(int npcId) {
		return (RobotInfo) robotInfos.get(npcId);
	}

	/**
	 * 返回已加载的机器人数量。
	 * Returns the number of loaded robots.
	 *
	 * @return 机器人数量 / robot count
	 */
	public int size() {
		return robotInfos.size();
	}
}
