package com.aionemu.gameserver.network.aion.serverpackets;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.junit.jupiter.api.Test;
import org.objenesis.ObjenesisStd;

import com.aionemu.gameserver.model.gameobjects.AionObject;
import com.aionemu.gameserver.model.team2.GeneralTeam;
import com.aionemu.gameserver.model.team2.alliance.PlayerAlliance;
import com.aionemu.gameserver.model.team2.league.League;
import com.aionemu.gameserver.model.team2.league.LeagueMember;

class SM_ALLIANCE_INFOTest {

	private static final ObjenesisStd OBJENESIS = new ObjenesisStd();

	@Test
	void writesLeagueMemberCountAndLeaderId() throws ReflectiveOperationException {
		LeagueMember leader = new LeagueMember(alliance(101), 0);
		League league = OBJENESIS.newInstance(League.class);
		setField(GeneralTeam.class, league, "members", new ConcurrentHashMap<Integer, LeagueMember>());
		setField(GeneralTeam.class, league, "leader", leader);
		league.addMember(leader);
		league.addMember(new LeagueMember(alliance(102), 1));

		SM_ALLIANCE_INFO packet = OBJENESIS.newInstance(SM_ALLIANCE_INFO.class);
		ByteBuffer buffer = ByteBuffer.allocate(9);
		packet.setBuf(buffer);
		packet.writeLeagueHeader(league);
		buffer.flip();

		assertEquals(226, buffer.getInt());
		assertEquals(2, buffer.get());
		assertEquals(101, buffer.getInt());
	}

	private static PlayerAlliance alliance(int objectId) throws ReflectiveOperationException {
		PlayerAlliance alliance = OBJENESIS.newInstance(PlayerAlliance.class);
		setField(AionObject.class, alliance, "objectId", objectId);
		return alliance;
	}

	private static void setField(Class<?> owner, Object target, String name, Object value)
			throws ReflectiveOperationException {
		Field field = owner.getDeclaredField(name);
		field.setAccessible(true);
		field.set(target, value);
	}
}
