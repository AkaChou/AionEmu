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
 * SvS（服务器对战）活动时间表配置。
 * SvS (Server vs Server) event schedule configuration.
 *
 * @author Rinzler (Encom)
 */
@XmlRootElement(name = "svs_schedule")
@XmlAccessorType(XmlAccessType.FIELD)
public class SvsSchedule {
	/**
	 * SvS 列表。
	 * List of SvSs.
	 */
	@XmlElement(name = "svs", required = true)
	private List<Svs> svssList;

	/**
	 * 获取 SvS 列表。
	 * Returns the SvS list.
	 */
	public List<Svs> getSvssList() {
		return svssList;
	}

	/**
	 * 设置 SvS 列表。
	 * Sets the SvS list.
	 */
	public void setSvssList(List<Svs> svsList) {
		this.svssList = svsList;
	}

	/**
	 * 从 XML 加载时间表。
	 * Loads the schedule from XML.
	 */
	public static SvsSchedule load() {
		SvsSchedule ss;
		try {
			String xml = Files.readString(Config.configFile("schedule/svs_schedule.xml").toPath(), StandardCharsets.UTF_8);
			ss = (SvsSchedule) JAXBUtil.deserialize(xml, SvsSchedule.class);
		} catch (Exception e) {
			throw new RuntimeException("Failed to initialize svs", e);
		}
		return ss;
	}

	/**
	 * 单个 SvS 的时间表条目。
	 * Schedule entry for a single SvS.
	 */
	@XmlAccessorType(XmlAccessType.FIELD)
	@XmlRootElement(name = "svs")
	public static class Svs {
		/**
	 * SvS ID / SvS ID
	 */
		@XmlAttribute(required = true)
		private int id;

		/**
		 * SvS 时间列表。
		 * List of SvS times.
		 */
		@XmlElement(name = "svsTime", required = true)
		private List<String> svsTimes;

		/**
		 * 获取 SvS ID。
		 * Returns the SvS ID.
		 */
		public int getId() {
			return id;
		}

		/**
		 * 设置 SvS ID。
		 * Sets the SvS ID.
		 */
		public void setId(int id) {
			this.id = id;
		}

		/**
		 * 获取 SvS 时间列表。
		 * Returns the SvS times.
		 */
		public List<String> getSvsTimes() {
			return svsTimes;
		}

		/**
		 * 设置 SvS 时间列表。
		 * Sets the SvS times.
		 */
		public void setSvsTimes(List<String> svsTimes) {
			this.svsTimes = svsTimes;
		}
	}
}
