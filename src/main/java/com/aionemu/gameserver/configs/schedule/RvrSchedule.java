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
 * RvR（种族对战）活动时间表配置。
 * RvR (Race vs Race) event schedule configuration.
 * @author Rinzler (Encom)
 */
@Setter
@Getter
@XmlRootElement(name = "rvr_schedule")
@XmlAccessorType(XmlAccessType.FIELD)
public class RvrSchedule {
	/**
	 * RvR 列表。
	 * List of RvRs.
	 * -- GETTER --
	 *  获取 RvR 列表。
	 *  Returns the RvR list.
	 * -- SETTER --
	 *  设置 RvR 列表。
	 *  Sets the RvR list.
	 */
	@XmlElement(name = "rvr", required = true)
	private List<Rvr> rvrsList;

	/**
	 * 从 XML 加载时间表。
	 * Loads the schedule from XML.
	 */
	public static RvrSchedule load() {
		RvrSchedule rs;
		try {
			String xml = Files.readString(Config.configFile("schedule/rvr_schedule.xml").toPath(), StandardCharsets.UTF_8);
			rs = JAXBUtil.deserialize(xml, RvrSchedule.class);
		} catch (Exception e) {
			throw new RuntimeException("Failed to initialize rvr", e);
		}
		return rs;
	}

	/**
	 * 单个 RvR 的时间表条目。
	 * Schedule entry for a single RvR.
	 */
	@Setter
	@Getter
    @XmlAccessorType(XmlAccessType.FIELD)
	@XmlRootElement(name = "rvr")
	public static class Rvr {
		/**
		 * RvR ID / RvR ID
         * -- GETTER --
         *  获取 RvR ID。
         *  Returns the RvR ID.
		 * -- SETTER --
		 *  设置 RvR ID。
		 *  Sets the RvR ID.
		 */
		@XmlAttribute(required = true)
		private int id;

		/**
		 * RvR 时间列表。
		 * List of RvR times.
         * -- GETTER --
         *  获取 RvR 时间列表。
         *  Returns the RvR times.
		 * -- SETTER --
		 *  设置 RvR 时间列表。
		 *  Sets the RvR times.
		 */
		@XmlElement(name = "rvrTime", required = true)
		private List<String> rvrTimes;

	}
}
