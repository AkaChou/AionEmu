package com.aionemu.gameserver.commands.admin;

import com.aionemu.gameserver.questEngine.model.QuestStatus;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * `//quest set` 的状态参数必须大小写与空白容错，否则 GM 会看到“status is one of ...”却无从判断。
 * The {@code //quest set} status argument must tolerate case and whitespace; GM feedback used to be a
 * dead end when the token was typed lowercase.
 */
class QuestCommandArgumentTest {

	@Test
	void statusArgumentIsCaseAndWhitespaceTolerant() {
		Map<String, QuestStatus> accepted = new LinkedHashMap<>();
		accepted.put("START", QuestStatus.START);
		accepted.put("start", QuestStatus.START);
		accepted.put(" Start ", QuestStatus.START);
		accepted.put("none", QuestStatus.NONE);
		accepted.put("Reward", QuestStatus.REWARD);
		accepted.put("COMPLETE", QuestStatus.COMPLETE);
		accepted.put(" complete \t", QuestStatus.COMPLETE);
		accepted.forEach((raw, expected) ->
			assertEquals(expected, Quest.parseStatus(raw), "status token: '" + raw + "'"));
	}

	@Test
	void unknownStatusIsRejected() {
		assertNull(Quest.parseStatus(null));
		assertNull(Quest.parseStatus(""));
		assertNull(Quest.parseStatus("   "));
		assertNull(Quest.parseStatus("STAR"));
		assertNull(Quest.parseStatus("START 0"));
	}
}
