package com.aionemu.gameserver.questEngine.runtime;

import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.services.QuestService;

import java.util.List;

/** Production lifecycle cleanup for resources owned by typed quest actions. */
public final class QuestRuntimeResources {
	private QuestRuntimeResources() {
	}

	public static void cleanupQuest(int playerId, int questId) {
		delete(QuestSpawnRegistry.global().cleanup(playerId, questId));
		QuestService.cleanupQuestTimers(playerId, questId);
	}

	public static void cleanupPlayer(int playerId) {
		delete(QuestSpawnRegistry.global().cleanupPlayer(playerId));
		QuestService.cleanupPlayerQuestTimers(playerId);
	}

	public static void cleanupInstance(int instanceId) {
		delete(QuestSpawnRegistry.global().cleanupInstance(instanceId));
		QuestService.cleanupInstanceQuestTimers(instanceId);
	}

	public static void cleanupAll() {
		delete(QuestSpawnRegistry.global().cleanupAll());
		QuestService.cleanupAllQuestTimers();
	}

	private static void delete(List<Npc> npcs) {
		for (Npc npc : npcs) {
			if (npc != null && npc.isSpawned()) {
				npc.getController().onDelete();
			}
		}
	}
}
