package com.aionemu.gameserver.model.gameobjects.player;

import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.dataholders.PlayerExperienceTable;
import com.aionemu.gameserver.model.PlayerClass;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
		Field field = PlayerCommonData.class.getDeclaredField(name);
		field.setAccessible(true);
		field.set(commonData, value);
	}
}
