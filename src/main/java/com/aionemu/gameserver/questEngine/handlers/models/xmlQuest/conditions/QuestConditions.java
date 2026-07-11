package com.aionemu.gameserver.questEngine.handlers.models.xmlQuest.conditions;

import java.util.List;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElements;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.questEngine.model.ConditionUnionType;
import com.aionemu.gameserver.questEngine.model.QuestEnv;

/**
 * XML 驱动任务条件集合，按 AND/OR 组合多个 {@link QuestCondition}。
 * Set of XML-driven quest conditions combined with AND/OR via {@link ConditionUnionType}.
 *
 * @author Mr. Poke
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "QuestConditions", propOrder = { "conditions" })
public class QuestConditions {

	/** 多态条件列表 / Polymorphic condition list */
	@XmlElements({ @XmlElement(name = "quest_status", type = QuestStatusCondition.class),
			@XmlElement(name = "npc_id", type = NpcIdCondition.class),
			@XmlElement(name = "pc_inventory", type = PcInventoryCondition.class),
			@XmlElement(name = "quest_var", type = QuestVarCondition.class),
			@XmlElement(name = "dialog_id", type = DialogIdCondition.class) })
	protected List<QuestCondition> conditions;
	/** 条件组合方式（AND / OR）。 / How conditions are combined (AND / OR). */
	@XmlAttribute(required = true)
	protected ConditionUnionType operate;

	/**
	 * 按配置的组合方式校验整组条件。
	 * Evaluates the whole condition set using the configured union type.
	 *
	 * @param env 任务环境 / Quest environment
	 * @return 组合结果是否通过 / Whether the combined result passes
	 */
	public boolean checkConditionOfSet(QuestEnv env) {
		boolean inCondition = (operate == ConditionUnionType.AND);
		for (QuestCondition cond : conditions) {
			boolean bCond = cond.doCheck(env);
			switch (operate) {
			case AND:
				if (!bCond) {
					return false;
				}
				inCondition = inCondition && bCond;
				break;
			case OR:
				if (bCond) {
					return true;
				}
				inCondition = inCondition || bCond;
				break;
			}
		}
		return inCondition;
	}
}
