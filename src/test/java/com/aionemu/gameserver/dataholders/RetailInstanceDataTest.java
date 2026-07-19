package com.aionemu.gameserver.dataholders;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.File;

import org.junit.jupiter.api.Test;

class RetailInstanceDataTest {

	@Test
	void loadsCompleteRetailInstanceClosure() {
		RetailInstanceData data = RetailInstanceData.load(
			new File("src/main/resources/aion/definitions/compact/instance"),
			new File("src/main/resources/aion/definitions/schemas/retail-instance-data.xsd"));

		assertEquals(378, data.definitionCount());
		assertEquals(145, data.limitCount());
		assertEquals(272, data.cooldownCount());
		assertEquals(158, data.matchCount());
		assertEquals(1, data.teamMatchCount());
		assertEquals(6, data.definitionsForWorld(300040000).size());
		assertEquals(9, data.limit(300040000).requiredInt("id"));
		assertEquals(300040000, data.match(6).requiredInt("world_id"));
		assertEquals(302350000, data.teamMatch(1).requiredInt("world_id"));
		assertEquals("special", data.coverage(720010000).value("classification"));
		assertEquals("HANDLER", data.coverage(300040000).value("behavior"));
		assertEquals("RETAIL_AI_QUEST", data.coverage(301340000).value("behavior"));
		assertEquals("DATA_ONLY", data.coverage(310020000).value("behavior"));
		assertEquals("EVENT", data.coverage(600080000).value("behavior"));
		assertEquals("HOUSING", data.coverage(720010000).value("behavior"));
		assertEquals("EXCLUDED_NON_PRODUCTION", data.coverage(900210000).value("behavior"));
		assertEquals("TOURNAMENT", data.coverage(302320000).value("behavior"));
		assertNotNull(data.coverage(302320000).value("behavior_source"));
		assertNotNull(data.rewards("world_timeattack").stream()
			.filter(row -> row.intValue("world_id", 0) == 300540000).findFirst().orElse(null));
		assertEquals(5, data.tournaments().size());
		assertEquals(2, data.tournamentForMatchmaker(125).requiredInt("id"));
		assertEquals(4, data.tournamentForMatchmaker(130).requiredInt("id"));
		assertEquals(357, data.tournamentForLobbyWorld(302320000).requiredInt("lobby_creation_id"));
		assertEquals(364, data.tournamentForStageWorld(302360000).requiredInt("stage_creation_id"));
		assertEquals(5, data.tournament(2).value("stage_start_01").split(";").length);
		assertEquals(2, data.tournament(2).requiredInt("round_1_win_kill_point"));
		assertEquals(186000454, data.tournament(2).requiredInt("round_5_item1_id"));
		assertEquals("TOURNAMENT", data.match(129).value("handler"));
		assertEquals(322, data.lunaDungeon(1).requiredInt("creation_id"));
		assertEquals(301640000, data.lunaDungeon(2).requiredInt("world_id"));
		assertEquals(2, data.lunaDungeonForWorld(301640000).requiredInt("id"));
		assertEquals(1, data.lunaPrice(45).requiredInt("free_turn"));
		assertEquals(5, data.lunaPrice(47).requiredInt("price_max_count"));
	}
}
