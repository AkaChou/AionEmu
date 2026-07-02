package com.aionemu.gameserver.model.templates.event;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;

import javax.xml.transform.stream.StreamSource;

import jakarta.xml.bind.JAXBContext;

import org.junit.jupiter.api.Test;

class EventTemplateTest {

	@Test
	void inventoryDropTasksDoNotDependOnNestedVisitorClasses() throws IOException {
		String bytecode = classBytes(EventTemplate.class);

		assertFalse(bytecode.contains("EventTemplate$1$1"));
		assertFalse(bytecode.contains("EventTemplate$2$1"));
	}

	@Test
	void unmarshalsMultipleInventoryDrops() throws Exception {
		String xml = """
			<event name="Dafarunerk Inventory" start="2020-01-01T00:00:00+00:00" end="2050-12-31T00:00:00+00:00">
				<inventory_drop startlevel="10" interval="30" count="100">186000317</inventory_drop>
				<inventory_drop startlevel="10" interval="60" count="1">164000421</inventory_drop>
			</event>
			""";

		EventTemplate event = JAXBContext.newInstance(EventTemplate.class).createUnmarshaller()
			.unmarshal(new StreamSource(new StringReader(xml)), EventTemplate.class).getValue();

		assertEquals(2, event.inventoryDrops.size());
		assertEquals(186000317, event.inventoryDrops.get(0).getDropItem());
		assertEquals(164000421, event.inventoryDrops.get(1).getDropItem());
	}

	private static String classBytes(Class<?> type) throws IOException {
		String resourceName = type.getSimpleName() + ".class";
		try (InputStream input = type.getResourceAsStream(resourceName)) {
			return new String(input.readAllBytes(), StandardCharsets.ISO_8859_1);
		}
	}
}
