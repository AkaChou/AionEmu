package com.aionemu.gameserver.services.abyss;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.network.aion.serverpackets.SM_ABYSS_RANKING_LEGIONS;
import com.aionemu.gameserver.network.aion.serverpackets.SM_ABYSS_RANKING_PLAYERS;
import org.junit.jupiter.api.Test;

class AbyssRankingCacheTest {

	@Test
	void storesRankingsInJdkMaps() throws Exception {
		AbyssRankingCache cache = new AbyssRankingCache();

		Map<Race, List<SM_ABYSS_RANKING_PLAYERS>> players = players(cache);
		Map<Race, SM_ABYSS_RANKING_LEGIONS> legions = legions(cache);

		assertEquals(HashMap.class, players.getClass());
		assertEquals(HashMap.class, legions.getClass());
	}

	@SuppressWarnings("unchecked")
	private Map<Race, List<SM_ABYSS_RANKING_PLAYERS>> players(AbyssRankingCache cache) throws Exception {
		Field field = AbyssRankingCache.class.getDeclaredField("players");
		field.setAccessible(true);
		return (Map<Race, List<SM_ABYSS_RANKING_PLAYERS>>) field.get(cache);
	}

	@SuppressWarnings("unchecked")
	private Map<Race, SM_ABYSS_RANKING_LEGIONS> legions(AbyssRankingCache cache) throws Exception {
		Field field = AbyssRankingCache.class.getDeclaredField("legions");
		field.setAccessible(true);
		return (Map<Race, SM_ABYSS_RANKING_LEGIONS>) field.get(cache);
	}
}
