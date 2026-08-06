package com.aionemu.gameserver.services.drop;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import com.aionemu.gameserver.configs.main.DropConfig;
import com.aionemu.gameserver.model.drop.DropModifiers;
import com.aionemu.gameserver.model.items.ItemId;
import com.aionemu.gameserver.model.templates.globaldrops.GlobalRule;

class DropRegistrationServiceTest {
	@Test
	void bracketsAutomaticLootWithStartAndStopPackets() throws Exception {
		String source = Files.readString(Path.of("src/main/java/com/aionemu/gameserver/services/drop/DropRegistrationService.java"));
		String autoLoot = source.substring(source.indexOf("if (player.getMinion() != null"),
				source.indexOf("public DropModifiers createDropModifiers"));

		int start = autoLoot.indexOf("new SM_MINIONS(8, 1, npcObjId, true)");
		int stop = autoLoot.indexOf("new SM_MINIONS(8, 1, npcObjId, false)");
		assertTrue(start >= 0 && stop > start);
	}

	@Test
	void reductionHonorsGlobalMapAndLevelOneChestExemptions() {
		boolean originalDisabled = DropConfig.DISABLE_DROP_REDUCTION;
		String originalMaps = DropConfig.DISABLE_DROP_REDUCTION_IN_ZONES;
		try {
			DropConfig.DISABLE_DROP_REDUCTION = false;
			DropConfig.DISABLE_DROP_REDUCTION_IN_ZONES = "0, 42,,";
			DropRegistrationService service = new DropRegistrationService();

			assertNull(service.getReductionDropRate(1, 20, 42, false));
			assertNull(service.getReductionDropRate(1, 20, 100, true));
			assertEquals(0f, service.getReductionDropRate(1, 20, 100, false));

			DropConfig.DISABLE_DROP_REDUCTION = true;
			assertNull(service.getReductionDropRate(1, 20, 100, false));
		} finally {
			DropConfig.DISABLE_DROP_REDUCTION = originalDisabled;
			DropConfig.DISABLE_DROP_REDUCTION_IN_ZONES = originalMaps;
		}
	}

	@Test
	void globalDropsDoNotExcludeChestsLowLevelOrAbyssNpcsAndReplaceExistingKinah() throws Exception {
		String source = Files.readString(Path.of("src/main/java/com/aionemu/gameserver/services/drop/DropRegistrationService.java"));
		String globalDrops = source.substring(source.indexOf("int registerGlobalDrops"),
				source.indexOf("private Player initDropNpc"));

		assertFalse(globalDrops.contains("getAi2()"));
		assertFalse(globalDrops.contains("getAbyssNpcType()"));
		assertFalse(globalDrops.contains("npc.getLevel() > 1"));
		assertTrue(globalDrops.contains("boolean kinahRegistered = false"));
		assertTrue(globalDrops.contains("droppedItems.removeIf"));
	}

	@Test
	void legacySoloAndTeamKinahDeliveryPathsAreRemoved() throws Exception {
		String soloRewards = Files.readString(Path.of("src/main/java/com/aionemu/gameserver/controllers/NpcController.java"));
		String teamRewards = Files.readString(
				Path.of("src/main/java/com/aionemu/gameserver/model/team2/common/service/PlayerTeamDistributionService.java"));
		String customConfig = Files.readString(Path.of("src/main/java/com/aionemu/gameserver/configs/main/CustomConfig.java"));

		assertFalse(soloRewards.contains("AUTO_KINAH_ENABLED"));
		assertFalse(teamRewards.contains("AUTO_KINAH_ENABLED"));
		assertFalse(soloRewards.contains("increaseKinah("));
		assertFalse(teamRewards.contains("increaseKinah("));
		assertFalse(customConfig.contains("gameserver.auto.kinah.enabled"));
	}

	@Test
	void guaranteedKinahChanceIgnoresOrdinaryBoostAndLevelReduction() {
		GlobalRule rule = new GlobalRule();
		rule.setChance(100f);
		DropModifiers modifiers = new DropModifiers();
		modifiers.setBoostDropRate(0.25f);
		modifiers.setReductionDropRate(0f);

		DropRegistrationService service = new DropRegistrationService();
		assertEquals(100f, service.calculateGlobalDropChance(rule, ItemId.KINAH.value(), modifiers));
		assertEquals(0f, service.calculateGlobalDropChance(rule, 1, modifiers));
	}
}
