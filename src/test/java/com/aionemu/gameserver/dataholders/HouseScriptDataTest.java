package com.aionemu.gameserver.dataholders;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class HouseScriptDataTest {

	@Test
	void xmlFormatterFormatsFragmentsWithoutXercesSerializer() {
		String formatted = HouseScriptData.XmlFormatter.format("<lboxes><lbox id=\"1\" icon=\"2\"/></lboxes>");

		assertTrue(formatted.contains("<lboxes>"));
		assertTrue(formatted.contains("  <lbox"));
		assertTrue(formatted.contains("id=\"1\""));
		assertTrue(formatted.contains("icon=\"2\""));
	}
}
