package com.aionemu.gameserver.dataholders;

import java.util.List;

import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlTransient;

import com.aionemu.gameserver.model.agent.AgentLocation;
import com.aionemu.gameserver.model.templates.agent.AgentTemplate;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 代理战斗地点数据容器，持有并索引全部 Agent 地点。
 * Agent fight location data holder, indexing all agent locations.
 *
 * @author Rinzler (Encom)
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "agent_fight")
public class AgentData {
	@XmlElement(name = "agent_location")
	private List<AgentTemplate> agentTemplates;

	@XmlTransient
	private Map<Integer, AgentLocation> agent = new LinkedHashMap<Integer, AgentLocation>();

	/**
	 * JAXB 反序列化完成后，将模板转为运行时地点并按 ID 索引。
	 * After JAXB unmarshalling, converts templates to runtime locations indexed by id.
	 */
	void afterUnmarshal(Unmarshaller u, Object parent) {
		for (AgentTemplate template : agentTemplates) {
			agent.put(template.getId(), new AgentLocation(template));
		}
	}

	/**
	 * 返回已加载的地点数量。
	 * Returns the number of loaded locations.
	 *
	 * @return 已加载的地点数量 / Returns the number of loaded locations.
	 */
	public int size() {
		return agent.size();
	}

	/**
	 * 返回全部 Agent 地点映射。
	 * Returns the full agent location map.
	 *
	 * @return ID 到地点的映射 / map of id to location
	 */
	public Map<Integer, AgentLocation> getAgentLocations() {
		return agent;
	}
}
