package com.aionemu.gameserver.model.templates.portal;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;

/**
 * 任务 Req 模板（静态数据/XML）。
 * XML template.
 *
 * @author xTz
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "QuestReq")
public class QuestReq {

	@XmlAttribute(name = "quest_id")
	protected int questId;
	@XmlAttribute(name = "quest_step")
	protected int questStep;
	@XmlAttribute(name = "err_quest")
	protected int errQuest;

	/** 返回任务 ID / Returns the quest id */
	public int getQuestId() {
		return questId;
	}

	/** 设置 quest id / Sets the quest id */
	public void setQuestId(int value) {
		this.questId = value;
	}

	/** 返回 quest step / Returns the quest step */
	public int getQuestStep() {
		return questStep;
	}

	/** 设置 quest step / Sets the quest step */
	public void setQuestStep(int value) {
		this.questStep = value;
	}

	/** 返回 err quest / Returns the err quest */
	public int getErrQuest() {
		return errQuest;
	}
}
