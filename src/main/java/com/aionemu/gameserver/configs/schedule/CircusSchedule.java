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
 * Circus 马戏团活动时间表配置。
 * Circus event schedule configuration.
 * @author Rinzler (Encom)
 */
@Setter
@Getter
@XmlRootElement(name = "circus_schedule")
@XmlAccessorType(XmlAccessType.FIELD)
public class CircusSchedule {
	/**
	 * Circus 列表。
	 * List of circuses.
	 * -- GETTER --
	 *  获取 Circus 列表。
	 *  Returns the circus list.
	 * -- SETTER --
	 *  设置 Circus 列表。
	 *  Sets the circus list.
	 */
	@XmlElement(name = "circus", required = true)
	private List<Circus> circussList;

	/**
	 * 从 XML 加载时间表。
	 * Loads the schedule from XML.
	 */
	public static CircusSchedule load() {
		CircusSchedule cs;
		try {
			String xml = Files.readString(Config.configFile("schedule/circus_schedule.xml").toPath(), StandardCharsets.UTF_8);
			cs = JAXBUtil.deserialize(xml, CircusSchedule.class);
		} catch (Exception e) {
			throw new RuntimeException("Failed to initialize circus", e);
		}
		return cs;
	}

	/**
	 * 单个 Circus 的时间表条目。
	 * Schedule entry for a single circus.
	 */
	@Setter
	@Getter
    @XmlAccessorType(XmlAccessType.FIELD)
	@XmlRootElement(name = "circus")
	public static class Circus {
		/**
		 * 马戏团 ID / Circus ID
         * -- GETTER --
         *  获取 Circus ID。
         *  Returns the circus ID.
		 * -- SETTER --
		 *  设置 Circus ID。
		 *  Sets the circus ID.
		 */
		@XmlAttribute(required = true)
		private int id;

		/**
		 * Circus 时间列表。
		 * List of circus times.
         * -- GETTER --
         *  获取 Circus 时间列表。
         *  Returns the circus times.
		 * -- SETTER --
		 *  设置 Circus 时间列表。
		 *  Sets the circus times.
		 */
		@XmlElement(name = "circusTime", required = true)
		private List<String> circusTimes;

	}
}
