package com.aionemu.gameserver.services.ranking;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.aionemu.gameserver.network.aion.serverpackets.SM_SEASON_RANKING;
import org.junit.jupiter.api.Test;

class SeasonRankingUpdateServiceTest {

	@Test
	void storesPlayersInJdkMap() throws Exception {
		SeasonRankingUpdateService service = new SeasonRankingUpdateService();

		Map<Integer, List<SM_SEASON_RANKING>> players = players(service);

		assertEquals(HashMap.class, players.getClass());
	}

	@SuppressWarnings("unchecked")
	private Map<Integer, List<SM_SEASON_RANKING>> players(SeasonRankingUpdateService service) throws Exception {
		Field field = SeasonRankingUpdateService.class.getDeclaredField("players");
		field.setAccessible(true);
		return (Map<Integer, List<SM_SEASON_RANKING>>) field.get(service);
	}
}
