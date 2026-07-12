package com.aionemu.gameserver.dataholders;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElementWrapper;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlTransient;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.model.templates.event.EventTemplate;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 活动配置数据容器，维护全部与当前激活的活动模板。
 * Event configuration data holder for all and currently active event templates.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "EventData", propOrder = { "active", "events" })
@XmlRootElement(name = "events_config")
public class EventData {

	@XmlElement(required = true)
	protected String active;

	@XmlElementWrapper(name = "events")
	@XmlElement(name = "event")
	protected List<EventTemplate> events;

	@XmlTransient
	private Map<String, EventTemplate> activeEvents = new LinkedHashMap<String, EventTemplate>();

	@XmlTransient
	private Map<String, EventTemplate> allEvents = new LinkedHashMap<String, EventTemplate>();

	@XmlTransient
	private int counter = 0;

	/**
	 * JAXB 反序列化完成后，根据 active 字段筛选并索引激活活动；保留 active 文本供后续查询。
	 * After JAXB unmarshalling, indexes active events from the active field; keeps active text for later queries.
	 */
	void afterUnmarshal(Unmarshaller u, Object parent) {
		if (active == null || events == null) {
			return;
		}
		counter = 0;
		allEvents.clear();
		activeEvents.clear();

		Set<String> ae = new HashSet<String>();
		Collections.addAll(ae, active.split(";"));

		for (EventTemplate ev : events) {
			if (ae.contains(ev.getName()) && ev.isActive()) {
				activeEvents.put(ev.getName(), ev);
				counter++;
			}
			allEvents.put(ev.getName(), ev);
		}

		events.clear();
		events = null;
	}

	/**
	 * 返回激活活动数量。
	 * Returns the number of active events.
	 *
	 * @return 激活活动数 / active event count
	 */
	public int size() {
		return counter;
	}

	/**
	 * 返回原始激活活动文本（分号分隔的活动名列表）。
	 * Returns the raw active-events text (semicolon-separated event names).
	 *
	 * @return 激活活动配置文本 / active events configuration text
	 */
	public String getActiveText() {
		return active;
	}

	/**
	 * 返回全部活动模板的快照列表。
	 * Returns a snapshot list of all event templates.
	 *
	 * @return 全部活动模板 / all event templates
	 */
	public List<EventTemplate> getAllEvents() {
		List<EventTemplate> result = new ArrayList<EventTemplate>();
		synchronized (allEvents) {
			result.addAll(allEvents.values());
		}
		return result;
	}

	/**
	 * 替换全部活动列表并重新构建索引；若同名旧活动已启动则保留 started 状态。
	 * Replaces the full event list and rebuilds indexes; preserves started state from same-named old events.
	 *
	 * @param events 新的活动模板列表 / new event template list
	 * @param active 激活活动名配置文本 / active event names configuration text
	 */
	public void setAllEvents(List<EventTemplate> events, String active) {
		if (events == null) {
			events = new ArrayList<EventTemplate>();
		}
		this.events = events;
		this.active = active;

		for (EventTemplate et : this.events) {
			if (allEvents.containsKey(et.getName())) {
				EventTemplate oldEvent = allEvents.get(et.getName());
				if (oldEvent.isActive() && oldEvent.isStarted()) {
					et.setStarted();
				}
			}
		}
		afterUnmarshal(null, null);
	}

	/**
	 * 返回当前激活活动模板的快照列表。
	 * Returns a snapshot list of currently active event templates.
	 *
	 * @return 激活活动模板 / active event templates
	 */
	public List<EventTemplate> getActiveEvents() {
		List<EventTemplate> result = new ArrayList<EventTemplate>();
		synchronized (activeEvents) {
			result.addAll(activeEvents.values());
		}

		return result;
	}

	/**
	 * 判断指定名称的活动是否处于激活状态。
	 * Checks whether the named event is currently active.
	 *
	 * event name
	 *
	 * @param eventName
	 * @return 若激活返回 true / true if the event is active
	 */
	public boolean Contains(String eventName) {
		return activeEvents.containsKey(eventName);
	}
}
