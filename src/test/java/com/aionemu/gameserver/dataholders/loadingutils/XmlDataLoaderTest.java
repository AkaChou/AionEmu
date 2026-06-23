package com.aionemu.gameserver.dataholders.loadingutils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.aionemu.gameserver.dataholders.StaticData;
import org.junit.jupiter.api.Test;

class XmlDataLoaderTest {

	@Test
	void staticDataSectionNameIsOnlyReportedForTopLevelStaticDataChildren() {
		Object section = new Object();

		assertEquals("Object", XmlDataLoader.staticDataSectionName(section, new StaticData()));
		assertNull(XmlDataLoader.staticDataSectionName(section, new Object()));
		assertNull(XmlDataLoader.staticDataSectionName(null, new StaticData()));
	}
}
