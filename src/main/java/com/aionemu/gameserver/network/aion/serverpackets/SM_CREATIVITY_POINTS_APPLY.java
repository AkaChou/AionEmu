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

	/**
	 * 构造创造点数应用结果包（类型与槽位数）。
	 * Creates a CP apply-result packet (type and slot count).
	 *
	 * @param type 应用类型 / apply type
	 * @param size 槽位数 / slot count
	 */
	public SM_CREATIVITY_POINTS_APPLY(int type, int size) {
		this.type = type;
		this.size = size;
	}

	/**
	 * 构造仅含类型的创造点数应用结果包。
	 * Creates a CP apply-result packet carrying only the type.
	 *
	 * @param type 应用类型 / apply type
	 */
	public SM_CREATIVITY_POINTS_APPLY(int type) {
		this.type = type;
	}

	/**
	 * 构造指定槽位/技能的点数变更反馈包。
	 * Creates a CP apply-result packet for a specific slot/skill point change.
	 *
	 * @param type 应用类型 / apply type
	 * @param id 槽位或技能 ID / slot or skill id
	 * @param slotPoint 变更后的槽位点数 / new slot point count
	 */
	public SM_CREATIVITY_POINTS_APPLY(int type, int id, int slotPoint) {
		this.type = type;
		this.id = id;
		this.slotPoint = slotPoint;
	}

	/**
	 * 构造完整创造点数应用结果包。
	 * Creates a full CP apply-result packet.
	 *
	 * @param type 应用类型 / apply type
	 * @param size 槽位数 / slot count
	 * @param id 槽位或技能 ID / slot or skill id
	 * @param slotPoint 变更后的槽位点数 / new slot point count
	 */
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
