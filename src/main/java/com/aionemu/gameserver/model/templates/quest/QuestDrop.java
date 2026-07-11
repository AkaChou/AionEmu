package com.aionemu.gameserver.model.templates.quest;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlTransient;
import jakarta.xml.bind.annotation.XmlType;

/**
 * 任务掉落模板（静态数据/XML）。
 * XML template. / XML template.
 */

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "QuestDrop")
public class QuestDrop {
	@XmlAttribute(name = "npc_id")
	protected Integer npcId;
	@XmlAttribute(name = "item_id")
	protected Integer itemId;
	@XmlAttribute
	protected Integer chance;
	@XmlAttribute(name = "drop_each_member")
	protected int dropEachMember = 0;
	@XmlAttribute(name = "collecting_step")
	protected int collecting_step = 0;

	@XmlTransient
	protected Integer questId;

	/** 返回 NPC ID / Returns the npc id */
	public Integer getNpcId() {
		return npcId;
	}

	/** 返回物品 ID / Returns the item id */
	public Integer getItemId() {
		return itemId;
	}

	/** 返回概率 / Returns the chance*/
	public int getChance() {
		if (chance == null) {
			return 100;
		}
		return chance;
	}

	/** Whether 掉落 each 成员小队 / Whether drop each member group */
	public boolean isDropEachMemberGroup() {
		return dropEachMember == 1;
	}

	/** Whether 掉落 each 成员联盟 / Whether drop each member alliance */
	public boolean isDropEachMemberAlliance() {
		return dropEachMember == 2;
	}

	/** 返回任务 ID / Returns the quest id */
	public Integer getQuestId() {
		return questId;
	}

	/** 返回 collecting step / Returns the collecting step */
	public int getCollectingStep() {
		return collecting_step;
	}

	/** 设置 quest id / Sets the quest id */
	public void setQuestId(Integer questId) {
		this.questId = questId;
	}
}
