package com.aionemu.gameserver.model.gameobjects.player;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.aionemu.gameserver.questEngine.definition.QuestMetadata;
import com.aionemu.gameserver.questEngine.definition.RepeatPolicy;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

class QuestStateListCanonicalMetadataTest {

	@Test
	void normalQuestCountUsesCanonicalCategoryAndIgnoresUnknownMetadata() {
		Map<Integer, QuestMetadata> metadata = Map.of(
			1, metadata("normal", "QUEST"),
			2, metadata("task", "TASK"));
		QuestStateList states = new QuestStateList(metadata::get);
		states.addQuest(1, state(1, QuestStatus.START));
		states.addQuest(2, state(2, QuestStatus.START));
		states.addQuest(3, state(3, QuestStatus.START));
		states.addQuest(4, state(4, QuestStatus.COMPLETE));

		assertEquals(List.of(1), states.getNormalQuests().stream().map(QuestState::getQuestId).toList());
	}

	private static QuestState state(int questId, QuestStatus status) {
		return new QuestState(questId, status, 0, 0, null, null, null);
	}

	private static QuestMetadata metadata(String name, String category) {
		return new QuestMetadata(name, 0, 1, 80, Set.of(), category, RepeatPolicy.once(),
			Set.of(), List.of(), List.of(), List.of());
	}
}
