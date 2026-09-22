package com.aionemu.gameserver.model.templates.spawns.agentspawns;

import java.util.List;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.model.agent.AgentStateType;
import com.aionemu.gameserver.model.templates.spawns.Spawn;
import lombok.Getter;

/**
 * 代理人刷新点模板（静态数据/XML）。
 * XML template.
 *
 * @author Rinzler (Encom)
 */

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "AgentSpawn")
public class AgentSpawn {
	/**
	 * -- GETTER --
	 * 返回 ID / Returns the id
	 */
	@Getter
	@XmlAttribute(name = "id")
	private int id;

	/**
	 * 代理状态模板列表。
	 * Agent state template list.
	 */
	@XmlElement(name = "agent_type")
	private List<AgentSpawn.AgentStateTemplate> AgentStateTemplate;

	/** 返回代理状态模板列表 / Returns the agent state templates */
	public List<AgentStateTemplate> getSiegeModTemplates() {
		return AgentStateTemplate;
	}

	/**
	 * 代理状态模板：绑定状态类型与对应的刷新列表。
	 * Agent state template: binds a state type with its spawn list.
	 */
	@Getter
    @XmlAccessorType(XmlAccessType.FIELD)
	@XmlType(name = "AgentStateTemplate")
	public static class AgentStateTemplate {

		/** 刷新列表。 / Spawn list.
         * -- GETTER --
         * 获取刷新。 / Returns the spawns.
         */
		@XmlElement(name = "spawn")
		private List<Spawn> spawns;

		/** 代理状态类型。 / Agent state type.
         * -- GETTER --
         * 获取代理人类型。 / Returns the agent type.
         */
		@XmlAttribute(name = "astate")
		private AgentStateType agentType;

    }
}
