package com.aionemu.gameserver.services;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import com.aionemu.gameserver.controllers.attack.KillList;
import org.junit.jupiter.api.Test;

class PvpServiceTest {

	@Test
	void storesKillListsInJdkMap() throws Exception {
		PvpService service = new PvpService();

		Map<Integer, KillList> killLists = killLists(service);

		assertEquals(HashMap.class, killLists.getClass());
	}

	@SuppressWarnings("unchecked")
	private Map<Integer, KillList> killLists(PvpService service) throws Exception {
		Field field = PvpService.class.getDeclaredField("pvpKillLists");
		field.setAccessible(true);
		return (Map<Integer, KillList>) field.get(service);
	}
}
