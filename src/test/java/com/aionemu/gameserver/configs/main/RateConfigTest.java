package com.aionemu.gameserver.configs.main;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import com.aionemu.commons.configuration.ConfigurableProcessor;
import org.junit.jupiter.api.Test;

class RateConfigTest {

	@Test
	void retailAlignmentRewardRatesStayAtOne() throws IOException {
		Properties properties = new Properties();
		try (var input = Files.newInputStream(Path.of("src/main/resources/aion/config/main/rates.properties"))) {
			properties.load(input);
		}
		ConfigurableProcessor.process(RateConfig.class, properties);
		assertRetailRates();

		ConfigurableProcessor.process(RateConfig.class, new Properties());
		assertRetailRates();
	}

	private void assertRetailRates() {
		for (float rate : new float[] { RateConfig.QUEST_XP_RATE, RateConfig.PREMIUM_QUEST_XP_RATE,
				RateConfig.VIP_QUEST_XP_RATE, RateConfig.QUEST_KINAH_RATE, RateConfig.PREMIUM_QUEST_KINAH_RATE,
				RateConfig.VIP_QUEST_KINAH_RATE, RateConfig.DROP_RATE, RateConfig.PREMIUM_DROP_RATE,
				RateConfig.VIP_DROP_RATE, RateConfig.AP_NPC_RATE, RateConfig.PREMIUM_AP_NPC_RATE,
				RateConfig.VIP_AP_NPC_RATE, RateConfig.DREDGION_REWARD_RATE, RateConfig.QUEST_AP_RATE,
				RateConfig.PREMIUM_QUEST_AP_RATE, RateConfig.VIP_QUEST_AP_RATE, RateConfig.QUEST_GP_RATE,
				RateConfig.PREMIUM_QUEST_GP_RATE, RateConfig.VIP_QUEST_GP_RATE,
				RateConfig.PVP_ARENA_DISCIPLINE_REWARD_RATE, RateConfig.PREMIUM_PVP_ARENA_DISCIPLINE_REWARD_RATE,
				RateConfig.VIP_PVP_ARENA_DISCIPLINE_REWARD_RATE, RateConfig.PVP_ARENA_CHAOS_REWARD_RATE,
				RateConfig.PREMIUM_PVP_ARENA_CHAOS_REWARD_RATE, RateConfig.VIP_PVP_ARENA_CHAOS_REWARD_RATE,
				RateConfig.PVP_ARENA_HARMONY_REWARD_RATE, RateConfig.PREMIUM_PVP_ARENA_HARMONY_REWARD_RATE,
				RateConfig.VIP_PVP_ARENA_HARMONY_REWARD_RATE, RateConfig.PVP_ARENA_GLORY_REWARD_RATE,
				RateConfig.PREMIUM_PVP_ARENA_GLORY_REWARD_RATE, RateConfig.VIP_PVP_ARENA_GLORY_REWARD_RATE }) {
			assertEquals(1f, rate);
		}
	}
}
