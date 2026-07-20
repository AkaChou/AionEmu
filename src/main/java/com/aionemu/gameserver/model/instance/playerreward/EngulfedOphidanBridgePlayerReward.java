package com.aionemu.gameserver.model.instance.playerreward;

import com.aionemu.gameserver.model.Race;

public final class EngulfedOphidanBridgePlayerReward extends BattlegroundPlayerReward {
	public EngulfedOphidanBridgePlayerReward(int objectId, byte buffId, Race race) {
		super(objectId, buffId, race);
	}

	public EngulfedOphidanBridgePlayerReward(int objectId, byte buffId, Race race, long joinedAt) {
		super(objectId, buffId, race, joinedAt);
	}
}
