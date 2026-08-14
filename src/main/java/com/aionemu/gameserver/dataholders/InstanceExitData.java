package com.aionemu.gameserver.dataholders;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlTransient;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.templates.portal.InstanceExit;

/**
 * 副本出口数据容器，按世界 ID 与种族索引出口点。
 * Instance exit data holder, indexing exit points by world id and race.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = { "instanceExit" })
@XmlRootElement(name = "instance_exits")
public class InstanceExitData {

	@XmlElement(name = "instance_exit")
	protected List<InstanceExit> instanceExit;

	@XmlTransient
	protected List<InstanceExit> instanceExits = new ArrayList<InstanceExit>();
	@XmlTransient
	private Map<Integer, List<InstanceExit>> exitsByWorldId = new HashMap<Integer, List<InstanceExit>>();

	/**
	 * JAXB 反序列化完成后，建立世界 ID 索引并释放原始列表。
	 * After JAXB unmarshalling, builds the world-id index and clears the raw list.
	 */
	void afterUnmarshal(Unmarshaller unmarshaller, Object parent) {
		for (InstanceExit exit : instanceExit) {
			instanceExits.add(exit);
			exitsByWorldId.computeIfAbsent(exit.getInstanceId(), k -> new ArrayList<InstanceExit>()).add(exit);
		}
		instanceExit.clear();
		instanceExit = null;
	}

	/**
	 * 按世界 ID 与种族获取匹配的副本出口。
	 * Returns the matching instance exit for the given world id and race.
	 *
	 * @param worldId 副本 ID / world / instance id。
	 * @param race 玩家种族 / player race
	 * @return 匹配的出口，不存在则为 null / matching exit or null
	 */
	public InstanceExit getInstanceExit(int worldId, Race race) {
		List<InstanceExit> exits = exitsByWorldId.get(worldId);
		if (exits == null) {
			return null;
		}
		for (InstanceExit exit : exits) {
			if (race.equals(exit.getRace()) || exit.getRace().equals(Race.PC_ALL)) {
				return exit;
			}
		}
		return null;
	}

	/**
	 * 返回已加载的副本出口数量。
	 * Returns the number of loaded instance exits.
	 *
	 * @return 已加载的副本出口数量 / Returns the number of loaded instance exits.
	 */
	public int size() {
		return instanceExits.size();
	}
}
