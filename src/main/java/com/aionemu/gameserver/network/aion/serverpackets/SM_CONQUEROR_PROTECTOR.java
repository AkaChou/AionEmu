package com.aionemu.gameserver.network.aion.serverpackets;

import java.util.ArrayList;
import java.util.Collection;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;

/**
 * 征服者/守护者状态包：同步 debuff 等级，或推送领地入侵扫描/雷达玩家列表。
 * Server packet for conqueror/protector status: debuff level, or territory intruder scan/radar player list.
 */
public class SM_CONQUEROR_PROTECTOR extends AionServerPacket {
	private int type;
	private int debuffLvl;
	private Collection<Player> players;

	/**
	 * debuff 等级同步（type 0/1）。
	 * Debuff-level sync (type 0/1).
	 *
	 * @param showMsg   是否显示消息 / whether to show a message
	 * debuff level
	 */
	public SM_CONQUEROR_PROTECTOR(boolean showMsg, int debuffLvl) {
		this.type = showMsg ? 1 : 0;
		this.debuffLvl = debuffLvl;
	}

	/**
	 * 领地入侵自动扫描玩家列表（type 4）。
	 * Automatic territory-intruder scan player list (type 4).
	 *
	 * @param players 扫描到的玩家 / scanned players
	 */
	public SM_CONQUEROR_PROTECTOR(Collection<Player> players) {
		this.type = 4;
		this.players = new ArrayList<Player>(players);
	}

	@Override
	protected void writeImpl(AionConnection con) {
		switch (type) {
		case 0:
		case 1:
			writeD(type);
			writeD(0x01);
			writeD(0x01);
			writeH(0x01);
			writeD(debuffLvl);
			break;
		case 4: // Automatic Territory Intruder Scan
			writeD(type);
			writeD(0x01);
			writeD(0x01);
			writeH(players.size());
			for (Player player : players) {
				writeD(player.getProtectorInfo().getRank());
				writeD(player.getProtectorInfo().getType());
				writeD(player.getConquerorInfo().getRank());
				writeD(player.getObjectId());
				writeD(0x01);
				writeD(player.getAbyssRank().getRank().getId());
				writeH(player.getLevel());
				writeF(player.getX());
				writeF(player.getY());
				writeS(player.getName(), 134);
				writeH(4);
			}
			break;
		case 5: // Intruder Radar
			writeH(players.size());
			for (Player player : players) {
				writeD(player.getProtectorInfo().getRank());
				writeD(player.getProtectorInfo().getType());
				writeD(player.getConquerorInfo().getRank());
				writeD(player.getObjectId());
				writeD(0x01);
				writeD(player.getAbyssRank().getRank().getId());
				writeH(player.getLevel());
				writeF(player.getX());
				writeF(player.getY());
				writeS(player.getName(), 134);
				writeH(4);
			}
			break;
		}
	}
}
