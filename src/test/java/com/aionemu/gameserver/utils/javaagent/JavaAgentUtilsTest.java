package com.aionemu.gameserver.utils.javaagent;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class JavaAgentUtilsTest {

	@Test
	void callbackSupportIsConfiguredWithoutStartupJavaagent() {
		assertTrue(JavaAgentUtils.isConfigured());
	}
}
