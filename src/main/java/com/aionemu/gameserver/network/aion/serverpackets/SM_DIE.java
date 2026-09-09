package com.aionemu.gameserver.network.aion.serverpackets;

import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;
import lombok.AllArgsConstructor;

/**
 * 玩家死亡界面包：重生/道具可用标志、Kisk 剩余时间、死亡类型与入侵标记。
 * Death UI packet: rebirth/item flags, remaining kisk time, death type and invasion flag.
 */
@AllArgsConstructor
public class SM_DIE extends AionServerPacket {
	private final boolean hasRebirth;
	private final boolean hasItem;
	private final int remainingKiskTime;
	private int type = 0;
	private final boolean invasion;

	/**
	 * @param hasRebirth        是否可用技能重生 / skill rebirth available
	 * @param hasItem           是否可用道具重生 / item rebirth available
	 * remaining kisk time
	 * @param type              死亡类型 / death type
	 */
	public SM_DIE(boolean hasRebirth, boolean hasItem, int remainingKiskTime, int type) {
		this(hasRebirth, hasItem, remainingKiskTime, type, false);
	}

	@Override
	protected void writeImpl(AionConnection con) {
		writeC((hasRebirth ? 1 : 0));
		writeC((hasItem ? 1 : 0));
		writeD(remainingKiskTime);
		writeC(type);
		writeC(invasion ? 0x80 : 0x00);
	}
}
