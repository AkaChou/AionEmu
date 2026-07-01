package com.aionemu.gameserver.model.gameobjects.player.title;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

class TitleListTest {

	@Test
	void storesTitlesInJdkMap() throws Exception {
		TitleList list = new TitleList();

		Map<Integer, Title> titles = titles(list);

		assertEquals(HashMap.class, titles.getClass());
	}

	@SuppressWarnings("unchecked")
	private Map<Integer, Title> titles(TitleList list) throws Exception {
		Field field = TitleList.class.getDeclaredField("titles");
		field.setAccessible(true);
		return (Map<Integer, Title>) field.get(list);
	}
}
