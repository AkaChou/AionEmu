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
 * Rift 裂隙活动时间表配置。
 * Rift event schedule configuration.
 *
 * @author Rinzler (Encom)
 */
@Setter
@Getter
@XmlRootElement(name = "rift_schedule")
@XmlAccessorType(XmlAccessType.FIELD)
public class RiftSchedule {
	/**
	 * Rift 列表。
	 * List of rifts.
	 * -- GETTER --
	 * 获取 Rift 列表。
	 * Returns the rift list.
	 * -- SETTER --
	 * 设置 Rift 列表。
	 * Sets the rift list.
	 */
	@XmlElement(name = "rift", required = true)
	private List<Rift> riftsList;

	/**
	 * 从 XML 加载时间表：外部运行时文件（{@code aion/config/schedule/rift_schedule.xml}）优先，
	 * 缺失时回退到 classpath 内打包资源，保证全新检出无需部署脚本即可自测；部署环境行为不变（外部文件仍优先）。
	 * Loads the schedule from XML. The external runtime file ({@code aion/config/schedule/rift_schedule.xml})
	 * wins; when absent the packaged classpath resource is used so a fresh checkout can self-test without the
	 * deploy script. Deployed behavior is unchanged (external file still wins).
	 */
	public static RiftSchedule load() {
		RiftSchedule rs;
		try {
			var external = Config.configFile("schedule/rift_schedule.xml").toPath();
			String xml = Files.isRegularFile(external)
				? Files.readString(external, StandardCharsets.UTF_8)
				: new String(RiftSchedule.class.getResourceAsStream("/aion/config/schedule/rift_schedule.xml")
				.readAllBytes(), StandardCharsets.UTF_8);
			rs = JAXBUtil.deserialize(xml, RiftSchedule.class);
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
		 * -- SETTER --
		 * 设置开启时间列表。
		 * Sets the open times.
		 */
		@Setter
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

	}
}
