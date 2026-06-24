package com.aionemu.gameserver.services;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

class BrokerServiceBytecodeTest {

	@Test
	void brokerServiceDoesNotDependOnCompilerGeneratedRaceSwitchMap() throws IOException {
		InputStream brokerServiceClass = ClassLoader.getSystemResourceAsStream(
				"com/aionemu/gameserver/services/BrokerService.class");

		assertNotNull(brokerServiceClass);

		String classBytes = new String(brokerServiceClass.readAllBytes(), StandardCharsets.ISO_8859_1);

		assertFalse(classBytes.contains("BrokerService$2"));
	}
}
