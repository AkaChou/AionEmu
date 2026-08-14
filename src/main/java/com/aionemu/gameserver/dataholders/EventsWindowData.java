package com.aionemu.gameserver.dataholders;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlTransient;

import com.aionemu.gameserver.model.templates.event.EventsWindow;

import com.aionemu.commons.utils.collections.IntObjectHashMap;

/**
 * 活动窗口配置数据容器，按 ID 索引事件窗口模板。
 * Events window configuration data holder, indexed by event window id.
 *
 * @author Ranastic
 */
@XmlRootElement(name = "events_window")
@XmlAccessorType(XmlAccessType.FIELD)
public class EventsWindowData {

	@XmlElement(name = "event_window")
	private List<EventsWindow> events_window;

	@XmlTransient
	private IntObjectHashMap<EventsWindow> eventData = new IntObjectHashMap<EventsWindow>();

	@XmlTransient
	private Map<Integer, EventsWindow> eventDataMap = new HashMap<Integer, EventsWindow>(1);

	/**
	 * JAXB 反序列化完成后，将事件窗口写入 ID 索引。
	 * After JAXB unmarshalling, indexes event windows by id.
	 */
	void afterUnmarshal(Unmarshaller paramUnmarshaller, Object paramObject) {
		for (EventsWindow event_window : events_window) {
			eventData.put(event_window.getId(), event_window);
			eventDataMap.put(event_window.getId(), event_window);
		}
	}

	/**
	 * 返回事件窗口数量。
	 * Returns the number of event windows.
	 *
	 * @return 事件窗口数量 / event window count
	 */
	public int size() {
		return eventData.size();
	}

	/**
	 * 按 ID 获取事件窗口模板。
	 * Returns the event window template for the given id.
	 *
	 * @param id 事件窗口 ID / event window id
	 * @return 事件窗口模板，不存在则为 null / event window template, or null if absent
	 */
	public EventsWindow getEventWindowId(int id) {
		return eventData.get(id);
	}

	/**
	 * 返回全部事件窗口映射。
	 * Returns the map of all event windows.
	 *
	 * @return ID 到事件窗口的映射 / map of id to event window
	 */
	public Map<Integer, EventsWindow> getAllEvents() {
		return eventDataMap;
	}
}
