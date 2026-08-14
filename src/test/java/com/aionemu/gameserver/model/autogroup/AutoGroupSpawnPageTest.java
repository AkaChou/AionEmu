package com.aionemu.gameserver.model.autogroup;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Path;

import jakarta.xml.bind.JAXBContext;

import org.junit.jupiter.api.Test;

import com.aionemu.gameserver.dataholders.AutoGroupData;
import com.aionemu.gameserver.dataholders.DataManager;

class AutoGroupSpawnPageTest {

	@Test
	void mapsRetailArenaDefinitionsToSpawnPages() throws Exception {
		AutoGroupData previous = DataManager.AUTO_GROUP;
		try {
			DataManager.AUTO_GROUP = (AutoGroupData) JAXBContext.newInstance(AutoGroupData.class).createUnmarshaller()
					.unmarshal(Path.of("src/main/resources/aion/data/static_data/auto_group/auto_group.xml").toFile());

			assertEquals(1, AutoGroupType.ARENA_OF_CHAOS_46_60_1.getSpawnPage());
			assertEquals(11, AutoGroupType.ARENA_OF_CHAOS_46_60_2.getSpawnPage());
			assertEquals(21, AutoGroupType.ARENA_OF_CHAOS_46_60_3.getSpawnPage());
			assertEquals(21, AutoGroupType.ARENA_OF_GLORY_46_60_1.getSpawnPage());
			assertEquals(31, AutoGroupType.ARENA_OF_CHAOS_61_65_1.getSpawnPage());
			assertEquals(41, AutoGroupType.ARENA_OF_CHAOS_66_83_1.getSpawnPage());
		} finally {
			DataManager.AUTO_GROUP = previous;
		}
	}
}
