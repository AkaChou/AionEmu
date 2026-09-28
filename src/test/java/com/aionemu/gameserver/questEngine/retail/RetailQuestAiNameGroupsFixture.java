package com.aionemu.gameserver.questEngine.retail;

import java.io.InputStream;
import java.util.List;

/**
 * 测试夹具：真端对话名组表的生产资源流（直接复用内存规范视图）。
 * <p>
 * Test fixture: streams of the production retail dialog-name group canonical view.
 */
public final class RetailQuestAiNameGroupsFixture {

	private RetailQuestAiNameGroupsFixture() {
	}

	public static List<InputStream> streams() {
		return RetailQuestAiNameGroups.streams();
	}
}
