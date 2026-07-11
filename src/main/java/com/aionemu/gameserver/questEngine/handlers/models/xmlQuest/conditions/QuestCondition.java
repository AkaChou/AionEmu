package com.aionemu.gameserver.questEngine.handlers.models.xmlQuest.conditions;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlSeeAlso;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.questEngine.model.ConditionOperation;
import com.aionemu.gameserver.questEngine.model.QuestEnv;

/**
 * XML 驱动任务条件的抽象基类，定义比较运算符与校验入口。
 * Abstract base for XML-driven quest conditions; defines the comparison operator and check entry.
 *
 * @author Mr. Poke
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "QuestCondition")
@XmlSeeAlso({ NpcIdCondition.class, DialogIdCondition.class, PcInventoryCondition.class, QuestVarCondition.class,
		QuestStatusCondition.class })
public abstract class QuestCondition {

	/** 条件比较运算符 / Condition comparison operator */
	@XmlAttribute(required = true)
	protected ConditionOperation op;

	/**
	 * 返回条件比较运算符。
	 * Returns the condition comparison operator.
	 *
	 * @return 比较运算符 / Comparison operator
	 */
	public ConditionOperation getOp() {
		return op;
	}

	/**
	 * 在给定任务环境下校验本条件是否成立。
	 * Evaluates whether this condition holds under the given quest environment.
	 *
	 * @param env 任务环境 / Quest environment
	 * @return 条件是否通过 / Whether the condition passes
	 */
	public abstract boolean doCheck(QuestEnv env);
}
