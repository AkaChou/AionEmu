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
 * Circus 马戏团活动时间表配置。
 * Circus event schedule configuration.
 *
 * @author Rinzler (Encom)
 */
@XmlRootElement(name = "circus_schedule")
@XmlAccessorType(XmlAccessType.FIELD)
public class CircusSchedule {
	/**
	 * Circus 列表。
	 * List of circuses.
	 */
	@XmlElement(name = "circus", required = true)
	private List<Circus> circussList;

	/**
	 * 获取 Circus 列表。
	 * Returns the circus list.
	 */
	public List<Circus> getCircussList() {
		return circussList;
	}

	/**
	 * 设置 Circus 列表。
	 * Sets the circus list.
	 */
	public void setCircussList(List<Circus> circusList) {
		this.circussList = circusList;
	}

	/**
	 * 从 XML 加载时间表。
	 * Loads the schedule from XML.
	 */
	public static CircusSchedule load() {
		CircusSchedule cs;
		try {
			String xml = Files.readString(Config.configFile("schedule/circus_schedule.xml").toPath(), StandardCharsets.UTF_8);
			cs = (CircusSchedule) JAXBUtil.deserialize(xml, CircusSchedule.class);
		} catch (Exception e) {
			throw new RuntimeException("Failed to initialize circus", e);
		}
		return cs;
	}

	/**
	 * 单个 Circus 的时间表条目。
	 * Schedule entry for a single circus.
	 */
	@XmlAccessorType(XmlAccessType.FIELD)
	@XmlRootElement(name = "circus")
	public static class Circus {
		/**
		 * 马戏团 ID / Circus ID
		 */
		@XmlAttribute(required = true)
		private int id;

		/**
		 * Circus 时间列表。
		 * List of circus times.
		 */
		@XmlElement(name = "circusTime", required = true)
		private List<String> circusTimes;

		/**
		 * 获取 Circus ID。
		 * Returns the circus ID.
		 */
		public int getId() {
			return id;
		}

		/**
		 * 设置 Circus ID。
		 * Sets the circus ID.
		 */
		public void setId(int id) {
			this.id = id;
		}

		/**
		 * 获取 Circus 时间列表。
		 * Returns the circus times.
		 */
		public List<String> getCircusTimes() {
			return circusTimes;
		}

		/**
		 * 设置 Circus 时间列表。
		 * Sets the circus times.
		 */
		public void setCircusTimes(List<String> circusTimes) {
			this.circusTimes = circusTimes;
		}
	}
}
