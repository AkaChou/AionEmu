package com.aionemu.gameserver.model.gameobjects.player.npcFaction;

import static com.aionemu.gameserver.questEngine.definition.QuestDsl.bitField;
import static com.aionemu.gameserver.questEngine.definition.QuestDsl.project;
import static com.aionemu.gameserver.questEngine.definition.QuestDsl.vars;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.aionemu.gameserver.questEngine.definition.ImmutableQuestCatalog;
import com.aionemu.gameserver.questEngine.definition.PersistenceMode;
import com.aionemu.gameserver.questEngine.definition.QuestDsl;
import com.aionemu.gameserver.questEngine.definition.QuestEvent;
import com.aionemu.gameserver.questEngine.definition.QuestMetadata;
import com.aionemu.gameserver.questEngine.definition.RepeatPolicy;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

class NpcFactionsCanonicalCatalogTest {

	@Test
	void dailyCandidatePoolUsesCanonicalFactionOwnerEligibilityAndWeekdayFilters() {
		var catalog = new ImmutableQuestCatalog(List.of(
			definition(9004, 4),
			definition(9002, 4),
			definition(9003, 5),
			definition(9005, 4),
			definition(9001, 4)));

		List<Integer> candidates = NpcFactions.canonicalDailyQuestCandidates(catalog, 4,
			id -> id != 9004,
			id -> id != 9001,
			id -> id != 9005);

		assertEquals(List.of(9002), candidates);
	}

	private static com.aionemu.gameserver.questEngine.definition.CompiledQuestDefinition definition(
			int questId, int npcFactionId) {
		return QuestDsl.quest(questId)
			.metadata(factionMetadata(questId, npcFactionId))
			.progress(bitField("var0", 0, 6, PersistenceMode.PERSISTENT))
			.node("start", project(QuestStatus.START, vars("var0", 0)))
			.on(new QuestEvent.LevelUp()).from("start").goTo("start")
			.compile();
	}

	private static QuestMetadata factionMetadata(int questId, int npcFactionId) {
		return new QuestMetadata("Faction " + questId, questId, 1, 80, Set.of(), "FACTION",
			RepeatPolicy.once(), Set.of(), List.of(), List.of(), List.of(), Set.of(), "", 0, 1, 1,
			false, false, false, 0, null, null, false, Set.of(), npcFactionId,
			"NONE", "NONE", 0, List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), Map.of());
	}
}
