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
 * Moltenus 活动时间表配置。
 * Moltenus event schedule configuration.
 *
 * @author Rinzler (Encom)
 */
@XmlRootElement(name = "moltenus_schedule")
@XmlAccessorType(XmlAccessType.FIELD)
public class MoltenusSchedule {
	/**
	 * Moltenus 列表。
	 * List of moltenuses.
	 */
	@XmlElement(name = "moltenus", required = true)
	private List<Moltenus> moltenussList;

	/**
	 * 获取 Moltenus 列表。
	 * Returns the moltenus list.
	 */
	public List<Moltenus> getMoltenussList() {
		return moltenussList;
	}

	/**
	 * 设置 Moltenus 列表。
	 * Sets the moltenus list.
	 */
	public void setFightsList(List<Moltenus> moltenusList) {
		this.moltenussList = moltenusList;
	}

	/**
	 * 从 XML 加载时间表。
	 * Loads the schedule from XML.
	 */
	public static MoltenusSchedule load() {
		MoltenusSchedule ms;
		try {
			String xml = Files.readString(Config.configFile("schedule/moltenus_schedule.xml").toPath(), StandardCharsets.UTF_8);
			ms = (MoltenusSchedule) JAXBUtil.deserialize(xml, MoltenusSchedule.class);
		} catch (Exception e) {
			throw new RuntimeException("Failed to initialize moltenus", e);
		}
		return ms;
	}

	/**
	 * 单个 Moltenus 的时间表条目。
	 * Schedule entry for a single moltenus.
	 */
	@XmlAccessorType(XmlAccessType.FIELD)
	@XmlRootElement(name = "moltenus")
	public static class Moltenus {
		/**
		 * 莫尔泰努斯 ID / Moltenus ID
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
		 * 获取 Moltenus ID。
		 * Returns the moltenus ID.
		 */
		public int getId() {
			return id;
		}

		/**
		 * 设置 Moltenus ID。
		 * Sets the moltenus ID.
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
