package com.aionemu.gameserver.skillengine;

import static org.junit.jupiter.api.Assertions.assertNull;

import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.dataholders.SkillData;
import com.aionemu.gameserver.lifecycle.TestServiceProviders;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class SkillEngineTest {

	private static final SkillEngine SKILL_ENGINE = new SkillEngine();

	@BeforeAll
	static void installSkillEngineProvider() {
		// 静态兜底已退役：测试显式提供引擎实例。
		// The static fallback is retired, so the test supplies the engine instance explicitly.
		TestServiceProviders.install(SkillEngine.class, SKILL_ENGINE);
	}

	@AfterAll
	static void clearSkillEngineProvider() {
		TestServiceProviders.clear(SkillEngine.class);
	}

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
