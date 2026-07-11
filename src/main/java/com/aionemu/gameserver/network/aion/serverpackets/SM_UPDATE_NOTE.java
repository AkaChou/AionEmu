package com.aionemu.gameserver.network.aion.serverpackets;

import lombok.extern.slf4j.Slf4j;

import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * 更新玩家个性签名/备注的服务端包。
 * signature. / signature.
 *
 * @author xavier
 */
@Slf4j
public class SM_UPDATE_NOTE extends AionServerPacket {

	private int targetObjId;
	private String note;

	/**
	 * target object id
	 * @param note        新备注内容 / new note text
	 */
	public SM_UPDATE_NOTE(int targetObjId, String note) {
		this.targetObjId = targetObjId;
		this.note = note;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void writeImpl(AionConnection con) {
		writeD(targetObjId);
		writeS(note);
		PacketSendUtility.sendPacket(con.getActivePlayer(), new SM_SYSTEM_MESSAGE(1390124, note));
		log.debug("Updated note. targetObjId={} noteLength={}", targetObjId, note == null ? 0 : note.length());
	}
}
