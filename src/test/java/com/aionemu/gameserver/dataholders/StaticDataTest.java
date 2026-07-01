package com.aionemu.gameserver.dataholders;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import java.lang.reflect.Method;

import com.aionemu.gameserver.configs.main.GSConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class StaticDataTest {

	@AfterEach
	void resetConfig() {
		GSConfig.STATIC_DATA_SUMMARY_LOG = false;
	}

	@Test
	void afterUnmarshalDoesNotEmitDetailedSummaryByDefault() throws Exception {
		Method afterUnmarshal = StaticData.class.getDeclaredMethod("afterUnmarshal", jakarta.xml.bind.Unmarshaller.class, Object.class);
		afterUnmarshal.setAccessible(true);

		assertDoesNotThrow(() -> afterUnmarshal.invoke(new StaticData(), null, null));
	}
}
