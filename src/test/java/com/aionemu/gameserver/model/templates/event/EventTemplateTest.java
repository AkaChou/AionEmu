package com.aionemu.gameserver.model.templates.event;

import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

class EventTemplateTest {

	@Test
	void inventoryDropTasksDoNotDependOnNestedVisitorClasses() throws IOException {
		String bytecode = classBytes(EventTemplate.class);

		assertFalse(bytecode.contains("EventTemplate$1$1"));
		assertFalse(bytecode.contains("EventTemplate$2$1"));
	}

	private static String classBytes(Class<?> type) throws IOException {
		String resourceName = type.getSimpleName() + ".class";
		try (InputStream input = type.getResourceAsStream(resourceName)) {
			return new String(input.readAllBytes(), StandardCharsets.ISO_8859_1);
		}
	}
}
