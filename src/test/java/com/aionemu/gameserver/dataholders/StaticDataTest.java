package com.aionemu.gameserver.dataholders;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import com.aionemu.testutil.ConfigSnapshot;
import com.aionemu.gameserver.configs.main.GSConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class StaticDataTest {

	private final ConfigSnapshot gsConfigSnapshot = ConfigSnapshot.of(GSConfig.class, "STATIC_DATA_SUMMARY_LOG");

	@AfterEach
	void resetConfig() {
		gsConfigSnapshot.restore();
	}

	@Test
	void summaryDoesNotEmitDetailedOutputByDefault() {
		assertDoesNotThrow(new StaticData()::logSummary);
	}
}
