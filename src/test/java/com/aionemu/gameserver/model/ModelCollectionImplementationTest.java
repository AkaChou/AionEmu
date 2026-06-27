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

	@Test
	void modelAndServiceListFieldsAvoidFastListInitializers() throws Exception {
		assertSourceOmits("src/main/java/com/aionemu/gameserver/services/transfers/PlayerTransferService.java",
				"rsList = FastList.newInstance()");
		assertSourceOmits("src/main/java/com/aionemu/gameserver/services/drop/DropRegistrationService.java",
				"private FastList<Integer> noReductionMaps");
		assertSourceOmits("src/main/java/com/aionemu/gameserver/services/drop/DropRegistrationService.java",
				"noReductionMaps = new FastList<Integer>()");
		assertSourceOmits("src/main/java/com/aionemu/gameserver/model/autogroup/LookingForParty.java",
				"FastList<SearchInstance> tempList");
		assertSourceOmits("src/main/java/com/aionemu/gameserver/model/skill/PlayerSkillList.java",
				"FastList<Integer> linked");
		assertSourceOmits("src/main/java/com/aionemu/gameserver/model/gameobjects/player/Player.java",
				"new FastList<DisassembleItem>()");
	}

	@Test
	void commandTemporaryListsUseJdkLists() throws Exception {
		assertSourceOmits("src/main/java/com/aionemu/gameserver/commands/player/GiveStigma.java",
				"FastList<");
		assertSourceOmits("src/main/java/com/aionemu/gameserver/commands/player/GiveStigma.java",
				"FastList.Node");
		assertSourceOmits("src/main/java/com/aionemu/gameserver/commands/admin/SpawnAssembledNpc.java",
				"FastList<AssembledNpcPart>");
		assertSourceOmits("src/main/java/com/aionemu/gameserver/commands/admin/LegionCommand.java",
				"FastList<String>");
		assertSourceOmits("src/main/java/com/aionemu/gameserver/commands/admin/LegionCommand.java",
				"FastList.recycle");
		assertSourceOmits("src/main/java/com/aionemu/gameserver/services/siegeservice/BalaurAssaultService.java",
				"FastList<AssembledNpcPart>");
		assertSourceOmits("src/main/java/com/aionemu/gameserver/services/ProtectorConquerorService.java",
				"FastList<Player> kill");
		assertSourceOmits("src/main/java/com/aionemu/gameserver/network/loginserver/serverpackets/SM_PTRANSFER_CONTROL.java",
				"FastList<QuestState>");
		assertSourceOmits("src/main/java/com/aionemu/gameserver/network/loginserver/serverpackets/SM_PTRANSFER_CONTROL.java",
				"FastList.recycle(quests)");
		assertSourceOmits("src/main/java/com/aionemu/gameserver/services/transfers/CMT_CHARACTER_INFORMATION.java",
				"FastList<String> itemOut");
		assertSourceOmits("src/main/java/com/aionemu/gameserver/services/transfers/CMT_CHARACTER_INFORMATION.java",
				"FastList<int[]> manastones");
		assertSourceOmits("src/main/java/com/aionemu/gameserver/services/transfers/CMT_CHARACTER_INFORMATION.java",
				"FastList.recycle(itemOut)");
	}

	@Test
	void localUtilityListsUseJdkLists() throws Exception {
		assertSourceOmits("src/main/java/com/aionemu/gameserver/dao/mysql8/MySQL8InventoryDAO.java",
				"FastList.newInstance()");
		assertSourceOmits("src/main/java/com/aionemu/gameserver/geoEngine/collision/bih/BIHNode.java",
				"FastList<BIHStackData>");
		assertSourceOmits("src/main/java/com/aionemu/gameserver/geoEngine/collision/bih/BIHNode.java",
				"FastList.recycle(stack)");
	}

	@Test
	void instanceFutureTaskListsUseJdkLists() throws Exception {
		List<String> instanceScripts = List.of(
				"src/main/java/com/aionemu/gameserver/instance/handlers/scripts/idgelDome/IdgelDomeLandmarkInstance.java",
				"src/main/java/com/aionemu/gameserver/instance/handlers/scripts/idgelDome/IdgelDomeInstance.java",
				"src/main/java/com/aionemu/gameserver/instance/handlers/scripts/EmperorTrillirunerkSafeInstance.java",
				"src/main/java/com/aionemu/gameserver/instance/handlers/scripts/SealedArgentManorInstance.java",
				"src/main/java/com/aionemu/gameserver/instance/handlers/scripts/TalocsHollowInstance.java",
				"src/main/java/com/aionemu/gameserver/instance/handlers/scripts/IronWallWarfrontInstance.java",
				"src/main/java/com/aionemu/gameserver/instance/handlers/scripts/OphidanWarpathInstance.java",
				"src/main/java/com/aionemu/gameserver/instance/handlers/scripts/event/Event_AturamSkyFortressInstance.java",
				"src/main/java/com/aionemu/gameserver/instance/handlers/scripts/event/Opportunity_FissureOfOblivionInstance.java",
				"src/main/java/com/aionemu/gameserver/instance/handlers/scripts/event/Event_ContaminatedUnderpathInstance.java");
		for (String sourcePath : instanceScripts) {
			assertSourceOmits(sourcePath, "FastList<Future<?>>");
			assertSourceOmits(sourcePath, "FastList.Node<Future<?>>");
		}
	}

	@Test
	void additionalInstanceFutureTaskListsUseJdkLists() throws Exception {
		List<String> instanceScripts = List.of(
				"src/main/java/com/aionemu/gameserver/instance/handlers/scripts/TrialsOfEternityInstance.java",
				"src/main/java/com/aionemu/gameserver/instance/handlers/scripts/EngulfedOphidanBridgeInstance.java",
				"src/main/java/com/aionemu/gameserver/instance/handlers/scripts/ArenaOfTenacityInstance.java",
				"src/main/java/com/aionemu/gameserver/instance/handlers/scripts/DarkPoetaInstance.java",
				"src/main/java/com/aionemu/gameserver/instance/handlers/scripts/DivineTowerInstanceL.java",
				"src/main/java/com/aionemu/gameserver/instance/handlers/scripts/KumukiCaveInstance.java",
				"src/main/java/com/aionemu/gameserver/instance/handlers/scripts/AbyssalSplinterInstance.java",
				"src/main/java/com/aionemu/gameserver/instance/handlers/scripts/ShugoImperialTombInstance.java",
				"src/main/java/com/aionemu/gameserver/instance/handlers/scripts/DrakenseerLairInstance.java",
				"src/main/java/com/aionemu/gameserver/instance/handlers/scripts/AturamSkyFortressInstance.java");
		for (String sourcePath : instanceScripts) {
			assertSourceOmits(sourcePath, "FastList<Future<?>>");
			assertSourceOmits(sourcePath, "FastList.Node<Future<?>>");
		}
	}

	@Test
	void remainingSimpleInstanceFutureTaskListsUseJdkLists() throws Exception {
		List<String> instanceScripts = List.of(
				"src/main/java/com/aionemu/gameserver/instance/handlers/scripts/event/IDEvent_Def_HInstance.java",
				"src/main/java/com/aionemu/gameserver/instance/handlers/scripts/SmolderingFireTempleInstance.java",
				"src/main/java/com/aionemu/gameserver/instance/handlers/scripts/TheEternalBastionInstance.java",
				"src/main/java/com/aionemu/gameserver/instance/handlers/scripts/FissureOfOblivionInstance.java",
				"src/main/java/com/aionemu/gameserver/instance/handlers/scripts/HallOfTenacityInstance.java",
				"src/main/java/com/aionemu/gameserver/instance/handlers/scripts/UnstableAbyssalSplinterInstance.java",
				"src/main/java/com/aionemu/gameserver/instance/handlers/scripts/dredgionDefense/SanctumInstance.java",
				"src/main/java/com/aionemu/gameserver/instance/handlers/scripts/luna/ContaminatedUnderpathInstance.java",
				"src/main/java/com/aionemu/gameserver/instance/handlers/scripts/EvergaleCanyonInstance.java",
				"src/main/java/com/aionemu/gameserver/instance/handlers/scripts/dredgion/BaranathDredgion.java");
		for (String sourcePath : instanceScripts) {
			assertSourceOmits(sourcePath, "FastList<Future<?>>");
			assertSourceOmits(sourcePath, "FastList.Node<Future<?>>");
		}
	}

	@Test
	void finalSimpleInstanceFutureTaskListsUseJdkLists() throws Exception {
		List<String> instanceScripts = List.of(
				"src/main/java/com/aionemu/gameserver/instance/handlers/scripts/DivineTowerInstanceD.java",
				"src/main/java/com/aionemu/gameserver/instance/handlers/scripts/dredgion/ChantraDredgionInstance.java",
				"src/main/java/com/aionemu/gameserver/instance/handlers/scripts/dredgion/TerathDredgionInstance.java",
				"src/main/java/com/aionemu/gameserver/instance/handlers/scripts/BeshmundirTempleInstance.java",
				"src/main/java/com/aionemu/gameserver/instance/handlers/scripts/KamarBattlefieldInstance.java",
				"src/main/java/com/aionemu/gameserver/instance/handlers/scripts/FallenPoetaInstance.java",
				"src/main/java/com/aionemu/gameserver/instance/handlers/scripts/BastionOfSoulsInstance.java",
				"src/main/java/com/aionemu/gameserver/instance/handlers/scripts/TheShugoEmperorVaultInstance.java",
				"src/main/java/com/aionemu/gameserver/instance/handlers/scripts/dredgionDefense/PandaemoniumInstance.java",
				"src/main/java/com/aionemu/gameserver/instance/handlers/scripts/dredgion/AshunatalDredgionInstance.java");
		for (String sourcePath : instanceScripts) {
			assertSourceOmits(sourcePath, "FastList<Future<?>>");
			assertSourceOmits(sourcePath, "FastList.Node<Future<?>>");
		}
	}

	@Test
	void multiListInstanceFutureTaskListsUseJdkLists() throws Exception {
		List<String> instanceScripts = List.of(
				"src/main/java/com/aionemu/gameserver/instance/handlers/scripts/luna/SecretMunitionsFactoryInstance.java",
				"src/main/java/com/aionemu/gameserver/instance/handlers/scripts/illuminaryObelisk/IlluminaryObeliskInstance.java",
				"src/main/java/com/aionemu/gameserver/instance/handlers/scripts/illuminaryObelisk/Infernal_IlluminaryObeliskInstance.java",
				"src/main/java/com/aionemu/gameserver/instance/handlers/scripts/StonespearReachInstance.java",
				"src/main/java/com/aionemu/gameserver/instance/handlers/scripts/crucible/CrucibleSpireInstance.java");
		for (String sourcePath : instanceScripts) {
			assertSourceOmits(sourcePath, "FastList<Future<?>>");
			assertSourceOmits(sourcePath, "FastList.Node<Future<?>>");
		}
		assertSourceOmits("src/main/java/com/aionemu/gameserver/instance/handlers/scripts/StonespearReachInstance.java",
				"FastList<Future<?>> taskList");
		assertSourceOmits("src/main/java/com/aionemu/gameserver/instance/handlers/scripts/StonespearReachInstance.java",
				"private FastList<Future<?>> getTaskListForRound");
	}

	@Test
	void templateLocalListsUseJdkLists() throws Exception {
		List<String> templateSources = List.of(
				"src/main/java/com/aionemu/gameserver/model/templates/gather/ExMaterials.java",
				"src/main/java/com/aionemu/gameserver/model/templates/windstreams/StreamLocations.java",
				"src/main/java/com/aionemu/gameserver/model/templates/goods/GoodsList.java",
				"src/main/java/com/aionemu/gameserver/model/templates/spawns/SpawnTemplate.java");
		for (String sourcePath : templateSources) {
			assertSourceOmits(sourcePath, "FastList");
			assertSourceOmits(sourcePath, "javolution.util");
		}
	}

	@Test
	void dataholderAndShopListsUseJdkLists() throws Exception {
		List<String> sourcePaths = List.of(
				"src/main/java/com/aionemu/gameserver/dataholders/LunaData.java",
				"src/main/java/com/aionemu/gameserver/dataholders/RecipeData.java",
				"src/main/java/com/aionemu/gameserver/services/player/LunaShopService.java",
				"src/main/java/com/aionemu/gameserver/model/ingameshop/InGameShopEn.java",
				"src/main/java/com/aionemu/gameserver/network/aion/serverpackets/SM_IN_GAME_SHOP_LIST.java");
		for (String sourcePath : sourcePaths) {
			assertSourceOmits(sourcePath, "FastList");
		}
		assertSourceOmits("src/main/java/com/aionemu/gameserver/network/aion/serverpackets/SM_IN_GAME_SHOP_LIST.java",
				"FastList.recycle");
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
