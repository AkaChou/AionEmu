package com.aionemu.gameserver.network.aion.clientpackets;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.aionemu.gameserver.questEngine.definition.QuestMetadata;
import com.aionemu.gameserver.questEngine.definition.RepeatPolicy;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

class CMQuestShareCanonicalMetadataTest {

	@Test
	void shareOfferFailsClosedAndUsesCanonicalRepeatAndLevelRules() {
		QuestMetadata once = metadata(false, new RepeatPolicy(1, 0, false, false));
		QuestMetadata repeatable = metadata(false, new RepeatPolicy(255, 0, false, false));
		QuestState started = state(QuestStatus.START);

		assertFalse(CM_QUEST_SHARE.canShare(null, started));
		assertFalse(CM_QUEST_SHARE.canShare(metadata(true, RepeatPolicy.once()), started));
		assertTrue(CM_QUEST_SHARE.canShare(once, started));
		assertFalse(CM_QUEST_SHARE.canReceiveByState(once, state(QuestStatus.COMPLETE)));
		assertTrue(CM_QUEST_SHARE.canReceiveByState(repeatable, state(QuestStatus.COMPLETE)));
		assertFalse(CM_QUEST_SHARE.canReceiveByState(repeatable, state(QuestStatus.REWARD)));
		assertFalse(CM_QUEST_SHARE.canReceiveByLevel(once, 9));
		assertTrue(CM_QUEST_SHARE.canReceiveByLevel(once, 10));
		assertTrue(CM_QUEST_SHARE.canReceiveByLevel(once, 20));
		assertFalse(CM_QUEST_SHARE.canReceiveByLevel(once, 21));
	}

	private static QuestState state(QuestStatus status) {
		return new QuestState(990401, status, 0, 0, null, null, null);
	}

	private static QuestMetadata metadata(boolean cannotShare, RepeatPolicy repeatPolicy) {
		return new QuestMetadata("share", 1, 10, 20, Set.of(), "QUEST", repeatPolicy,
			Set.of(), List.of(), List.of(), List.of(), Set.of(), "", 0, 1, 1,
			cannotShare, false, false, 0, null, null, false, Set.of(), 0,
			"NONE", "NONE", 0, List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), Map.of());
	}
}
