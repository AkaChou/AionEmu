package com.aionemu.gameserver.services;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.objenesis.ObjenesisStd;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.gameobjects.player.QuestStateList;
import com.aionemu.gameserver.questEngine.definition.QuestCatalogDrop;
import com.aionemu.gameserver.questEngine.definition.QuestDropScope;
import com.aionemu.gameserver.questEngine.definition.QuestMetadata;
import com.aionemu.gameserver.questEngine.definition.RepeatPolicy;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

class QuestServiceCanonicalMetadataTest {

	@Test
	void handlerSideDropWithoutCanonicalMetadataFailsClosed() {
		int questId = 990201;
		Player player = new ObjenesisStd().newInstance(Player.class);
		QuestStateList states = new QuestStateList();
		states.addQuest(questId, new QuestState(questId, QuestStatus.START, 0, 0,
			null, null, null));
		player.setQuestStateList(states);
		QuestCatalogDrop drop = new QuestCatalogDrop(questId, 210133, 182200205, 100,
			QuestDropScope.NONE, 0, 0, Optional.empty());

		assertFalse(QuestService.isQuestDrop(player, drop));
	}

	@Test
	void abandonRequiresCanonicalMetadataAndHonorsCannotGiveup() {
		QuestState state = new QuestState(990202, QuestStatus.START, 0, 0, null, null, null);

		assertFalse(QuestService.canAbandon(null, state));
		assertFalse(QuestService.canAbandon(metadata(true), state));
		assertFalse(QuestService.canAbandon(metadata(false), null));
		assertTrue(QuestService.canAbandon(metadata(false), state));
	}

	private static QuestMetadata metadata(boolean cannotGiveup) {
		return new QuestMetadata("abandon", 1, 1, 80, Set.of(), "QUEST", RepeatPolicy.once(),
			Set.of(), List.of(), List.of(), List.of(), Set.of(), "", 0, 1, 1,
			false, cannotGiveup, false, 0, null, null, false, Set.of(), 0,
			"NONE", "NONE", 0, List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), Map.of());
	}
}
