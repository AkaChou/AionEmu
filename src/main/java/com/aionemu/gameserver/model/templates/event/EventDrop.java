package com.aionemu.gameserver.model.templates.event;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlTransient;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.model.templates.item.ItemTemplate;
import lombok.Getter;

/**
 * 活动掉落模板（静态数据/XML）。
 * Event Drop Template (static data/XML).
 */

@Getter
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "EventDrop")
public class EventDrop {
	/** 返回地区 ID / Returns the loc id */
	@XmlAttribute(name = "loc_id")
	protected int locId;
	/** 返回 NPC ID / Returns the npc id */
	@XmlAttribute(name = "npc_id")
	protected int npcId;
	/** 返回物品 ID / Returns the item id */
	@XmlAttribute(name = "item_id", required = true)
	protected int itemId;
	/** 获取计数。 / Returns the count. */
	@XmlAttribute(name = "count", required = true)
	protected long count;
	/** 返回概率 / Returns the chance */
	@XmlAttribute(name = "chance", required = true)
	protected float chance;
	/** 返回最小等级差 / Returns the min diff */
	@XmlAttribute(name = "minDiff")
	protected int minDiff;
	/** 返回最大等级差 / Returns the max diff */
	@XmlAttribute(name = "maxDiff")
	protected int maxDiff;
	/** 返回最低等级 / Returns the min lvl */
	@XmlAttribute(name = "minLvl")
	protected int minLvl;
	/** 返回最高等级 / Returns the max lvl */
	@XmlAttribute(name = "maxLvl")
	protected int maxLvl;

	@XmlTransient
	private ItemTemplate template;

	/** 获取物品模板。 / Returns the item template. */
	public ItemTemplate getItemTemplate() {
		if (template == null) {
			template = DataManager.ITEM_DATA.getItemTemplate(itemId);
		}
		return template;
	}
}
