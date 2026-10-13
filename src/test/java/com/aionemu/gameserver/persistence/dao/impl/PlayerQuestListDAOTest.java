package com.aionemu.gameserver.persistence.dao.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

/**
 * 任务奖励参数绑定契约测试。
 * 旧实现以 {@code PreparedStatement} 代理断言 {@code setNull(Types.INTEGER)}；映射层改为 Flex 更新链后，
 * 「未选奖励写 NULL」由 nullable 的 {@code getRewardOrNull()} 直接交给更新链表达——若误用带 0 兜底的
 * {@code getReward()}，未选奖励会被持久化成 0。
 * Contract tests for quest-reward parameter binding. The legacy test asserted
 * {@code setNull(Types.INTEGER)} through a {@code PreparedStatement} proxy; with the Flex update chain the
 * "unselected reward writes NULL" behaviour is expressed by handing the nullable
 * {@code getRewardOrNull()} straight to the chain — using the 0-fallback {@code getReward()} instead would
 * persist an unselected reward as 0.
 */
class PlayerQuestListDAOTest {

	@Test
	void unselectedRewardStaysNullForPersistence() {
		QuestState state = new QuestState(1101, QuestStatus.START, 0, 0, null, null, null);

		assertNull(state.getRewardOrNull());
	}

	@Test
	void selectedRewardKeepsItsIndexForPersistence() {
		QuestState state = new QuestState(1101, QuestStatus.COMPLETE, 0, 1, null, 3, null);

		assertEquals(3, state.getRewardOrNull());
	}

	@Test
	void rewardColumnIsBoundFromTheNullableAccessor() throws IOException {
		String dao = Files.readString(Path.of(
			"src/main/java/com/aionemu/gameserver/persistence/dao/impl/PlayerQuestDaoImpl.java"));

		assertTrue(dao.contains(".set(PlayerQuestPo::getReward, qs.getRewardOrNull())"));
		assertFalse(dao.contains("qs.getReward()"));
	}
}
