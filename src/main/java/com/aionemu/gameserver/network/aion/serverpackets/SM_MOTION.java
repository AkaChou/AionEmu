package com.aionemu.gameserver.network.aion.serverpackets;

import java.util.Collection;
import java.util.Map;

import com.aionemu.gameserver.model.gameobjects.player.motion.Motion;
import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;

/**
 * 动作（Motion）列表/状态同步服务端包。
 * Server packet that synchronizes player motion (emote/style) list and state to the client.
 * <p>
 * action 取值：1=列表、2=剩余时间、5=激活类型、6=移除、7=他人当前动作。
 * action values: 1=list, 2=remaining time, 5=activate type, 6=remove, 7=other player's active motions.
 */
public class SM_MOTION extends AionServerPacket {
	byte action;
	short motionId;
	int remainingTime;
	int playerId;
	Map<Integer, Motion> activeMotions;
	Collection<Motion> motions;
	byte type;

	/**
	 * 同步玩家全部动作列表（action=1）。
	 * Syncs the full motion list for a player (action=1).
	 *
	 * motion collection
	 */
	public SM_MOTION(Collection<Motion> motions) {
		this.action = 1;
		this.motions = motions;
	}

	/**
	 * 同步指定动作的剩余时间（action=2）。
	 * Syncs remaining time for a motion (action=2).
	 *
	 * motion id
	 * @param remainingTime 剩余时间（秒） / remaining time in seconds
	 */
	public SM_MOTION(short motionId, int remainingTime) {
		this.action = 2;
		this.motionId = motionId;
		this.remainingTime = remainingTime;
	}

	/**
	 * 激活/切换动作类型（action=5）。
	 * Activates or switches a motion type (action=5).
	 *
	 * motion id
	 * @param type 动作类型标志 / motion type flag
	 */
	public SM_MOTION(short motionId, byte type) {
		this.action = 5;
		this.motionId = motionId;
		this.type = type;
	}

	/**
	 * 移除指定动作（action=6）。
	 * Removes a motion (action=6).
	 *
	 * motion id
	 */
	public SM_MOTION(short motionId) {
		this.action = 6;
		this.motionId = motionId;
	}

	/**
	 * 同步其他玩家当前激活的动作槽（action=7）。
	 * Syncs another player's currently active motion slots (action=7).
	 *
	 * player object id
	 * @param activeMotions 槽位→动作映射 / slot-to-motion map
	 */
	public SM_MOTION(int playerId, Map<Integer, Motion> activeMotions) {
		this.action = 7;
		this.playerId = playerId;
		this.activeMotions = activeMotions;
	}

	@Override
	protected void writeImpl(AionConnection con) {
		writeC(action);
		switch (action) {
		case 1:
			writeH(motions.size());
			for (Motion motion : motions) {
				writeH(motion.getId());
				writeD(motion.getRemainingTime());
				writeC(motion.isActive() ? 1 : 0);
			}
			break;
		case 2:
			writeH(motionId);
			writeD(remainingTime);
			break;
		case 5:
			writeH(motionId);
			writeC(type);
			break;
		case 6:
			writeH(motionId);
			break;
		case 7:
			writeD(playerId);
			for (int i = 1; i < 6; i++) {
				Motion motion = activeMotions.get(i);
				if (motion == null) {
					writeH(0);
				} else {
					writeH(motion.getId());
				}
			}
			break;
		}
	}
}
