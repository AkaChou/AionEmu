package com.aionemu.gameserver.model.gameobjects.player;

import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.dataholders.PlayerExperienceTable;
import com.aionemu.gameserver.model.PlayerClass;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import org.junit.jupiter.api.Test;
import org.objenesis.ObjenesisStd;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 锁定高阶守护者晋升不依赖当前 65 级经验，并直接到达 66 级起始状态。
 * Locks ArchDaeva promotion to reach the level-66 start state regardless of current level-65 EXP.
 */
class PlayerCommonDataArchDaevaTest {
	private static final long LEVEL_65_EXP = 1926765410L;
	private static final long LEVEL_66_EXP = 2066885620L;

	@Test
	void promotionDirectlyReachesLevel66RegardlessOfCurrentExp() throws Exception {
		PlayerExperienceTable original = DataManager.PLAYER_EXPERIENCE_TABLE;
		try {
			DataManager.PLAYER_EXPERIENCE_TABLE = experienceTable();
			PlayerCommonData commonData = new PlayerCommonData(7);
			setField(commonData, "playerClass", PlayerClass.GLADIATOR);
			setField(commonData, "level", 65);
			setField(commonData, "exp", LEVEL_65_EXP + 1);

			commonData.setArchDaeva();

			assertEquals(66, commonData.getLevel());
			assertEquals(LEVEL_66_EXP, commonData.getExp());
			assertTrue(commonData.isArchDaeva());
		} finally {
			DataManager.PLAYER_EXPERIENCE_TABLE = original;
		}
	}

	@Test
	void incompleteArchDaevaQuestCapsExpBelowLevel66() throws Exception {
		PlayerExperienceTable original = DataManager.PLAYER_EXPERIENCE_TABLE;
		try {
			DataManager.PLAYER_EXPERIENCE_TABLE = experienceTable();
			PlayerCommonData commonData = commonDataWithQuestState(Race.ELYOS, 10520, QuestStatus.START, 3);
			setField(commonData, "playerClass", PlayerClass.GLADIATOR);
			setField(commonData, "level", 65);
			setField(commonData, "exp", LEVEL_65_EXP + 1);

			commonData.setExp(LEVEL_66_EXP, false);
			assertEquals(65, commonData.getLevel());
			assertEquals(LEVEL_66_EXP - 1, commonData.getExp());

			commonData.setExp(LEVEL_66_EXP + 50_000_000L, false);
			assertEquals(65, commonData.getLevel());
			assertEquals(LEVEL_66_EXP - 1, commonData.getExp());
		} finally {
			DataManager.PLAYER_EXPERIENCE_TABLE = original;
		}
	}

	@Test
	void onlyTheOwnRacialQuestCompletionReleasesTheLevelCap() throws Exception {
		assertFalse(commonDataWithQuestState(Race.ELYOS, 10520, QuestStatus.COMPLETE, 0).isArchDaevaLevelCapped());
		assertFalse(commonDataWithQuestState(Race.ASMODIANS, 20520, QuestStatus.COMPLETE, 0)
			.isArchDaevaLevelCapped());
		assertTrue(commonDataWithQuestState(Race.ELYOS, 10520, QuestStatus.START, 4).isArchDaevaLevelCapped());
		assertTrue(commonDataWithQuestState(Race.ELYOS, 20520, QuestStatus.COMPLETE, 0).isArchDaevaLevelCapped());
		assertTrue(commonDataWithQuestState(Race.ASMODIANS, 10520, QuestStatus.COMPLETE, 0)
			.isArchDaevaLevelCapped());
	}

	private static PlayerCommonData commonDataWithQuestState(Race race, int questId, QuestStatus status,
			int questVar) throws Exception {
		Player player = new ObjenesisStd().newInstance(Player.class);
		PlayerCommonData commonData = new PlayerCommonData(7) {
			@Override
			public Player getPlayer() {
				return player;
			}
		};
		commonData.setRace(race);
		setField(Player.class, player, "playerCommonData", commonData);
		QuestStateList questStates = new QuestStateList();
		questStates.addQuest(questId, new QuestState(questId, status, questVar, 0, null, null, null));
		setField(Player.class, player, "questStateList", questStates);
		return commonData;
	}

	private static PlayerExperienceTable experienceTable() throws Exception {
		PlayerExperienceTable table = new PlayerExperienceTable();
		long[] experience = new long[67];
		experience[64] = LEVEL_65_EXP;
		experience[65] = LEVEL_66_EXP;
		experience[66] = 2631427378L;
		Field field = PlayerExperienceTable.class.getDeclaredField("experience");
		field.setAccessible(true);
		field.set(table, experience);
		return table;
	}

	private static void setField(PlayerCommonData commonData, String name, Object value) throws Exception {
		setField(PlayerCommonData.class, commonData, name, value);
	}

	private static void setField(Class<?> declaringClass, Object target, String name, Object value) throws Exception {
		Field field = declaringClass.getDeclaredField(name);
		field.setAccessible(true);
		field.set(target, value);
	}
}
