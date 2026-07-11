package com.aionemu.gameserver.network.aion.serverpackets;

import java.util.Map;

import com.aionemu.gameserver.controllers.RVController;
import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;

/**
 * 向客户端同步裂隙（Rift）状态、归属或入口信息。
 * Server packet synchronizing rift status, ownership, or portal info to the client.
 *
 * @author Sweetkr
 */
public class SM_RIFT_ANNOUNCE extends AionServerPacket {

	private int actionId;
	private RVController rift;
	private Map<Integer, Integer> rifts;
	private int objectId;
	private int gelkmaros, inggison;

	/**
	 * 使用给定参数构造 SM_RIFT_ANNOUNCE 包。
	 * Rift announce packet
	 *
	 * @param rifts 裂隙映射 / rift map
	 */
	public SM_RIFT_ANNOUNCE(Map<Integer, Integer> rifts) {
		this.actionId = 0;
		this.rifts = rifts;
	}

	/**
	 * 使用给定参数构造 SM_RIFT_ANNOUNCE 包。
	 * Creates a SM_RIFT_ANNOUNCE packet with the given parameters.
	 *
	 * gelkmaros flag
	 * inggison flag
	 */
	public SM_RIFT_ANNOUNCE(boolean gelkmaros, boolean inggison) {
		this.gelkmaros = gelkmaros ? 1 : 0;
		this.inggison = inggison ? 1 : 0;
		this.actionId = 1;
	}

	/**
	 * 使用给定参数构造 SM_RIFT_ANNOUNCE 包。
	 * Rift announce packet
	 *
	 * @param rift 裂隙控制器 / rift controller
	 * is master
	 */
	public SM_RIFT_ANNOUNCE(RVController rift, boolean isMaster) {
		this.rift = rift;
		this.actionId = isMaster ? 2 : 3;
	}

	/**
	 * 使用给定参数构造 SM_RIFT_ANNOUNCE 包。
	 * Rift despawn
	 *
	 * object id
	 */
	public SM_RIFT_ANNOUNCE(int objectId) {
		this.objectId = objectId;
		this.actionId = 5;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void writeImpl(AionConnection con) {
		switch (actionId) {
		case 0:
			writeH(0x57);
			writeC(actionId);
			for (int value : rifts.values()) {
				writeD(value);
			}
			break;
		case 1:
			writeH(0x09);
			writeC(actionId);
			writeD(gelkmaros);
			writeD(inggison);
			break;
		case 2:
			writeH(0x39);
			writeC(actionId);
			writeD(rift.getOwner().getObjectId());
			writeD(rift.getMaxEntries());
			writeD(rift.getRemainTime());
			writeD(rift.getMinLevel());
			writeD(rift.getMaxLevel());
			writeF(rift.getOwner().getX());
			writeF(rift.getOwner().getY());
			writeF(rift.getOwner().getZ());
			writeC(rift.isVortex() ? 1 : 0);
			writeC(rift.isMaster() ? 1 : 0);
			writeD(rift.getOwner().getWorldId());
			writeD(rift.getAbyssPoint());
			break;
		case 3:
			writeH(0x15);
			writeC(actionId);
			writeD(rift.getOwner().getObjectId());
			writeD(rift.getUsedEntries());
			writeD(rift.getRemainTime());
			writeC(rift.isVortex() ? 1 : 0);
			writeC(rift.isMaster() ? 1 : 0);
			writeD(rift.getAbyssPoint());
			break;
		case 4:
			writeH(0x07);
			writeC(actionId);
			writeD(objectId);
			writeC(rift.isVortex() ? 1 : 0);
			writeC(rift.isMaster() ? 1 : 0);
			break;
		case 5:
			writeH(0x05);
			writeC(actionId);
			writeD(0x00);
			break;
		}
	}
}
