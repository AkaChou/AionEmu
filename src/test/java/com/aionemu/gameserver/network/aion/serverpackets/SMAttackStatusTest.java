package com.aionemu.gameserver.network.aion.serverpackets;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class SMAttackStatusTest {

	@Test
	void usesTheRetailDrowningStatusType() {
		assertEquals(12, SM_ATTACK_STATUS.TYPE.DROWNING.getValue());
	}

	@Test
	void hpAndDamageShareOneWireTypeButOppositeSignConventions() {
		// 线值相同（都是 7）：TYPE.HP 走默认分支原样写出有符号增减量，TYPE.DAMAGE 走取反分支。
		// 直接改血的同步出口依赖前者，改动这条约定会让血量变化的符号反向。
		// Same wire value (7): TYPE.HP writes the signed delta as-is while TYPE.DAMAGE negates it. The direct
		// HP-assignment outlet relies on the former, so changing this convention would flip the delta sign.
		assertEquals(7, SM_ATTACK_STATUS.TYPE.HP.getValue());
		assertEquals(SM_ATTACK_STATUS.TYPE.DAMAGE.getValue(), SM_ATTACK_STATUS.TYPE.HP.getValue());
	}
}
