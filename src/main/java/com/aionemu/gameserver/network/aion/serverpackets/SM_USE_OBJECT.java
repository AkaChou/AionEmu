package com.aionemu.gameserver.network.aion.serverpackets;

import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;

/**
 * 玩家对目标对象施放使用动作（采集、交互进度等）的服务端包。
 * Server packet for a player using a target object (gathering, interaction progress, etc.).
 *
 * @author ATracer
 */
public class SM_USE_OBJECT extends AionServerPacket {

	private int playerObjId;
	private int targetObjId;
	private int time;
	private int actionType;

	/**
	 * player object id
	 * target object id
	 * @param time        使用耗时（毫秒） / use duration in ms
	 * action type
	 */
	public SM_USE_OBJECT(int playerObjId, int targetObjId, int time, int actionType) {
		super();
		this.playerObjId = playerObjId;
		this.targetObjId = targetObjId;
		this.time = time;
		this.actionType = actionType;
	}

	@Override
	protected void writeImpl(AionConnection con) {
		writeD(playerObjId);
		writeD(targetObjId);
		writeD(time);
		writeC(actionType);
	}
}
