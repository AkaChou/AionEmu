package com.aionemu.gameserver.network.aion.serverpackets;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.ByteBuffer;
import java.util.Collections;

import org.junit.jupiter.api.Test;

import com.aionemu.gameserver.model.gameobjects.player.equipmentsetting.EquipmentSetting;

class SMEquipmentSettingTest {

	@Test
	void writesSavedDisplayFlagsAfterSlot() {
		EquipmentSetting setting = new EquipmentSetting(0, "Hidden Helmet", 5, 101, 102, 103, 104, 105, 106, 107,
				108, 109, 110, 111, 112, 113, 114, 115, 116, 117, 118, 119, 120, 121);
		SM_EQUIPMENT_SETTING packet = new SM_EQUIPMENT_SETTING(Collections.singletonList(setting));
		ByteBuffer buffer = ByteBuffer.allocate(128);

		packet.setBuf(buffer);
		packet.writeImpl(null);
		buffer.flip();

		assertEquals(1, Short.toUnsignedInt(buffer.getShort()));
		assertEquals(0, buffer.getInt());
		assertEquals(5, buffer.getInt());
		assertEquals(101, buffer.getInt());
	}
}
