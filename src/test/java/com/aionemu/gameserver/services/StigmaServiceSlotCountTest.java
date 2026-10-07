package com.aionemu.gameserver.services;

import com.aionemu.gameserver.questEngine.definition.QuestDefinitionXmlCompiler;
import com.aionemu.gameserver.questEngine.definition.QuestMetadata;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 烙印槽位数量的纯计算回归：等级档位、教学任务资格与会员权限。 / Regression for the pure stigma slot-count computation: level tiers, tutorial entitlement, and membership. */
class StigmaServiceSlotCountTest {

	@Test
	void belowLevelTwentyNoSlotsOpenEvenWhenEntitled() {
		assertEquals(0, StigmaService.stigmaSlotCount(19, false, true));
		assertEquals(0, StigmaService.stigmaSlotCount(1, true, true));
	}

	@Test
	void unmetTutorialEntitlementKeepsRegularSlotsClosed() {
		assertEquals(0, StigmaService.stigmaSlotCount(20, false, false));
		assertEquals(0, StigmaService.stigmaSlotCount(80, false, false));
	}

	@Test
	void membershipPerkUnlocksSevenSlotsWithoutTheTutorial() {
		assertEquals(7, StigmaService.stigmaSlotCount(20, true, false));
		assertEquals(7, StigmaService.stigmaSlotCount(80, true, false));
	}

	@Test
	void entitledPlayersOpenSlotsAtTheRetailLevelTiers() {
		assertEquals(2, StigmaService.stigmaSlotCount(20, false, true));
		assertEquals(2, StigmaService.stigmaSlotCount(29, false, true));
		assertEquals(3, StigmaService.stigmaSlotCount(30, false, true));
		assertEquals(3, StigmaService.stigmaSlotCount(39, false, true));
		assertEquals(4, StigmaService.stigmaSlotCount(40, false, true));
		assertEquals(4, StigmaService.stigmaSlotCount(44, false, true));
		assertEquals(5, StigmaService.stigmaSlotCount(45, false, true));
		assertEquals(5, StigmaService.stigmaSlotCount(49, false, true));
		assertEquals(6, StigmaService.stigmaSlotCount(50, false, true));
		assertEquals(6, StigmaService.stigmaSlotCount(54, false, true));
		assertEquals(7, StigmaService.stigmaSlotCount(55, false, true));
		assertEquals(7, StigmaService.stigmaSlotCount(80, false, true));
	}

	@Test
	void refreshToleratesMissingPlayers() {
		StigmaService.refreshStigmaSlots(null);
	}

	@Test
	void onlyQuestDataDeclaringTheStigmaSlotExtensionEntitlesSlots() throws Exception {
		assertFalse(StigmaService.extendsStigmaSlots(null));
		assertFalse(StigmaService.extendsStigmaSlots(
			metadataOf("src/main/resources/aion/data/static_data/quest/definitions/quests/1001.xml")));
		assertTrue(StigmaService.extendsStigmaSlots(
			metadataOf("src/main/resources/aion/data/static_data/quest/definitions/quests/1929.xml")));
	}

	private static QuestMetadata metadataOf(String path) throws Exception {
		try (InputStream input = Objects.requireNonNull(Files.newInputStream(Path.of(path)), path)) {
			return QuestDefinitionXmlCompiler.compile(input).definition().metadata();
		}
	}
}
