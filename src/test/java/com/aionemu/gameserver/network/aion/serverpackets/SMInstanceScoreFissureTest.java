package com.aionemu.gameserver.network.aion.serverpackets;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.objenesis.ObjenesisStd;

import com.aionemu.gameserver.model.gameobjects.AionObject;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.instance.InstanceScoreType;
import com.aionemu.gameserver.model.instance.instancereward.FissureOfOblivionReward;
import com.aionemu.gameserver.model.instance.playerreward.FissureOfOblivionPlayerReward;
import com.aionemu.gameserver.network.aion.AionConnection;

class SMInstanceScoreFissureTest {

	@Test
	void writesFrozenMarbleTemplateBeforeItsCount() throws ReflectiveOperationException {
		FissureOfOblivionReward reward = new FissureOfOblivionReward(302100000, 1);
		reward.setInstanceScoreType(InstanceScoreType.END_PROGRESS);
		reward.setRank(5);
		reward.addPoints(10350);
		FissureOfOblivionPlayerReward playerReward = new FissureOfOblivionPlayerReward(1);
		playerReward.setFrozenMarbleOfMemory(1);
		reward.addPlayerReward(playerReward);

		SM_INSTANCE_SCORE packet = new SM_INSTANCE_SCORE(reward);
		ByteBuffer buffer = ByteBuffer.allocate(64);
		packet.setBuf(buffer);
		packet.writeImpl(connection(1));
		buffer.flip();

		assertEquals(302100000, buffer.getInt());
		assertEquals(0, buffer.getInt());
		assertEquals(InstanceScoreType.END_PROGRESS.getId(), buffer.getInt());
		assertEquals(10350, buffer.getInt());
		assertEquals(0, buffer.getInt());
		assertEquals(0, buffer.getInt());
		assertEquals(5, buffer.getInt());
		assertEquals(0, buffer.getInt());
		assertEquals(0, buffer.getInt());
		assertEquals(186000448, buffer.getInt());
		assertEquals(1, buffer.getInt());
	}

	private static AionConnection connection(int objectId) throws ReflectiveOperationException {
		ObjenesisStd objenesis = new ObjenesisStd();
		Player player = objenesis.newInstance(Player.class);
		setField(AionObject.class, player, "objectId", objectId);
		AionConnection connection = objenesis.newInstance(AionConnection.class);
		setField(AionConnection.class, connection, "activePlayer", new AtomicReference<>(player));
		return connection;
	}

	private static void setField(Class<?> owner, Object target, String name, Object value)
			throws ReflectiveOperationException {
		Field field = owner.getDeclaredField(name);
		field.setAccessible(true);
		field.set(target, value);
	}
}
