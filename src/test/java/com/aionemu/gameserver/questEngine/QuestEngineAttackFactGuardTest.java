package com.aionemu.gameserver.questEngine;

import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.stats.container.CreatureLifeStats;
import com.aionemu.gameserver.model.stats.container.NpcLifeStats;
import com.aionemu.gameserver.world.WorldPosition;
import org.junit.jupiter.api.Test;

import org.objenesis.ObjenesisStd;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 攻击事实权威性守卫的回归。
 * Regression for the attack-fact authority guard.
 * <p>已删除但仍存活的 NPC 必须被吞：诱导到点删除后的在途攻击会空寻址 after-commit 监视动作。
 * 已死亡的 NPC 必须放行：伤害先于 {@code QuestEngine.onAttack} 钩子结算，致命一击抵达时已是死亡态
 * （任务 1006 的 {@code attack-npc 211043 + npc-hp-below-percent 50} 就靠这条事实成立）。</p>
 * <p>A deleted-but-alive NPC must be swallowed: in-flight attacks after lure deletion would address no
 * target in the after-commit watch action. A dead NPC must pass: damage settles before the
 * {@code QuestEngine.onAttack} hook, so the lethal blow arrives already dead — quest 1006's
 * {@code attack-npc 211043 + npc-hp-below-percent 50} transition depends on that fact.</p>
 */
class QuestEngineAttackFactGuardTest {

	@Test
	void deletedButAliveNpcIsNotAuthoritative() throws Exception {
		assertFalse(QuestEngine.attackFactIsAuthoritative(npc(false, false)));
	}

	@Test
	void spawnedAliveNpcIsAuthoritative() throws Exception {
		assertTrue(QuestEngine.attackFactIsAuthoritative(npc(true, false)));
	}

	@Test
	void lethalBlowOnSpawnedNpcIsAuthoritative() throws Exception {
		assertTrue(QuestEngine.attackFactIsAuthoritative(npc(true, true)));
	}

	@Test
	void deadNpcWhoseCorpseAlreadyDecayedIsStillAuthoritative() throws Exception {
		// 无掉落时尸体消散延迟为 0ms，可能抢在钩子前把 isSpawned 置假。
		// Without drops the corpse decay delay is 0ms and may clear isSpawned before the hook runs.
		assertTrue(QuestEngine.attackFactIsAuthoritative(npc(false, true)));
	}

	@Test
	void npcWithoutLifeStatsIsNotAuthoritative() throws Exception {
		Npc npc = npc(true, false);
		setField(Creature.class, npc, "lifeStats", null);
		assertFalse(QuestEngine.attackFactIsAuthoritative(npc));
	}

	private static Npc npc(boolean spawned, boolean dead) throws Exception {
		Npc npc = new ObjenesisStd().newInstance(Npc.class);
		WorldPosition position = new WorldPosition(310010000);
		setField(WorldPosition.class, position, "isSpawned", spawned);
		npc.setPosition(position);
		NpcLifeStats lifeStats = new ObjenesisStd().newInstance(NpcLifeStats.class);
		setField(CreatureLifeStats.class, lifeStats, "alreadyDead", dead);
		setField(Creature.class, npc, "lifeStats", lifeStats);
		return npc;
	}

	private static void setField(Class<?> declaringClass, Object target, String name, Object value) throws Exception {
		Field field = declaringClass.getDeclaredField(name);
		field.setAccessible(true);
		field.set(target, value);
	}
}
