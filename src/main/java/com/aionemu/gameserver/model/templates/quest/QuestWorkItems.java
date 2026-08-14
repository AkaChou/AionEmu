package com.aionemu.gameserver.model.templates.quest;

import java.util.ArrayList;
import java.util.List;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;

/**
 * 任务 Work 物品模板（静态数据/XML）。
 * XML template.
 */

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "QuestWorkItems", propOrder = { "questWorkItem" })
public class QuestWorkItems {

	@XmlElement(name = "quest_work_item")
	protected List<QuestItems> questWorkItem;

	/**
	 * 返回任务工作物品列表（惰性初始化，修改会直接反映到 JAXB 对象）。
	 * Returns the quest work item list (lazily initialized; modifications are reflected in the JAXB object).
	 *
	 * @return 工作物品列表 / list of {@link QuestItems}
	 */
	public List<QuestItems> getQuestWorkItem() {
		if (questWorkItem == null) {
			questWorkItem = new ArrayList<QuestItems>();
		}
		return this.questWorkItem;
	}
}
