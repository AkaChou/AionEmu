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
 * Instance 副本活动时间表配置。
 * Instance event schedule configuration.
 *
 * @author Rinzler (Encom)
 */
@XmlRootElement(name = "instance_schedule")
@XmlAccessorType(XmlAccessType.FIELD)
public class InstanceSchedule {
	/**
	 * Instance 列表。
	 * List of instances.
	 */
	@XmlElement(name = "instance", required = true)
	private List<Instance> instancesList;

	/**
	 * 获取 Instance 列表。
	 * Returns the instance list.
	 */
	public List<Instance> getInstancesList() {
		return instancesList;
	}

	/**
	 * 设置 Instance 列表。
	 * Sets the instance list.
	 */
	public void setInstancesList(List<Instance> instanceList) {
		this.instancesList = instanceList;
	}

	/**
	 * 从 XML 加载时间表。
	 * Loads the schedule from XML.
	 */
	public static InstanceSchedule load() {
		InstanceSchedule is;
		try {
			String xml = Files.readString(Config.configFile("schedule/instance_schedule.xml").toPath(), StandardCharsets.UTF_8);
			is = (InstanceSchedule) JAXBUtil.deserialize(xml, InstanceSchedule.class);
		} catch (Exception e) {
			throw new RuntimeException("Failed to initialize instance", e);
		}
		return is;
	}

	/**
	 * 单个 Instance 的时间表条目。
	 * Schedule entry for a single instance.
	 */
	@XmlAccessorType(XmlAccessType.FIELD)
	@XmlRootElement(name = "instance")
	public static class Instance {
		/**
		 * 副本 ID / Instance ID
		 */
		@XmlAttribute(required = true)
		private int id;

		/**
		 * 副本时间列表。
		 * List of instance times.
		 */
		@XmlElement(name = "instanceTime", required = true)
		private List<String> instanceTimes;

		/**
		 * 获取 Instance ID。
		 * Returns the instance ID.
		 */
		public int getId() {
			return id;
		}

		/**
		 * 设置 Instance ID。
		 * Sets the instance ID.
		 */
		public void setId(int id) {
			this.id = id;
		}

		/**
		 * 获取副本时间列表。
		 * Returns the instance times.
		 */
		public List<String> getInstanceTimes() {
			return instanceTimes;
		}

		/**
		 * 设置副本时间列表。
		 * Sets the instance times.
		 */
		public void setInstanceTimes(List<String> instanceTimes) {
			this.instanceTimes = instanceTimes;
		}
	}
}
