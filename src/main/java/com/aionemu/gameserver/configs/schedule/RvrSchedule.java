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
 * RvR（种族对战）活动时间表配置。
 * RvR (Race vs Race) event schedule configuration.
 *
 * @author Rinzler (Encom)
 */
@XmlRootElement(name = "rvr_schedule")
@XmlAccessorType(XmlAccessType.FIELD)
public class RvrSchedule {
	/**
	 * RvR 列表。
	 * List of RvRs.
	 */
	@XmlElement(name = "rvr", required = true)
	private List<Rvr> rvrsList;

	/**
	 * 获取 RvR 列表。
	 * Returns the RvR list.
	 */
	public List<Rvr> getRvrsList() {
		return rvrsList;
	}

	/**
	 * 设置 RvR 列表。
	 * Sets the RvR list.
	 */
	public void setRvrsList(List<Rvr> rvrList) {
		this.rvrsList = rvrList;
	}

	/**
	 * 从 XML 加载时间表。
	 * Loads the schedule from XML.
	 */
	public static RvrSchedule load() {
		RvrSchedule rs;
		try {
			String xml = Files.readString(Config.configFile("schedule/rvr_schedule.xml").toPath(), StandardCharsets.UTF_8);
			rs = (RvrSchedule) JAXBUtil.deserialize(xml, RvrSchedule.class);
		} catch (Exception e) {
			throw new RuntimeException("Failed to initialize rvr", e);
		}
		return rs;
	}

	/**
	 * 单个 RvR 的时间表条目。
	 * Schedule entry for a single RvR.
	 */
	@XmlAccessorType(XmlAccessType.FIELD)
	@XmlRootElement(name = "rvr")
	public static class Rvr {
		/**
	 * RvR ID / RvR ID
	 */
		@XmlAttribute(required = true)
		private int id;

		/**
		 * RvR 时间列表。
		 * List of RvR times.
		 */
		@XmlElement(name = "rvrTime", required = true)
		private List<String> rvrTimes;

		/**
		 * 获取 RvR ID。
		 * Returns the RvR ID.
		 */
		public int getId() {
			return id;
		}

		/**
		 * 设置 RvR ID。
		 * Sets the RvR ID.
		 */
		public void setId(int id) {
			this.id = id;
		}

		/**
		 * 获取 RvR 时间列表。
		 * Returns the RvR times.
		 */
		public List<String> getRvrTimes() {
			return rvrTimes;
		}

		/**
		 * 设置 RvR 时间列表。
		 * Sets the RvR times.
		 */
		public void setRvrTimes(List<String> rvrTimes) {
			this.rvrTimes = rvrTimes;
		}
	}
}
