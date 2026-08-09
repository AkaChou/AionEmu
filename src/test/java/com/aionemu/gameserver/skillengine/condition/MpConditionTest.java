package com.aionemu.gameserver.skillengine.condition;

import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.stats.container.CreatureLifeStats;
import com.aionemu.gameserver.model.stats.container.PlayerLifeStats;
import com.aionemu.gameserver.skillengine.model.Skill;
import com.aionemu.gameserver.skillengine.model.SkillTemplate;
import org.junit.jupiter.api.Test;
import org.objenesis.ObjenesisStd;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MpConditionTest {
	private static final ObjenesisStd OBJENESIS = new ObjenesisStd();

	@Test
	void npcCastsIgnoreUnmodeledMpResource() {
		Npc npc = OBJENESIS.newInstance(Npc.class);
		MpCondition condition = condition(30);

		assertTrue(condition.validate(skill(npc)));
	}

	@Test
	void playerCastsStillRequireEnoughMp() throws Exception {
		Player player = OBJENESIS.newInstance(Player.class);
		PlayerLifeStats lifeStats = OBJENESIS.newInstance(PlayerLifeStats.class);
		player.setLifeStats(lifeStats);
		MpCondition condition = condition(30);

		assertFalse(condition.validate(skill(player)));
		setCurrentMp(lifeStats, 31);
		assertTrue(condition.validate(skill(player)));
	}

	private static MpCondition condition(int value) {
		MpCondition condition = new MpCondition();
		condition.value = value;
		return condition;
	}

	private static Skill skill(Creature effector) {
		return new Skill(new SkillTemplate(), effector, 1, effector, null);
	}

	private static void setCurrentMp(CreatureLifeStats<?> lifeStats, int value) throws Exception {
		Field field = CreatureLifeStats.class.getDeclaredField("currentMp");
		field.setAccessible(true);
		field.setInt(lifeStats, value);
	}
}
