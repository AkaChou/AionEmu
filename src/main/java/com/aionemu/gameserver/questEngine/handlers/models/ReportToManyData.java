package com.aionemu.gameserver.questEngine.handlers.models;

import java.util.List;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.questEngine.QuestEngine;
import com.aionemu.gameserver.questEngine.handlers.template.ReportToMany;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 多步汇报类任务的 XML 数据模型（依次对话多个 NPC），注册 {@link ReportToMany} 模板。
 * XML data model for multi-step report quests (talk to NPCs in sequence); registers {@link ReportToMany}.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ReportToManyData")
public class ReportToManyData extends XMLQuest {

	/**
	 * 触发任务的起始物品 ID（可选）。
	 * Start item id that triggers the quest (optional).
	 */
	@XmlAttribute(name = "start_item_id")
	protected int startItemId;

	/**
	 * 接取任务的起始 NPC ID 列表。
	 * Start NPC ids that offer the quest.
	 */
	@XmlAttribute(name = "start_npc_ids")
	protected List<Integer> startNpcIds;

	/**
	 * 最终交还的结束 NPC ID 列表。
	 * Final end NPC ids for turn-in.
	 */
	@XmlAttribute(name = "end_npc_ids")
	protected List<Integer> endNpcIds;

	/**
	 * 起始对话 dialog ID。
	 * Start dialogue dialog id.
	 */
	@XmlAttribute(name = "start_dialog_id")
	protected int startDialog;

	/**
	 * 结束对话 dialog ID。
	 * End dialogue dialog id.
	 */
	@XmlAttribute(name = "end_dialog_id")
	protected int endDialog;

	/**
	 * 中间各步 NPC 对话配置列表。
	 * Intermediate NPC dialogue step configs.
	 */
	@XmlElement(name = "npc_infos", required = true)
	protected List<NpcInfos> npcInfos;

	/**
	 * 注册 {@link ReportToMany} 模板；构建 NPC→配置映射并计算最大变量。
	 * Registers {@link ReportToMany}; builds NPC→info map and max variable.
	 *
	 * Quest engine
	 */
	@Override
	public void register(QuestEngine questEngine) {
		int maxVar = 0;
		Map<Integer, NpcInfos> NpcInfo = new LinkedHashMap<Integer, NpcInfos>();
		for (NpcInfos mi : npcInfos) {
			NpcInfo.put(mi.getNpcId(), mi);
			if (mi.getVar() > maxVar) {
				maxVar = mi.getVar();
			}
		}
		ReportToMany template = new ReportToMany(id, startItemId, startNpcIds, endNpcIds, NpcInfo, startDialog, endDialog, maxVar);
		questEngine.addQuestHandler(template);
	}
}
