package com.aionemu.gameserver.network;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import com.aionemu.gameserver.model.Gender;
import com.aionemu.gameserver.model.PlayerClass;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.account.PlayerAccountData;
import com.aionemu.gameserver.model.gameobjects.player.PlayerAppearance;
import com.aionemu.gameserver.model.gameobjects.player.PlayerCommonData;
import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.serverpackets.SM_CREATE_CHARACTER;
import com.aionemu.gameserver.world.WorldPosition;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.util.Collections;
import org.junit.jupiter.api.Test;

class SMCreateCharacterTest {

	@Test
	void writesCreatedCharacterThatHasNeverLoggedIn() throws Exception {
		SM_CREATE_CHARACTER packet = new SM_CREATE_CHARACTER(newCharacterWithNoLastOnline(),
				SM_CREATE_CHARACTER.RESPONSE_OK);
		packet.setBuf(ByteBuffer.allocate(1024));
		Method writeImpl = SM_CREATE_CHARACTER.class.getDeclaredMethod("writeImpl", AionConnection.class);
		writeImpl.setAccessible(true);

		assertDoesNotThrow(() -> writeImpl.invoke(packet, new Object[] { null }));
	}

	private PlayerAccountData newCharacterWithNoLastOnline() {
		PlayerCommonData commonData = new PlayerCommonData(1);
		commonData.setName("Fresh");
		commonData.setGender(Gender.MALE);
		commonData.setRace(Race.ELYOS);
		commonData.setPlayerClass(PlayerClass.WARRIOR);
		commonData.setLastOnline(null);
		WorldPosition position = new WorldPosition(210010000);
		position.setXYZH(1f, 2f, 3f, (byte) 0);
		commonData.setPosition(position);
		return new PlayerAccountData(commonData, null, new PlayerAppearance(), Collections.emptyList(), null);
	}
}
