package com.aionemu.gameserver.model.stats.container;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.aionemu.gameserver.network.aion.serverpackets.SM_ATTACK_STATUS.TYPE;

class NpcLifeStatsTest {

	@Test
	void naturalNpcHealingUsesTheTargetHpPacketLayout() {
		assertEquals(TYPE.HP, NpcLifeStats.packetType(TYPE.NATURAL_HP));
		assertEquals(TYPE.DAMAGE, NpcLifeStats.packetType(TYPE.DAMAGE));
	}
}
