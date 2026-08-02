package com.aionemu.gameserver.questEngine.runtime;

import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.gameobjects.AionObject;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.VisibleObject;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.gameobjects.player.PlayerCommonData;
import com.aionemu.gameserver.model.stats.container.PlayerLifeStats;
import com.aionemu.gameserver.questEngine.definition.QuestEvent;
import com.aionemu.gameserver.questEngine.definition.QuestPvpCreditSource;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.world.WorldPosition;
import org.junit.jupiter.api.Test;
import org.objenesis.ObjenesisStd;

import java.lang.reflect.Field;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerQuestPvpEventPortTest {
	@Test
	void soloPortCapturesAuthoritativeFactsAndZones() throws Exception {
		Player killer = player(7, Race.ELYOS, 50, 210130000, false);
		Player victim = player(20, Race.ASMODIANS, 55, 210130000, false);
		QuestEnv env = new QuestEnv(victim, killer, 0, 0);
		PlayerQuestPvpEventPort port = new PlayerQuestPvpEventPort((left, right) -> true,
			player -> Set.of("Sulfur_Fortress_400010000"));

		QuestEvent.KillInWorld event = port.killInWorld(env, killer, 3, 210130000, QuestPvpCreditSource.SOLO);
		assertEquals(killer.getObjectId(), event.facts().recipientId());
		assertEquals(victim.getObjectId(), event.facts().victimId());
		assertEquals(50, event.facts().recipientLevel());
		assertEquals(Set.of("SULFUR_FORTRESS_400010000"), event.facts().recipientZones());
	}

	@Test
	void portRejectsWrongRaceDeadRecipientWorldAndCreditSource() throws Exception {
		Player killer = player(7, Race.ELYOS, 50, 210130000, false);
		Player victim = player(20, Race.ASMODIANS, 55, 210130000, false);
		PlayerQuestPvpEventPort port = new PlayerQuestPvpEventPort((left, right) -> true, ignored -> Set.of());

		assertThrows(IllegalArgumentException.class, () -> port.killInWorld(
			new QuestEnv(victim, killer, 0, 0), killer, 3, 210070000, QuestPvpCreditSource.SOLO));
		Player dead = player(7, Race.ELYOS, 50, 210130000, true);
		assertThrows(IllegalArgumentException.class, () -> port.killInWorld(
			new QuestEnv(victim, dead, 0, 0), dead, 3, 210130000, QuestPvpCreditSource.SOLO));
		assertThrows(IllegalArgumentException.class, () -> port.killInWorld(
			new QuestEnv(victim, killer, 0, 0), killer, 3, 210130000, QuestPvpCreditSource.GROUP));
		Player sameRaceVictim = player(21, Race.ELYOS, 55, 210130000, false);
		assertThrows(IllegalArgumentException.class, () -> port.killInWorld(
			new QuestEnv(sameRaceVictim, killer, 0, 0), killer, 3, 210130000, QuestPvpCreditSource.SOLO));
	}

	@Test
	void portFailsClosedWhenRecipientIsOutOfRange() throws Exception {
		Player killer = player(7, Race.ELYOS, 50, 210130000, false);
		Player victim = player(20, Race.ASMODIANS, 55, 210130000, false);
		PlayerQuestPvpEventPort port = new PlayerQuestPvpEventPort((left, right) -> false, ignored -> Set.of());
		assertThrows(IllegalArgumentException.class, () -> port.killRanked(
			new QuestEnv(victim, killer, 0, 0), killer, 3, QuestPvpCreditSource.SOLO));
	}

	private static Player player(int id, Race race, int level, int worldId, boolean dead) throws Exception {
		ObjenesisStd objenesis = new ObjenesisStd();
		Player player = objenesis.newInstance(Player.class);
		PlayerCommonData common = objenesis.newInstance(PlayerCommonData.class);
		setField(AionObject.class, player, "objectId", id);
		setField(Player.class, player, "playerCommonData", common);
		setField(PlayerCommonData.class, common, "race", race);
		setField(PlayerCommonData.class, common, "level", level);
		setField(VisibleObject.class, player, "position", new WorldPosition(worldId));
		PlayerLifeStats lifeStats = objenesis.newInstance(PlayerLifeStats.class);
		setField(Creature.class, player, "lifeStats", lifeStats);
		setField(com.aionemu.gameserver.model.stats.container.CreatureLifeStats.class,
			lifeStats, "alreadyDead", dead);
		return player;
	}

	private static void setField(Class<?> declaringClass, Object target, String name, Object value) throws Exception {
		Field field = declaringClass.getDeclaredField(name);
		field.setAccessible(true);
		field.set(target, value);
	}
}
