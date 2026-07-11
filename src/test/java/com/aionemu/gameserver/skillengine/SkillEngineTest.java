package com.aionemu.gameserver.skillengine;

import static org.junit.jupiter.api.Assertions.assertNull;

import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.dataholders.SkillData;
import org.junit.jupiter.api.Test;

class SkillEngineTest {

	@Test
	void applyingUnknownEffectDoesNothing() {
		SkillData oldSkillData = DataManager.SKILL_DATA;
		try {
			DataManager.SKILL_DATA = new SkillData();
			assertNull(SkillEngine.getInstance().applyEffect(Integer.MAX_VALUE, null, null));
		} finally {
			DataManager.SKILL_DATA = oldSkillData;
		}
	}
}
