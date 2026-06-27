package com.aionemu.gameserver.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.aionemu.gameserver.model.gameobjects.PetAction;
import com.aionemu.gameserver.model.gameobjects.PetEmote;
import com.aionemu.gameserver.model.gameobjects.player.QuestStateList;
import com.aionemu.gameserver.model.house.HouseRegistry;
import com.aionemu.gameserver.model.house.MaintenanceTask;
import com.aionemu.gameserver.network.aion.serverpackets.SM_QUEST_COMPLETED_LIST;
import com.aionemu.gameserver.network.aion.serverpackets.SM_QUEST_LIST;
import com.aionemu.gameserver.model.templates.item.ItemEnchantTemplate;
import com.aionemu.gameserver.model.templates.towns.TownSpawn;
import com.aionemu.gameserver.model.templates.towns.TownSpawnMap;
import com.aionemu.gameserver.model.team2.common.events.TeamCommand;
import com.aionemu.gameserver.model.trade.Exchange;
import com.aionemu.gameserver.services.HousingService;

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

	@Test
	void housingRegistriesUseJdkCollectionInterfaces() throws Exception {
		assertEquals(Map.class, fieldType(HouseRegistry.class, "objects"));
		assertEquals(Map.class, fieldType(HouseRegistry.class, "customParts"));
		assertEquals(List.class, fieldType(MaintenanceTask.class, "maintainedHouses"));

		assertEquals(List.class, methodReturnType(HouseRegistry.class, "getObjects"));
		assertEquals(List.class, methodReturnType(HouseRegistry.class, "getSpawnedObjects"));
		assertEquals(List.class, methodReturnType(HouseRegistry.class, "getNotSpawnedObjects"));
		assertEquals(List.class, methodReturnType(HouseRegistry.class, "getCustomParts"));
		assertEquals(List.class, methodReturnType(HouseRegistry.class, "getDefaultParts"));
		assertEquals(List.class, methodReturnType(HouseRegistry.class, "getAllParts"));
		assertEquals(List.class, methodReturnType(HousingService.class, "getCustomHouses"));

		String houseObjectsPacket = Files
				.readString(Path.of("src/main/java/com/aionemu/gameserver/network/aion/serverpackets/SM_HOUSE_OBJECTS.java"));
		assertFalse(houseObjectsPacket.contains("FastList<HouseObject<?>>"));
	}

	@Test
	void playerQuestAndRewardTemporaryListsUseJdkLists() throws Exception {
		assertEquals(List.class, methodReturnType(QuestStateList.class, "getAllFinishedQuests"));
		assertEquals(List.class, fieldType(SM_QUEST_LIST.class, "questState"));
		assertEquals(List.class, constructorFirstParameterType(SM_QUEST_LIST.class));
		assertEquals(List.class, fieldType(SM_QUEST_COMPLETED_LIST.class, "allQuests"));
		assertEquals(List.class, constructorFirstParameterType(SM_QUEST_COMPLETED_LIST.class));

		assertSourceOmits("src/main/java/com/aionemu/gameserver/services/player/PlayerEnterWorldService.java",
				"FastList<QuestState>");
		assertSourceOmits("src/main/java/com/aionemu/gameserver/services/reward/RewardService.java",
				"FastList<Integer> rewarded");
		assertSourceOmits("src/main/java/com/aionemu/gameserver/model/gameobjects/Kisk.java",
				"new FastList<Player>()");
	}

	private Class<?> fieldType(Class<?> owner, String name) throws NoSuchFieldException {
		Field field = owner.getDeclaredField(name);
		return field.getType();
	}

	private Class<?> methodReturnType(Class<?> owner, String name) throws NoSuchMethodException {
		Method method = owner.getDeclaredMethod(name);
		return method.getReturnType();
	}

	private Class<?> constructorFirstParameterType(Class<?> owner) {
		for (Constructor<?> constructor : owner.getDeclaredConstructors()) {
			if (constructor.getParameterCount() > 0) {
				return constructor.getParameterTypes()[0];
			}
		}
		throw new AssertionError("No constructor with parameters found for " + owner.getName());
	}

	private void assertSourceOmits(String sourcePath, String text) throws Exception {
		String source = Files.readString(Path.of(sourcePath));
		assertFalse(source.contains(text));
	}
}
