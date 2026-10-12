package com.aionemu.gameserver.persistence.dao.impl;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Update;
import org.junit.jupiter.api.Test;

import com.aionemu.gameserver.persistence.mapper.LimitedQuestCounterMapper;

/**
 * 名额计数 SQL 契约测试。
 * SQL contract tests for the limited-quest counter statements.
 */
class LimitedQuestDAOSqlTest {

	@Test
	void acquisitionOnlyReportsRowsThatWereActuallyDecremented() throws Exception {
		String acquire = sql(Update.class, "acquire", int.class);

		assertTrue(acquire.contains("SET `remaining` = `remaining` - 1"));
		assertTrue(acquire.contains("AND `remaining` > 0"));
		assertFalse(acquire.contains("IF("));
	}

	@Test
	void recoveryIsCappedInsideTheDatabaseStatement() throws Exception {
		String recover = sql(Update.class, "recover", int.class, int.class, int.class);

		assertTrue(recover.contains("LEAST(`remaining` + #{amount}, #{maxCount})"));
	}

	@Test
	void initializationDoesNotOverwritePersistedRemainingCount() throws Exception {
		String initialize = sql(Insert.class, "initialize", int.class, int.class);

		assertTrue(initialize.startsWith("INSERT IGNORE"));
	}

	private static String sql(Class<? extends java.lang.annotation.Annotation> annotation, String method,
			Class<?>... parameterTypes) throws Exception {
		return String.join("", (String[]) annotation.getMethod("value").invoke(
			LimitedQuestCounterMapper.class.getMethod(method, parameterTypes).getAnnotation(annotation)));
	}
}
