package com.aionemu.gameserver.questEngine.handlers.models.xmlQuest.operations;

import java.util.List;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElements;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.questEngine.model.QuestEnv;

/**
 * XML 驱动任务操作集合，顺序执行全部子操作并返回是否覆盖默认处理。
 * Set of XML-driven quest operations; runs children in order and returns whether to override default handling.
 *
 * @author Mr. Poke
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "QuestOperations", propOrder = { "operations" })
public class QuestOperations {

	/** 多态操作列表 / Polymorphic operation list */
	@XmlElements({ @XmlElement(name = "take_item", type = TakeItemOperation.class),
			@XmlElement(name = "npc_dialog", type = NpcDialogOperation.class),
			@XmlElement(name = "set_quest_status", type = SetQuestStatusOperation.class),
			@XmlElement(name = "give_item", type = GiveItemOperation.class),
			@XmlElement(name = "npc_use", type = ActionItemUseOperation.class),
			@XmlElement(name = "set_quest_var", type = SetQuestVarOperation.class),
			@XmlElement(name = "collect_items", type = CollectItemQuestOperation.class) })
	protected List<QuestOperation> operations;
	/** 是否覆盖默认处理；null 表示 true / Whether to override default handling; null means true */
	@XmlAttribute
	protected Boolean override;

	/**
	 * 返回是否覆盖默认处理（未配置时默认 true）。
	 * Returns whether default handling is overridden (defaults to true when unset).
	 *
	 * Whether override is enabled
	 */
	public boolean isOverride() {
		if (override == null) {
			return true;
		} else {
			return override;
		}
	}

	/**
	 * 顺序执行全部子操作，并返回覆盖标志。
	 * Runs all child operations in order and returns the override flag.
	 *
	 * @param env 任务环境 / Quest environment
	 * @return 是否覆盖默认处理 / Whether to override default handling
	 */
	public boolean operate(QuestEnv env) {
		if (operations != null) {
			for (QuestOperation oper : operations) {
				oper.doOperate(env);
			}
		}
		return isOverride();
	}
}
