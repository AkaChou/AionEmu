package com.aionemu.gameserver.model.templates.spawns.agentspawns;

import java.util.List;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.model.agent.AgentStateType;
import com.aionemu.gameserver.model.templates.spawns.Spawn;

/**
 * 代理人刷新点模板（静态数据/XML）。
 * XML template.
 *
 * @author Rinzler (Encom)
 */

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "AgentSpawn")
public class AgentSpawn {
	@XmlAttribute(name = "id")
	private int id;

	/** 返回 ID / Returns the id */
	public int getId() {
		return id;
	}

	@XmlElement(name = "agent_type")
	private List<AgentSpawn.AgentStateTemplate> AgentStateTemplate;

	/** 返回 siege mod templates / Returns the siege mod templates */
	public List<AgentStateTemplate> getSiegeModTemplates() {
		return AgentStateTemplate;
	}

	@XmlAccessorType(XmlAccessType.FIELD)
	@XmlType(name = "AgentStateTemplate")
	public static class AgentStateTemplate {

		@XmlElement(name = "spawn")
		private List<Spawn> spawns;

		@XmlAttribute(name = "astate")
		private AgentStateType agentType;

		/** 获取刷新。 / Returns the spawns. */
		public List<Spawn> getSpawns() {
			return spawns;
		}

		/** 获取代理人类型。 / Returns the agent type. */
		public AgentStateType getAgentType() {
			return agentType;
		}
	}
}
