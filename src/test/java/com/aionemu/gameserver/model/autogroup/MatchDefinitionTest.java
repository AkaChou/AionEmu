package com.aionemu.gameserver.model.autogroup;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.dataholders.RetailInstanceData;
import com.aionemu.gameserver.model.PlayerClass;

class MatchDefinitionTest {
	private static RetailInstanceData previous;

	@BeforeAll
	static void loadRetailData() {
		previous = DataManager.RETAIL_INSTANCE_DATA;
		DataManager.RETAIL_INSTANCE_DATA = RetailInstanceData.load(
				new File("src/main/resources/aion/definitions/compact/instance"),
				new File("src/main/resources/aion/definitions/schemas/retail-instance-data.xsd"));
	}

	@AfterAll
	static void restoreRetailData() {
		DataManager.RETAIL_INSTANCE_DATA = previous;
	}

	@Test
	void exposesCompleteRetailMatchmakerDefinitions() {
		assertEquals(159, MatchDefinition.all().size());
		assertNull(MatchDefinition.getByMaskId(349));
		assertNull(MatchDefinition.getByMaskId(408));
		assertTrue(MatchDefinition.all().stream().filter(definition -> !definition.isTournament())
				.allMatch(definition -> definition.getAutoInstance() != null));
	}

	@Test
	void resolvesRetailNpcAndCreationMappings() {
		MatchDefinition dredgion = MatchDefinition.forNpc(50, 279039);
		assertEquals(1, dredgion.getInstanceMaskId());
		assertEquals(61, dredgion.getCreationId());
		assertEquals(300110000, dredgion.getInstanceMapId());
		assertTrue(dredgion.getNpcIds().contains(279039));

		MatchDefinition steelRake = MatchDefinition.getByMaskId(10);
		assertEquals(63, steelRake.getCreationId());
		assertEquals(1, steelRake.getDifficultId());
	}

	@Test
	void usesPerSideRetailCapacityAndClassQuotas() {
		MatchDefinition kamar = MatchDefinition.getByMaskId(107);
		assertEquals(12, kamar.getPlayersPerSide());
		assertEquals(24, kamar.getPlayerSize());

		MatchDefinition bastion = MatchDefinition.getByMaskId(109);
		assertEquals(24, bastion.getPlayersPerSide());
		assertEquals(48, bastion.getPlayerSize());

		MatchDefinition dredgion = MatchDefinition.getByMaskId(1);
		assertEquals(1, dredgion.getRequiredPlayers(PlayerClass.CLERIC));
		assertEquals(1, dredgion.getMaximumPlayers(PlayerClass.TEMPLAR));
		assertEquals(2, dredgion.getMaximumPlayers(PlayerClass.GLADIATOR));
		assertEquals(1_800_000, dredgion.getAgeRequisiteMillis());
		assertEquals(420_000, dredgion.getAgeToleranceMillis());
		assertInstanceOf(AutoAsyunatarDredgionInstance.class,
				MatchDefinition.getByMaskId(121).getAutoInstance());
	}

	@Test
	void exposesRetailShuffleAndTeamMatchRules() {
		MatchDefinition discipline = MatchDefinition.getByMaskId(24);
		assertEquals(65535, discipline.getShuffleLimitSize());
		assertEquals(1, discipline.getShuffleMinimum());
		assertEquals(30_000, discipline.getShuffleIntervalMillis());
		assertEquals(302350000, MatchDefinition.teamMatch(1).requiredInt("world_id"));
		MatchDefinition teamMatch = MatchDefinition.getByMaskId(302350000);
		assertTrue(teamMatch.isTeamMatch());
		assertEquals(48, teamMatch.getPlayersPerSide());
		assertEquals(12, teamMatch.getMinimumPlayersPerSide());
		assertEquals(96, teamMatch.getPlayerSize());
		assertInstanceOf(AutoGeneralInstance.class, teamMatch.getAutoInstance());
	}

	@Test
	void evaluatesScheduleInChinaTime() {
		MatchDefinition dredgion = MatchDefinition.getByMaskId(1);
		ZonedDateTime chinaOpen = ZonedDateTime.of(2026, 7, 20, 20, 30, 0, 0,
				ZoneId.of("Asia/Shanghai"));
		assertTrue(dredgion.isOpen(chinaOpen.withZoneSameInstant(ZoneId.of("UTC"))));
		assertFalse(dredgion.isOpen(chinaOpen.minusHours(2)));
	}
}
