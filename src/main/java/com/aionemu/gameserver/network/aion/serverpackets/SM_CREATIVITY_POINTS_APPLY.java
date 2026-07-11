package com.aionemu.gameserver.network.aion.serverpackets;

import lombok.extern.slf4j.Slf4j;

import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;

/**
 * 创造点数应用结果包：反馈某槽位/技能的点数变更。
 * Server packet for creativity-points apply result: reports a slot/skill point change.
 *
 * @author Falke_34
 * @Rework Xnemonix
 */
@Slf4j
public class SM_CREATIVITY_POINTS_APPLY extends AionServerPacket {

	private int type;
	private int size;
	private int id;
	private int slotPoint;

	public SM_CREATIVITY_POINTS_APPLY(int type, int size) {
		this.type = type;
		this.size = size;
	}

	public SM_CREATIVITY_POINTS_APPLY(int type) {
		this.type = type;
	}

	public SM_CREATIVITY_POINTS_APPLY(int type, int id, int slotPoint) {
		this.id = id;
		this.slotPoint = slotPoint;
	}

	public SM_CREATIVITY_POINTS_APPLY(int type, int size, int id, int slotPoint) {
		this.type = type;
		this.size = size;
		this.id = id;
		this.slotPoint = slotPoint;
	}

	@Override
	protected void writeImpl(AionConnection con) {
		writeH(0x01);
		writeH(0x01); // No Loop should only return 1
		switch (type) {
		case 0:
			writeD(id);
			writeH(slotPoint);
			break;
		case 1:
			writeD(id);
			writeH(slotPoint);
			break;
		}
	}
}
