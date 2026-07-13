package com.aionemu.gameserver.dataholders;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlTransient;

import com.aionemu.gameserver.skillengine.model.MotionTime;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 动作时间数据容器，按动作名索引 {@link MotionTime}。
 * Motion-time data holder, indexing {@link MotionTime} by motion name.
 *
 * @author kecimis
 */
@XmlRootElement(name = "motion_times")
@XmlAccessorType(XmlAccessType.FIELD)
public class MotionData {

	@XmlElement(name = "motion_time")
	protected List<MotionTime> motionTimes;

	@XmlTransient
	private Map<String, MotionTime> motionTimesMap = new LinkedHashMap<String, MotionTime>();

	/**
	 * JAXB 反序列化完成后，按动作名建立索引。
	 * After JAXB unmarshalling, indexes motion times by name.
	 */
	void afterUnmarshal(Unmarshaller u, Object parent) {
		for (MotionTime motion : motionTimes) {
			motionTimesMap.put(motion.getName().toLowerCase(Locale.ROOT), motion);
		}
	}

	/**
	 * 返回动作时间列表；若为空则惰性创建。
	 * Returns the motion-time list, creating it lazily when null.
	 *
	 * @return 动作时间列表 / motion-time list
	 */
	public List<MotionTime> getMotionTimes() {
		if (motionTimes == null) {
			motionTimes = new ArrayList<MotionTime>();
		}
		return motionTimes;
	}

	/**
	 * 按动作名获取动作时间。
	 * Returns the motion time for the given name.
	 *
	 * motion name
	 *
	 * @param name
	 * @return 动作时间或 null / motion time or null
	 */
	public MotionTime getMotionTime(String name) {
		return name == null ? null : motionTimesMap.get(name.toLowerCase(Locale.ROOT));
	}

	/**
	 * 返回已加载的动作时间数量。
	 * Returns the number of loaded motion times.
	 *
	 * @return 动作时间数量 / motion-time count
	 */
	public int size() {
		if (motionTimes == null) {
			return 0;
		}
		return motionTimes.size();
	}
}
