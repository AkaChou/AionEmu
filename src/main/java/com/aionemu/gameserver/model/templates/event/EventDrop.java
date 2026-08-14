package com.aionemu.gameserver.model.templates.event;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlTransient;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.model.templates.item.ItemTemplate;

/**
 * 活动掉落模板（静态数据/XML）。
 * Event Drop Template (static data/XML).
 */

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "EventDrop")
public class EventDrop {
	@XmlAttribute(name = "loc_id")
	protected int locId;
	@XmlAttribute(name = "npc_id")
	protected int npcId;
	@XmlAttribute(name = "item_id", required = true)
	protected int itemId;
	@XmlAttribute(name = "count", required = true)
	protected long count;
	@XmlAttribute(name = "chance", required = true)
	protected float chance;
	@XmlAttribute(name = "minDiff")
	protected int minDiff;
	@XmlAttribute(name = "maxDiff")
	protected int maxDiff;
	@XmlAttribute(name = "minLvl")
	protected int minLvl;
	@XmlAttribute(name = "maxLvl")
	protected int maxLvl;

	@XmlTransient
	private ItemTemplate template;

	/** 返回物品 ID / Returns the item id */
	public int getItemId() {
		return itemId;
	}

	/** 获取计数。 / Returns the count. */
	public long getCount() {
		return count;
	}

	/** 返回概率 / Returns the chance */
	public float getChance() {
		return chance;
	}

	/** 返回最小等级差 / Returns the min diff */
	public int getMinDiff() {
		return minDiff;
	}

	/** 返回最大等级差 / Returns the max diff */
	public int getMaxDiff() {
		return maxDiff;
	}

	/** 返回地区 ID / Returns the loc id */
	public int getLocId() {
		return locId;
	}

	/** 返回 NPC ID / Returns the npc id */
	public int getNpcId() {
		return npcId;
	}

	/** 返回最低等级 / Returns the min lvl */
	public int getMinLvl() {
		return minLvl;
	}

	/** 返回最高等级 / Returns the max lvl */
	public int getMaxLvl() {
		return maxLvl;
	}

	/** 获取物品模板。 / Returns the item template. */
	public ItemTemplate getItemTemplate() {
		if (template == null) {
			template = DataManager.ITEM_DATA.getItemTemplate(itemId);
		}
		return template;
	}
}
