package com.aionemu.gameserver.questEngine.handlers.models.xmlQuest.conditions;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.questEngine.model.QuestEnv;

/**
 * 按当前对话 ID 与配置值比较的条件。
 * Condition that compares the current dialog id against a configured value.
 *
 * @author Mr. Poke
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "DialogIdCondition")
public class DialogIdCondition extends QuestCondition {

	/** 期望的对话 ID / Expected dialog id */
	@XmlAttribute(required = true)
	protected int value;

	/**
	 * 返回期望的对话 ID。
	 * Returns the expected dialog id.
	 *
	 * Dialog id
	 */
	public int getValue() {
		return value;
	}

	/**
	 * 比较环境中的对话 ID 与配置值。
	 * Compares the environment dialog id with the configured value.
	 *
	 * @param env 任务环境 / Quest environment
	 * @return 比较是否成立 / Whether the comparison holds
	 */
	@Override
	public boolean doCheck(QuestEnv env) {
		int data = env.getDialogId();
		switch (getOp()) {
		case EQUAL:
			return data == value;
		case NOT_EQUAL:
			return data != value;
		default:
			return false;
		}
	}
}
