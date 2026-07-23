package com.aionemu.gameserver.scriptEngine;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 脚本注册表：按 NPC ID / 任务 ID 绑定 {@link ScriptNpc} / {@link ScriptQuest}。
 * Script registry binding {@link ScriptNpc} / {@link ScriptQuest} by NPC id / quest id.
 *
 * <p>对应真端 ScriptDLL64 的脚本实例化表；由 {@link ScriptEngine} 在启动期从
 * {@code definitions} 加载绑定数据后填充，运行时供 AI / Quest / Instance 引擎查询，
 * 替代逐副本、逐任务手写的 Java 桥接。
 */
public final class ScriptRegistry {

	/** NPC ID -> 脚本 NPC / NPC id -> script NPC */
	private final Map<Integer, ScriptNpc> scriptNpcs = new ConcurrentHashMap<>();
	/** 任务 ID -> 脚本任务 / quest id -> script quest */
	private final Map<Integer, ScriptQuest> scriptQuests = new ConcurrentHashMap<>();

	/**
	 * 注册脚本 NPC；同一 NPC ID 重复注册时记录警告并覆盖。
	 * Register a script NPC; logs a warning and overwrites on duplicate NPC id.
	 *
	 * @param scriptNpc 脚本 NPC / script NPC
	 */
	public void registerScriptNpc(ScriptNpc scriptNpc) {
		if (scriptNpc != null) {
			scriptNpcs.put(scriptNpc.getNpcId(), scriptNpc);
		}
	}

	/**
	 * 按 NPC ID 查询脚本 NPC。
	 * Look up a script NPC by NPC id.
	 *
	 * @param npcId NPC 模板 ID / NPC template id
	 * @return 脚本 NPC；未绑定返回 {@code null} / script NPC, or {@code null}
	 */
	public ScriptNpc getScriptNpc(int npcId) {
		return scriptNpcs.get(npcId);
	}

	/**
	 * 注册脚本任务；同一任务 ID 重复注册时覆盖。
	 * Register a script quest; overwrites on duplicate quest id.
	 *
	 * @param scriptQuest 脚本任务 / script quest
	 */
	public void registerScriptQuest(ScriptQuest scriptQuest) {
		if (scriptQuest != null) {
			scriptQuests.put(scriptQuest.getQuestId(), scriptQuest);
		}
	}

	/**
	 * 按任务 ID 查询脚本任务。
	 * Look up a script quest by quest id.
	 *
	 * @param questId 任务 ID / quest id
	 * @return 脚本任务；未绑定返回 {@code null} / script quest, or {@code null}
	 */
	public ScriptQuest getScriptQuest(int questId) {
		return scriptQuests.get(questId);
	}

	/**
	 * 返回已注册脚本 NPC 数量。
	 * Return the number of registered script NPCs.
	 *
	 * @return 脚本 NPC 数 / script-NPC count
	 */
	public int scriptNpcCount() {
		return scriptNpcs.size();
	}

	/**
	 * 返回已注册脚本任务数量。
	 * Return the number of registered script quests.
	 *
	 * @return 脚本任务数 / script-quest count
	 */
	public int scriptQuestCount() {
		return scriptQuests.size();
	}

	/**
	 * 清空任务脚本绑定，保留 NPC 脚本。
	 * Clear quest-script bindings while retaining NPC scripts.
	 */
	public void clearScriptQuests() {
		scriptQuests.clear();
	}

	/**
	 * 清空全部注册项。
	 * Clear every registered binding.
	 */
	public void clear() {
		scriptNpcs.clear();
		clearScriptQuests();
	}
}
