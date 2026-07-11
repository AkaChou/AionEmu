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
 * Vortex 漩涡入侵活动时间表配置。
 * Vortex invasion event schedule configuration.
 *
 * @author Rinzler (Encom)
 */
@XmlRootElement(name = "vortex_schedule")
@XmlAccessorType(XmlAccessType.FIELD)
public class VortexSchedule {
	/**
	 * Vortex 列表。
	 * List of vortexes.
	 */
	@XmlElement(name = "vortex", required = true)
	private List<Vortex> vortexsList;

	/**
	 * 获取 Vortex 列表。
	 * Returns the vortex list.
	 */
	public List<Vortex> getVortexsList() {
		return vortexsList;
	}

	/**
	 * 设置 Vortex 列表。
	 * Sets the vortex list.
	 */
	public void setInvasionsList(List<Vortex> vortexList) {
		this.vortexsList = vortexList;
	}

	/**
	 * 从 XML 加载时间表。
	 * Loads the schedule from XML.
	 */
	public static VortexSchedule load() {
		VortexSchedule vs;
		try {
			String xml = Files.readString(Config.configFile("schedule/vortex_schedule.xml").toPath(), StandardCharsets.UTF_8);
			vs = (VortexSchedule) JAXBUtil.deserialize(xml, VortexSchedule.class);
		} catch (Exception e) {
			throw new RuntimeException("Failed to initialize vortex", e);
		}
		return vs;
	}

	/**
	 * 单个 Vortex 的时间表条目。
	 * Schedule entry for a single vortex.
	 */
	@XmlAccessorType(XmlAccessType.FIELD)
	@XmlRootElement(name = "vortex")
	public static class Vortex {
		/**
	 * 漩涡 ID / Vortex ID
	 */
		@XmlAttribute(required = true)
		private int id;

		/**
		 * 入侵时间列表。
		 * List of invasion times.
		 */
		@XmlElement(name = "invasionTime", required = true)
		private List<String> invasionTimes;

		/**
		 * 获取 Vortex ID。
		 * Returns the vortex ID.
		 */
		public int getId() {
			return id;
		}

		/**
		 * 设置 Vortex ID。
		 * Sets the vortex ID.
		 */
		public void setId(int id) {
			this.id = id;
		}

		/**
		 * 获取入侵时间列表。
		 * Returns the invasion times.
		 */
		public List<String> getInvasionTimes() {
			return invasionTimes;
		}

		/**
		 * 设置入侵时间列表。
		 * Sets the invasion times.
		 */
		public void setInvasionTimes(List<String> invasionTimes) {
			this.invasionTimes = invasionTimes;
		}
	}
}
