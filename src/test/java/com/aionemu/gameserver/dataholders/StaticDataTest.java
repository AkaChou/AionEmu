package com.aionemu.gameserver.dataholders;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import com.aionemu.gameserver.configs.main.GSConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class StaticDataTest {

	@AfterEach
	void resetConfig() {
		GSConfig.STATIC_DATA_SUMMARY_LOG = false;
	}

	@Test
	void summaryDoesNotEmitDetailedOutputByDefault() {
		assertDoesNotThrow(new StaticData()::logSummary);
	}
}
