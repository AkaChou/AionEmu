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
 * Vortex 漩涡入侵活动时间表配置。
 * Vortex invasion event schedule configuration.
 * @author Rinzler (Encom)
 */
@Getter
@XmlRootElement(name = "vortex_schedule")
@XmlAccessorType(XmlAccessType.FIELD)
public class VortexSchedule {
	/**
	 * Vortex 列表。
	 * List of vortexes.
	 * -- GETTER --
	 *  获取 Vortex 列表。
	 *  Returns the vortex list.
	 */
	@XmlElement(name = "vortex", required = true)
	private List<Vortex> vortexsList;

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
			vs = JAXBUtil.deserialize(xml, VortexSchedule.class);
		} catch (Exception e) {
			throw new RuntimeException("Failed to initialize vortex", e);
		}
		return vs;
	}

	/**
	 * 单个 Vortex 的时间表条目。
	 * Schedule entry for a single vortex.
	 */
	@Setter
	@Getter
    @XmlAccessorType(XmlAccessType.FIELD)
	@XmlRootElement(name = "vortex")
	public static class Vortex {
		/**
		 * 漩涡 ID / Vortex ID
         * -- GETTER --
         *  获取 Vortex ID。
         *  Returns the vortex ID.
		 * -- SETTER --
		 *  设置 Vortex ID。
		 *  Sets the vortex ID.
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
