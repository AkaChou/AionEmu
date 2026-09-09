package com.aionemu.gameserver.model.templates.portal;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;
import lombok.Getter;
import lombok.Setter;

/**
 * 任务 Req 模板（静态数据/XML）。
 * XML template.
 *
 * @author xTz
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "QuestReq")
public class QuestReq {

	/** 返回任务 ID / Returns the quest id */
	@Getter
	@Setter
	@XmlAttribute(name = "quest_id")
	protected int questId;
	/** 返回 quest step / Returns the quest step */
	@Getter
	@Setter
	@XmlAttribute(name = "quest_step")
	protected int questStep;
	/** 返回 err quest / Returns the err quest */
	@Getter
	@XmlAttribute(name = "err_quest")
	protected int errQuest;
}
