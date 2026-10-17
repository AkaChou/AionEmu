package com.aionemu.gameserver.model.templates.event;

import java.util.ArrayList;
import java.util.List;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElements;
import jakarta.xml.bind.annotation.XmlType;

/**
 * 活动掉落组模板（静态数据/XML）。npc_drop 与 event_drop 共用 {@link EventDrop} 模型。
 * Event Drops Template (static data/XML). npc_drop shares the {@link EventDrop} model with event_drop.
 */

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "EventDrops")
public class EventDrops {

	/**
	 * event_drop 为全服随机掉落；npc_drop 按 npc_id 限定掉落来源 NPC（无 minDiff/maxDiff 属性，解析为 0）。
	 * event_drop is a server-wide random drop; npc_drop restricts the drop to the given npc_id (no minDiff/maxDiff attributes, parsed as 0).
	 */
	@XmlElements({
		@XmlElement(name = "event_drop", type = EventDrop.class),
		@XmlElement(name = "npc_drop", type = EventDrop.class)
	})
	protected List<EventDrop> eventDrops;

	/** 返回活动掉落列表 / Returns the event drops */
	public List<EventDrop> getEventDrops() {
		if (eventDrops == null) {
			eventDrops = new ArrayList<>();
		}
		return this.eventDrops;
	}
}
