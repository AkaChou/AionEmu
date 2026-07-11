package com.aionemu.gameserver.questEngine.handlers.models.xmlQuest.operations;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlSeeAlso;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.questEngine.model.QuestEnv;

/**
 * XML 驱动任务操作的抽象基类，定义在任务环境下执行副作用的入口。
 * Abstract base for XML-driven quest operations; defines the entry that applies side effects under a quest env.
 *
 * @author Mr. Poke
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "QuestOperation")
@XmlSeeAlso({ TakeItemOperation.class, SetQuestVarOperation.class, NpcDialogOperation.class,
		GiveItemOperation.class, SetQuestStatusOperation.class })
public abstract class QuestOperation {

	/**
	 * 在给定任务环境下执行本操作。
	 * Applies this operation under the given quest environment.
	 *
	 * @param env 任务环境 / Quest environment
	 */
	public abstract void doOperate(QuestEnv env);
}
