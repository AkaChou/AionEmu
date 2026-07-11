package com.aionemu.gameserver.model.templates.quest;

import java.util.ArrayList;
import java.util.List;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;

/**
 * 任务 Work 物品模板（静态数据/XML）。
 * XML template. / XML template.
 */

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "QuestWorkItems", propOrder = { "questWorkItem" })
public class QuestWorkItems {

	@XmlElement(name = "quest_work_item")
	protected List<QuestItems> questWorkItem;

	/**
	 * 获取 questWorkItem 属性值。 / Gets the value of the questWorkItem property. <p> This accessor method returns a reference to the live list, not a snapshot. Therefore any modification you make to the returned list will be present inside the JAXB object. This is why there is not a <CODE>set</CODE> method for the questWorkItem property. <p> For example, to add a new item, do as follows: <pre> getQuestWorkItem().add(newItem); </pre> <p> Objects of the following type(s) are allowed in the list {@link QuestItems }
	 */
	public List<QuestItems> getQuestWorkItem() {
		if (questWorkItem == null) {
			questWorkItem = new ArrayList<QuestItems>();
		}
		return this.questWorkItem;
	}
}
