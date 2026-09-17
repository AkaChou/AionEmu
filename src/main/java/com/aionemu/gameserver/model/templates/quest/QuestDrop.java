package com.aionemu.gameserver.model.templates.quest;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlTransient;
import jakarta.xml.bind.annotation.XmlType;
import lombok.Getter;
import lombok.Setter;

/**
 * 任务掉落模板（静态数据/XML）。
 * XML template.
 */

@Getter
@Setter
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "QuestDrop")
public class QuestDrop {
	/** 返回 NPC ID / Returns the npc id */
	@XmlAttribute(name = "npc_id")
	protected Integer npcId;
	/** 返回物品 ID / Returns the item id */
	@XmlAttribute(name = "item_id")
	protected Integer itemId;
	@XmlAttribute
	protected Integer chance;
	@XmlAttribute(name = "drop_each_member")
	protected int dropEachMember = 0;
	@XmlAttribute(name = "collecting_step")
	protected int collecting_step = 0;

	/** 返回任务 ID / Returns the quest id */
	@XmlTransient
	protected Integer questId;

	/** 返回概率 / Returns the chance. */
	public int getChance() {
		if (chance == null) {
			return 100;
		}
		return chance;
	}

	/** 是否每人一份（小队） / Whether to drop for each member (group) */
	public boolean isDropEachMemberGroup() {
		return dropEachMember == 1;
	}

	/** 是否每人一份（联盟） / Whether to drop for each member (alliance) */
	public boolean isDropEachMemberAlliance() {
		return dropEachMember == 2;
	}

	/** 返回收集步骤 / Returns the collecting step */
	public int getCollectingStep() {
		return collecting_step;
	}
}
