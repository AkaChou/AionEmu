package com.aionemu.gameserver.network.aion.serverpackets;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;

/**
 * 开启/确认整容（含改性别）界面的服务端包。
 * Server packet that opens or confirms the plastic-surgery (including sex-change) UI.
 *
 * @author IlBuono
 */
public class SM_PLASTIC_SURGERY extends AionServerPacket {

	private int playerObjId;
	private byte check_ticket;
	private byte change_sex;

	/**
	 * target player
	 * @param check_ticket 是否校验整容券 / whether a surgery ticket is required
	 * @param change_sex 是否允许改性别 / whether sex change is allowed
	 */
	public SM_PLASTIC_SURGERY(Player player, byte check_ticket, byte change_sex) {
		this.playerObjId = player.getObjectId();
		this.check_ticket = check_ticket;
		this.change_sex = change_sex;
	}

	@Override
	protected void writeImpl(AionConnection con) {
		writeD(playerObjId);
		writeC(check_ticket);
		writeC(change_sex);
	}
}
