package com.aionemu.gameserver.questEngine.handlers.models.xmlQuest.operations;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.services.QuestService;

/**
 * 收集物品检查操作：通过时走 true 分支，否则走 false 分支。
 * Collect-item check operation: runs the true branch on success, false branch otherwise.
 *
 * @author Mr. Poke
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "CollectItemQuestOperation", propOrder = { "_true", "_false" })
public class CollectItemQuestOperation extends QuestOperation {

	/** 检查通过时执行的操作 / Operations when the check succeeds */
	@XmlElement(name = "true", required = true)
	protected QuestOperations _true;
	/** 检查失败时执行的操作 / Operations when the check fails */
	@XmlElement(name = "false", required = true)
	protected QuestOperations _false;
	/** 是否 to remove items on check; null treated as true / Whether to remove items on check; null treated as true */
	@XmlAttribute
	protected Boolean removeItems;

	/**
	 * 调用 {@link QuestService#collectItemCheck} 并按结果执行分支。
	 * Calls {@link QuestService#collectItemCheck} and runs the matching branch.
	 *
	 * @param env 任务环境 / Quest environment
	 */
	@Override
	public void doOperate(QuestEnv env) {
		if (QuestService.collectItemCheck(env, removeItems == null || removeItems)) {
			_true.operate(env);
		} else {
			_false.operate(env);
		}
	}
}
