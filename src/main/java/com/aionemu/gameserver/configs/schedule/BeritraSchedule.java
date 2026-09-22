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
import lombok.Getter;
import lombok.Setter;

/**
 * Beritra 入侵活动时间表配置。
 * Beritra invasion event schedule configuration.
 *
 * @author Rinzler (Encom)
 */
@Getter
@XmlRootElement(name = "beritra_schedule")
@XmlAccessorType(XmlAccessType.FIELD)
public class BeritraSchedule {
	/**
	 * Beritra 列表。
	 * List of beritras.
	 * -- GETTER --
	 *  获取 Beritra 列表。
	 *  Returns the beritra list.

	 */
	@XmlElement(name = "beritra", required = true)
	private List<Beritra> beritrasList;

	/**
	 * 设置 Beritra 列表。
	 * Sets the beritra list.
	 */
	public void setInvasionsList(List<Beritra> beritraList) {
		this.beritrasList = beritraList;
	}

	/**
	 * 从 XML 加载时间表。
	 * Loads the schedule from XML.
	 */
	public static BeritraSchedule load() {
		BeritraSchedule bs;
		try {
			String xml = Files.readString(Config.configFile("schedule/beritra_schedule.xml").toPath(), StandardCharsets.UTF_8);
			bs = JAXBUtil.deserialize(xml, BeritraSchedule.class);
		} catch (Exception e) {
			throw new RuntimeException("Failed to initialize beritra", e);
		}
		return bs;
	}

	/**
	 * 单个 Beritra 的时间表条目。
	 * Schedule entry for a single beritra.
	 */
	@Setter
	@Getter
    @XmlAccessorType(XmlAccessType.FIELD)
	@XmlRootElement(name = "beritra")
	public static class Beritra {
		/**
		 * 贝里特拉 ID / Beritra ID
         * -- GETTER --
         *  获取 Beritra ID。
         *  Returns the beritra ID.
		 * -- SETTER --
		 *  设置 Beritra ID。
		 *  Sets the beritra ID.


		 */
		@XmlAttribute(required = true)
		private int id;

		/**
		 * 入侵时间列表。
		 * List of invasion times.
         * -- GETTER --
         *  获取入侵时间列表。
         *  Returns the invasion times.
		 * -- SETTER --
		 *  设置入侵时间列表。
		 *  Sets the invasion times.


		 */
		@XmlElement(name = "invasionTime", required = true)
		private List<String> invasionTimes;

	}
}
