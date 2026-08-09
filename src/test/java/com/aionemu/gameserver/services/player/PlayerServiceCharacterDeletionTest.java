package com.aionemu.gameserver.services.player;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import com.aionemu.commons.configuration.Property;
import com.aionemu.gameserver.configs.main.GSConfig;
import org.junit.jupiter.api.Test;

class PlayerServiceCharacterDeletionTest {

	@Test
	void definesTenMinuteDefaultInConfigAndProperties() throws NoSuchFieldException, IOException {
		Property property = GSConfig.class.getField("CHARACTER_DELETE_DELAY_MINUTES").getAnnotation(Property.class);
		assertEquals("gameserver.character.delete.delay.minutes", property.key());
		assertEquals("10", property.defaultValue());

		Properties properties = new Properties();
		Path path = Path.of("src/main/resources/aion/config/main/gameserver.properties");
		try (InputStream input = Files.newInputStream(path)) {
			properties.load(input);
		}
		assertEquals("10", properties.getProperty("gameserver.character.delete.delay.minutes"));
	}

	@Test
	void convertsConfiguredMinutesToDeletionDelay() {
		int savedDelay = GSConfig.CHARACTER_DELETE_DELAY_MINUTES;
		try {
			GSConfig.CHARACTER_DELETE_DELAY_MINUTES = 10;
			assertEquals(TimeUnit.MINUTES.toMillis(10), PlayerService.getCharacterDeletionDelayMillis());

			GSConfig.CHARACTER_DELETE_DELAY_MINUTES = 3;
			assertEquals(TimeUnit.MINUTES.toMillis(3), PlayerService.getCharacterDeletionDelayMillis());

			GSConfig.CHARACTER_DELETE_DELAY_MINUTES = -1;
			assertEquals(0, PlayerService.getCharacterDeletionDelayMillis());
		} finally {
			GSConfig.CHARACTER_DELETE_DELAY_MINUTES = savedDelay;
		}
	}
}
