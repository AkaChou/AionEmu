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
 * Dredgion 欧比斯舰活动时间表配置。
 * Dredgion event schedule configuration.
 *
 * @author Rinzler (Encom)
 */
@XmlRootElement(name = "dredgion_schedule")
@XmlAccessorType(XmlAccessType.FIELD)
public class DredgionSchedule {
	/**
	 * Dredgion 列表。
	 * List of dredgions.
	 */
	@XmlElement(name = "dredgion", required = true)
	private List<Dredgion> dredgionsList;

	/**
	 * 获取 Dredgion 列表。
	 * Returns the dredgion list.
	 */
	public List<Dredgion> getDredgionsList() {
		return dredgionsList;
	}

	/**
	 * 设置 Dredgion 列表。
	 * Sets the dredgion list.
	 */
	public void setZorshivsList(List<Dredgion> dredgionList) {
		this.dredgionsList = dredgionList;
	}

	/**
	 * 从 XML 加载时间表。
	 * Loads the schedule from XML.
	 */
	public static DredgionSchedule load() {
		DredgionSchedule ds;
		try {
			String xml = Files.readString(Config.configFile("schedule/dredgion_schedule.xml").toPath(), StandardCharsets.UTF_8);
			ds = (DredgionSchedule) JAXBUtil.deserialize(xml, DredgionSchedule.class);
		} catch (Exception e) {
			throw new RuntimeException("Failed to initialize dredgion", e);
		}
		return ds;
	}

	/**
	 * 单个 Dredgion 的时间表条目。
	 * Schedule entry for a single dredgion.
	 */
	@XmlAccessorType(XmlAccessType.FIELD)
	@XmlRootElement(name = "dredgion")
	public static class Dredgion {
		/**
		 * 无畏舰 ID / Dredgion ID
		 */
		@XmlAttribute(required = true)
		private int id;

		/**
		 * Zorshiv 时间列表。
		 * List of zorshiv times.
		 */
		@XmlElement(name = "zorshivTime", required = true)
		private List<String> zorshivTimes;

		/**
		 * 获取 Dredgion ID。
		 * Returns the dredgion ID.
		 */
		public int getId() {
			return id;
		}

		/**
		 * 设置 Dredgion ID。
		 * Sets the dredgion ID.
		 */
		public void setId(int id) {
			this.id = id;
		}

		/**
		 * 获取 Zorshiv 时间列表。
		 * Returns the zorshiv times.
		 */
		public List<String> getZorshivTimes() {
			return zorshivTimes;
		}

		/**
		 * 设置 Zorshiv 时间列表。
		 * Sets the zorshiv times.
		 */
		public void setZorshivTimes(List<String> zorshivTimes) {
			this.zorshivTimes = zorshivTimes;
		}
	}
}
