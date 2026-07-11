package com.aionemu.gameserver.configs.schedule;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

import com.aionemu.commons.utils.xml.JAXBUtil;
import com.aionemu.gameserver.configs.Config;

/**
 * Agent 活动时间表配置。
 * Agent event schedule configuration.
 *
 * @author Rinzler (Encom)
 */
@XmlRootElement(name = "agent_schedule")
@XmlAccessorType(XmlAccessType.FIELD)
public class AgentSchedule {
	/**
	 * Agent 列表。
	 * List of agents.
	 */
	@XmlElement(name = "agent", required = true)
	private List<Agent> agentsList;

	/**
	 * 获取 Agent 列表。
	 * Returns the agent list.
	 */
	public List<Agent> getAgentsList() {
		return agentsList;
	}

	/**
	 * 设置 Agent 列表。
	 * Sets the agent list.
	 */
	public void setFightsList(List<Agent> agentList) {
		this.agentsList = agentList;
	}

	/**
	 * 从 XML 加载时间表。
	 * Loads the schedule from XML.
	 */
	public static AgentSchedule load() {
		AgentSchedule as;
		try {
			String xml = Files.readString(Config.configFile("schedule/agent_schedule.xml").toPath(), StandardCharsets.UTF_8);
			as = (AgentSchedule) JAXBUtil.deserialize(xml, AgentSchedule.class);
		} catch (Exception e) {
			throw new RuntimeException("Failed to initialize agent", e);
		}
		return as;
	}

	/**
	 * 单个 Agent 的时间表条目。
	 * Schedule entry for a single agent.
	 */
	@XmlAccessorType(XmlAccessType.FIELD)
	@XmlRootElement(name = "agent")
	public static class Agent {
		/**
	 * 代理人 ID / Agent ID
	 */
		@XmlAttribute(required = true)
		private int id;

		/**
		 * 战斗时间列表。
		 * List of fight times.
		 */
		@XmlElement(name = "fightTime", required = true)
		private List<String> fightTimes;

		/**
		 * 获取 Agent ID。
		 * Returns the agent ID.
		 */
		public int getId() {
			return id;
		}

		/**
		 * 设置 Agent ID。
		 * Sets the agent ID.
		 */
		public void setId(int id) {
			this.id = id;
		}

		/**
		 * 获取战斗时间列表。
		 * Returns the fight times.
		 */
		public List<String> getFightTimes() {
			return fightTimes;
		}

		/**
		 * 设置战斗时间列表。
		 * Sets the fight times.
		 */
		public void setFightTimes(List<String> fightTimes) {
			this.fightTimes = fightTimes;
		}
	}
}
