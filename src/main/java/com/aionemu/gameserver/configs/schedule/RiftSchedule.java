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
 * Rift 裂隙活动时间表配置。
 * Rift event schedule configuration.
 *
 * @author Rinzler (Encom)
 */
@XmlRootElement(name = "rift_schedule")
@XmlAccessorType(XmlAccessType.FIELD)
public class RiftSchedule {
	/**
	 * Rift 列表。
	 * List of rifts.
	 */
	@XmlElement(name = "rift", required = true)
	private List<Rift> riftsList;

	/**
	 * 获取 Rift 列表。
	 * Returns the rift list.
	 */
	public List<Rift> getRiftsList() {
		return riftsList;
	}

	/**
	 * 设置 Rift 列表。
	 * Sets the rift list.
	 */
	public void setRiftsList(List<Rift> riftList) {
		this.riftsList = riftList;
	}

	/**
	 * 从 XML 加载时间表。
	 * Loads the schedule from XML.
	 */
	public static RiftSchedule load() {
		RiftSchedule rs;
		try {
			String xml = Files.readString(Config.configFile("schedule/rift_schedule.xml").toPath(), StandardCharsets.UTF_8);
			rs = (RiftSchedule) JAXBUtil.deserialize(xml, RiftSchedule.class);
		} catch (Exception e) {
			throw new RuntimeException("Failed to initialize rifts", e);
		}
		return rs;
	}

	/**
	 * 单个 Rift 的时间表条目。
	 * Schedule entry for a single rift.
	 */
	@XmlAccessorType(XmlAccessType.FIELD)
	@XmlRootElement(name = "rift")
	public static class Rift {
		/**
		 * 世界 ID。
		 * World ID.
		 */
		@XmlAttribute(required = true)
		private int id;

		/**
		 * 开启时间列表。
		 * List of open times.
		 */
		@XmlElement(name = "openTime", required = true)
		private List<String> openTimes;

		/**
		 * 获取世界 ID。
		 * Returns the world ID.
		 */
		public int getWorldId() {
			return id;
		}

		/**
		 * 获取开启时间列表。
		 * Returns the open times.
		 */
		public List<String> getOpenTime() {
			return openTimes;
		}

		/**
		 * 设置开启时间列表。
		 * Sets the open times.
		 */
		public void setOpenTimes(List<String> openTimes) {
			this.openTimes = openTimes;
		}
	}
}
