package com.aionemu.gameserver.questEngine.retail;

import java.io.InputStream;
import java.util.List;

/**
 * 测试夹具：真端对话名组表的生产资源流（每次调用新开流，避免重复消费）。
 * <p>
 * Test fixture: freshly opened streams of the production retail dialog-name group table, so every
 * index build sees the same declarations as production (no silent test/production divergence).
 */
public final class RetailQuestAiNameGroupsFixture {

	private static final String QUEST_AI_NAME_GROUPS =
		"/aion/data/static_data/quest_retail/retail-quest-ai-name-groups.tsv";

	private RetailQuestAiNameGroupsFixture() {
	}

	public static List<InputStream> streams() {
		InputStream input = RetailQuestAiNameGroupsFixture.class.getResourceAsStream(QUEST_AI_NAME_GROUPS);
		if (input == null) {
			throw new IllegalStateException("missing resource " + QUEST_AI_NAME_GROUPS);
		}
		return List.of(input);
	}
}
