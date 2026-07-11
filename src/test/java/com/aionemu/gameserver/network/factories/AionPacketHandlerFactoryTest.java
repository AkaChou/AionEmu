package com.aionemu.gameserver.network.factories;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.lang.reflect.Field;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.aionemu.gameserver.network.aion.AionClientPacket;
import com.aionemu.gameserver.network.aion.AionConnection.State;
import com.aionemu.gameserver.network.aion.AionPacketHandler;
import com.aionemu.gameserver.network.aion.clientpackets.CM_MAY_QUIT;

class AionPacketHandlerFactoryTest {

	@Test
	void registersEquipmentSettingUsePacket() throws Exception {
		Map<Integer, AionClientPacket> prototypes = packetPrototypes(new AionPacketHandlerFactory().getPacketHandler());
		AionClientPacket packet = prototypes.get(0x01D3);

		assertNotNull(packet);
		assertEquals("CM_EQUIPMENT_SETTING_USE", packet.getPacketName());
	}

	@Test
	void rejectsDuplicateOpcodes() {
		AionPacketHandler handler = new AionPacketHandler();
		handler.addPacketPrototype(new CM_MAY_QUIT(0x123, State.IN_GAME));

		assertThrows(IllegalArgumentException.class,
				() -> handler.addPacketPrototype(new CM_MAY_QUIT(0x123, State.IN_GAME)));
	}

	@SuppressWarnings("unchecked")
	private Map<Integer, AionClientPacket> packetPrototypes(AionPacketHandler handler) throws Exception {
		Field field = AionPacketHandler.class.getDeclaredField("packetsPrototypes");
		field.setAccessible(true);
		return (Map<Integer, AionClientPacket>) field.get(handler);
	}
}
