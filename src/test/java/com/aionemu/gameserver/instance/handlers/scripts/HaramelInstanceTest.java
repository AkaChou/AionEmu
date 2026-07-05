package com.aionemu.gameserver.instance.handlers.scripts;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import org.junit.jupiter.api.Test;

class HaramelInstanceTest {

	@Test
	void sendMovieIgnoresMissingDamageOwner() throws Exception {
		HaramelInstance instance = new HaramelInstance();
		Method sendMovie = HaramelInstance.class.getDeclaredMethod("sendMovie", Player.class, int.class);
		sendMovie.setAccessible(true);

		Object sent = assertDoesNotThrow(() -> sendMovie.invoke(instance, null, 457));

		assertEquals(Boolean.FALSE, sent);
		assertTrue(movies(instance).isEmpty());
	}

	@SuppressWarnings("unchecked")
	private List<Integer> movies(HaramelInstance instance) throws Exception {
		Field field = HaramelInstance.class.getDeclaredField("movies");
		field.setAccessible(true);
		return (List<Integer>) field.get(instance);
	}
}
