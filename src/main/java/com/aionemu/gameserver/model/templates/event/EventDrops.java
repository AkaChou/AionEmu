package com.aionemu.gameserver.model.templates.event;

import java.util.ArrayList;
import java.util.List;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;

/**
 * 活动掉落组模板（静态数据/XML）。
 * Event Drops Template (static data/XML).
 */

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "EventDrops")
public class EventDrops {

	@XmlElement(name = "event_drop")
	protected List<EventDrop> eventDrops;

	/** 返回活动掉落列表 / Returns the event drops */
	public List<EventDrop> getEventDrops() {
		if (eventDrops == null) {
			eventDrops = new ArrayList<EventDrop>();
		}
		return this.eventDrops;
	}
}
