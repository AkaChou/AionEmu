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
 * Siege 要塞攻城时间表配置。
 * Siege fortress schedule configuration.
 */
@XmlRootElement(name = "siege_schedule")
@XmlAccessorType(XmlAccessType.FIELD)
public class SiegeSchedule {
	/**
	 * 要塞列表。
	 * List of fortresses.
	 */
	@XmlElement(name = "fortress", required = true)
	private List<Fortress> fortressesList;

	/**
	 * 获取要塞列表。
	 * Returns the fortress list.
	 */
	public List<Fortress> getFortressesList() {
		return fortressesList;
	}

	/**
	 * 设置要塞列表。
	 * Sets the fortress list.
	 */
	public void setFortressesList(List<Fortress> fortressList) {
		this.fortressesList = fortressList;
	}

	/**
	 * 从 XML 加载时间表。
	 * Loads the schedule from XML.
	 */
	public static SiegeSchedule load() {
		SiegeSchedule ss;
		try {
			String xml = Files.readString(Config.configFile("schedule/siege_schedule.xml").toPath(), StandardCharsets.UTF_8);
			ss = (SiegeSchedule) JAXBUtil.deserialize(xml, SiegeSchedule.class);
		} catch (Exception e) {
			throw new RuntimeException("Failed to initialize sieges", e);
		}
		return ss;
	}

	/**
	 * 单个要塞的时间表条目。
	 * Schedule entry for a single fortress.
	 */
	@XmlAccessorType(XmlAccessType.FIELD)
	@XmlRootElement(name = "fortress")
	public static class Fortress {

		/**
		 * 要塞 ID。
		 * Fortress ID.
		 */
		@XmlAttribute(required = true)
		private int id;

		/**
		 * 攻城时间列表。
		 * List of siege times.
		 */
		@XmlElement(name = "siegeTime", required = true)
		private List<String> siegeTimes;

		/**
		 * 获取要塞 ID。
		 * Returns the fortress ID.
		 */
		public int getId() {
			return id;
		}

		/**
		 * 设置要塞 ID。
		 * Sets the fortress ID.
		 */
		public void setId(int id) {
			this.id = id;
		}

		/**
		 * 获取攻城时间列表。
		 * Returns the siege times.
		 */
		public List<String> getSiegeTimes() {
			return siegeTimes;
		}

		/**
		 * 设置攻城时间列表。
		 * Sets the siege times.
		 */
		public void setSiegeTimes(List<String> siegeTimes) {
			this.siegeTimes = siegeTimes;
		}
	}
}
