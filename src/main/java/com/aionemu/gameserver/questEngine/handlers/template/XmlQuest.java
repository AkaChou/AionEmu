package com.aionemu.gameserver.questEngine.handlers.template;

import java.util.Iterator;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.handlers.models.Monster;
import com.aionemu.gameserver.questEngine.handlers.models.XmlQuestData;
import com.aionemu.gameserver.questEngine.handlers.models.xmlQuest.events.OnKillEvent;
import com.aionemu.gameserver.questEngine.handlers.models.xmlQuest.events.OnTalkEvent;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

/**
 * 通用 XML 任务模板：将 {@link XmlQuestData} 中的对话/击杀事件配置委托给对应事件处理器执行。
 * Generic XML quest template: delegates dialog/kill handling to event configs from {@link XmlQuestData}.
 */
public class XmlQuest extends QuestHandler {
	/** XML 任务配置数据 / XML quest config data */
	private final XmlQuestData xmlQuestData;

	/**
	 * 构造 XML 驱动任务处理器。
	 * Constructs an XML-driven quest handler.
	 *
	 * XML quest config
	 */
	public XmlQuest(XmlQuestData xmlQuestData) {
		super(xmlQuestData.getId());
		this.xmlQuestData = xmlQuestData;
	}

	/**
	 * 按 XML 配置注册起始/结束 NPC、对话与击杀事件。
	 * Registers start/end NPCs, talk and kill events from the XML config.
	 */
	@Override
	public void register() {
		if (xmlQuestData.getStartNpcId() != null) {
			qe.registerQuestNpc(xmlQuestData.getStartNpcId()).addOnQuestStart(getQuestId());
			qe.registerQuestNpc(xmlQuestData.getStartNpcId()).addOnTalkEvent(getQuestId());
		}
		if (xmlQuestData.getEndNpcId() != null) {
			qe.registerQuestNpc(xmlQuestData.getEndNpcId()).addOnTalkEvent(getQuestId());
		}
		for (OnTalkEvent talkEvent : xmlQuestData.getOnTalkEvent()) {
			for (int npcId : talkEvent.getIds()) {
				qe.registerQuestNpc(npcId).addOnTalkEvent(getQuestId());
			}
		}
		for (OnKillEvent killEvent : xmlQuestData.getOnKillEvent()) {
			for (Monster monster : killEvent.getMonsters()) {
				Iterator<Integer> iterator = monster.getNpcIds().iterator();
				while (iterator.hasNext()) {
					int monsterId = iterator.next();
					qe.registerQuestNpc(monsterId).addOnKillEvent(getQuestId());
				}
			}
		}
	}

	/**
	 * 处理对话事件：优先委托 XML OnTalkEvent，再处理默认接取/奖励流程。
	 * Handles dialog events: prefers XML OnTalkEvent handlers, then default accept/reward flow.
	 *
	 * @param env 任务环境 / quest environment
	 * @return 是否已处理 / whether the dialog event was handled
	 */
	@Override
	public boolean onDialogEvent(QuestEnv env) {
		env.setQuestId(getQuestId());
		for (OnTalkEvent talkEvent : xmlQuestData.getOnTalkEvent()) {
			if (talkEvent.operate(env)) {
				return true;
			}
		}
		final Player player = env.getPlayer();
		int targetId = env.getTargetId();
		QuestState qs = player.getQuestStateList().getQuestState(getQuestId());
		if (qs == null || qs.getStatus() == QuestStatus.NONE || qs.canRepeat()) {
			if (targetId == xmlQuestData.getStartNpcId()) {
				if (env.getDialog() == QuestDialog.START_DIALOG) {
					return sendQuestDialog(env, 1011);
				} else {
					return sendQuestStartDialog(env);
				}
			}
		} else if (qs.getStatus() == QuestStatus.REWARD && targetId == xmlQuestData.getEndNpcId()) {
			return sendQuestEndDialog(env);
		}
		return false;
	}

	/**
	 * 处理击杀事件：委托 XML OnKillEvent 配置执行。
	 * Handles kill events: delegates to XML OnKillEvent configs.
	 *
	 * @param env 任务环境 / quest environment
	 * @return 是否已处理 / whether the kill event was handled
	 */
	@Override
	public boolean onKillEvent(QuestEnv env) {
		env.setQuestId(getQuestId());
		for (OnKillEvent killEvent : xmlQuestData.getOnKillEvent()) {
			if (killEvent.operate(env)) {
				return true;
			}
		}
		return false;
	}
}
