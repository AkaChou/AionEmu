package com.aionemu.gameserver.persistence.dao.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.ibatis.annotations.Insert;
import org.junit.jupiter.api.Test;

import com.aionemu.gameserver.persistence.mapper.LimitedQuestCounterMapper;

/**
 * 名额计数 SQL 契约测试。
 * 列自减与 LEAST 上限改由 Flex 更新链表达；只有 {@code INSERT IGNORE} 没有 Flex 等价物，
 * 仍以注解 SQL 保留。
 * SQL contract tests for the limited-quest counter statements. The column self-decrement and the LEAST
 * cap are now expressed with the Flex update chain; only {@code INSERT IGNORE} has no Flex equivalent
 * and stays as annotated SQL.
 */
class LimitedQuestDAOSqlTest {

	@Test
	void initializationDoesNotOverwritePersistedRemainingCount() throws Exception {
		assertTrue(initializeSql().startsWith("INSERT IGNORE"));
	}

	@Test
	void initializeIsTheOnlyAnnotatedStatementLeft() throws Exception {
		assertEquals(1, LimitedQuestCounterMapper.class.getDeclaredMethods().length);
	}

	@Test
	void acquisitionOnlyReportsRowsThatWereActuallyDecremented() throws IOException {
		String dao = daoSource();

		assertTrue(dao.contains(".set(remaining, remaining.subtract(1))"));
		assertTrue(dao.contains(".and(remaining.gt(0))"));
		assertFalse(dao.contains("PreparedStatement"));
	}

	@Test
	void recoveryIsCappedInsideTheDatabaseStatement() throws IOException {
		assertTrue(daoSource().contains("new FunctionQueryColumn(\"LEAST\", remaining.add(amount)"));
	}

	private static String initializeSql() throws Exception {
		return String.join("", LimitedQuestCounterMapper.class
			.getMethod("initialize", int.class, int.class).getAnnotation(Insert.class).value());
	}

	private static String daoSource() throws IOException {
		return Files.readString(Path.of(
			"src/main/java/com/aionemu/gameserver/persistence/dao/impl/LimitedQuestCounterDaoImpl.java"));
	}
}
