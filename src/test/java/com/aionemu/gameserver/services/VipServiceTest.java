package com.aionemu.gameserver.services;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.File;
import java.lang.reflect.Field;

import jakarta.xml.bind.JAXBContext;

import org.junit.jupiter.api.Test;
import org.objenesis.ObjenesisStd;

import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.dataholders.ServiceBuffData;
import com.aionemu.gameserver.model.account.Account;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.stats.container.PlayerGameStats;
import com.aionemu.gameserver.model.stats.container.StatEnum;

class VipServiceTest {

	@Test
	void mapsVipStageToBaseAndOptionBenefits() {
		assertArrayEquals(new int[0], VipService.benefitBuffIds(0));
		assertArrayEquals(new int[] { 1000001, 1000007, 1000013 }, VipService.benefitBuffIds(1));
		assertArrayEquals(new int[] { 1000006, 1000012, 1000018 }, VipService.benefitBuffIds(6));
	}

	@Test
	void rejectsUnknownVipStage() {
		assertThrows(IllegalArgumentException.class, () -> VipService.benefitBuffIds(7));
	}

	@Test
	void appliesLevelSixMaxHpFromServiceBonusData() throws Exception {
		ServiceBuffData previous = DataManager.SERVICE_BUFF_DATA;
		try {
			DataManager.SERVICE_BUFF_DATA = (ServiceBuffData) JAXBContext.newInstance(ServiceBuffData.class)
				.createUnmarshaller().unmarshal(new File("src/main/resources/aion/data/static_data/service_bonusattr/service_bonusattr.xml"));
			Player player = new ObjenesisStd().newInstance(TestPlayer.class);
			player.setGameStats(new TestPlayerGameStats(player));
			Field playerAccount = Player.class.getDeclaredField("playerAccount");
			playerAccount.setAccessible(true);
			playerAccount.set(player, new Account(1));
			player.getPlayerAccount().setVipLevel((byte) 6);

			VipService.applyBenefits(player);

			assertEquals(1888, player.getGameStats().getStat(StatEnum.MAXHP, 1000).getCurrent());
		} finally {
			DataManager.SERVICE_BUFF_DATA = previous;
		}
	}

	private static final class TestPlayerGameStats extends PlayerGameStats {

		private TestPlayerGameStats(Player owner) {
			super(owner);
		}

		@Override
		protected void onStatsChange() {
		}
	}

	private static final class TestPlayer extends Player {

		private TestPlayer() {
			super(null, null, null, new Account(1));
		}
	}
}
