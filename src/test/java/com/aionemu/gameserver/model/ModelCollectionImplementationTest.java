package com.aionemu.gameserver.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.aionemu.gameserver.model.gameobjects.PetAction;
import com.aionemu.gameserver.model.gameobjects.PetEmote;
import com.aionemu.gameserver.model.templates.item.ItemEnchantTemplate;
import com.aionemu.gameserver.model.templates.towns.TownSpawn;
import com.aionemu.gameserver.model.templates.towns.TownSpawnMap;
import com.aionemu.gameserver.model.team2.common.events.TeamCommand;
import com.aionemu.gameserver.model.trade.Exchange;

class ModelCollectionImplementationTest {

	@Test
	void enumLookupCachesUseJdkMapInterfaces() throws Exception {
		assertEquals(Map.class, fieldType(TeamCommand.class, "teamCommands"));
		assertEquals(Map.class, fieldType(PetAction.class, "petActions"));
		assertEquals(Map.class, fieldType(PetEmote.class, "petEmotes"));

		assertEquals(TeamCommand.GROUP_BAN_MEMBER, TeamCommand.getCommand(2));
		assertEquals(PetAction.SPAWN, PetAction.getActionById(3));
		assertEquals(PetAction.UNKNOWN, PetAction.getActionById(-1));
		assertEquals(PetEmote.ALARM, PetEmote.getEmoteById(-114));
		assertEquals(PetEmote.UNKNOWN, PetEmote.getEmoteById(404));
	}

	@Test
	void townAndEnchantIndexesUseJdkMapInterfaces() throws Exception {
		assertEquals(Map.class, fieldType(TownSpawn.class, "townLevelsData"));
		assertEquals(Map.class, fieldType(TownSpawnMap.class, "townSpawnsData"));
		assertEquals(Map.class, fieldType(ItemEnchantTemplate.class, "enchants"));
	}

	@Test
	void exchangeTracksItemsToUpdateInJdkList() {
		Exchange exchange = new Exchange(null, null);

		List<?> itemsToUpdate = exchange.getItemsToUpdate();

		assertInstanceOf(ArrayList.class, itemsToUpdate);
	}

	private Class<?> fieldType(Class<?> owner, String name) throws NoSuchFieldException {
		Field field = owner.getDeclaredField(name);
		return field.getType();
	}
}
