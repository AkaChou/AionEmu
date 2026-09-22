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
 * Anoha 活动时间表配置。
 * Anoha event schedule configuration.
 *
 * @author Rinzler (Encom)
 */
@Getter
@XmlRootElement(name = "anoha_schedule")
@XmlAccessorType(XmlAccessType.FIELD)
public class AnohaSchedule {
	/**
	 * Anoha 列表。
	 * List of anohas.
	 * -- GETTER --
	 *  获取 Anoha 列表。
	 *  Returns the anoha list.

	 */
	@XmlElement(name = "anoha", required = true)
	private List<Anoha> anohasList;

	/**
	 * 设置 Anoha 列表。
	 * Sets the anoha list.
	 */
	public void setBerserksList(List<Anoha> anohaList) {
		this.anohasList = anohaList;
	}

	/**
	 * 从 XML 加载时间表。
	 * Loads the schedule from XML.
	 */
	public static AnohaSchedule load() {
		AnohaSchedule as;
		try {
			String xml = Files.readString(Config.configFile("schedule/anoha_schedule.xml").toPath(), StandardCharsets.UTF_8);
			as = JAXBUtil.deserialize(xml, AnohaSchedule.class);
		} catch (Exception e) {
			throw new RuntimeException("Failed to initialize anoha", e);
		}
		return as;
	}

	/**
	 * 单个 Anoha 的时间表条目。
	 * Schedule entry for a single anoha.
	 */
	@Setter
	@Getter
    @XmlAccessorType(XmlAccessType.FIELD)
	@XmlRootElement(name = "anoha")
	public static class Anoha {
		/**
		 * 阿诺哈 ID / Anoha ID
         * -- GETTER --
         *  获取 Anoha ID。
         *  Returns the anoha ID.
		 * -- SETTER --
		 *  设置 Anoha ID。
		 *  Sets the anoha ID.


		 */
		@XmlAttribute(required = true)
		private int id;

		/**
		 * 狂暴时间列表。
		 * List of berserk times.
         * -- GETTER --
         *  获取狂暴时间列表。
         *  Returns the berserk times.
		 * -- SETTER --
		 *  设置狂暴时间列表。
		 *  Sets the berserk times.


		 */
		@XmlElement(name = "berserkTime", required = true)
		private List<String> berserkTimes;

	}
}
