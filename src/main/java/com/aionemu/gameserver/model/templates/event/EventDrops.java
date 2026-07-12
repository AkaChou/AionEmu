package com.aionemu.gameserver.model.templates.event;

import java.util.ArrayList;
import java.util.List;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;

/**
 * 活动 Drops 模板（静态数据/XML）。
 * XML template.
 */

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "EventDrops")
public class EventDrops {

	@XmlElement(name = "event_drop")
	protected List<EventDrop> eventDrops;

	/** 返回 event drops / Returns the event drops */
	public List<EventDrop> getEventDrops() {
		if (eventDrops == null) {
			eventDrops = new ArrayList<EventDrop>();
		}
		return this.eventDrops;
	}
}
