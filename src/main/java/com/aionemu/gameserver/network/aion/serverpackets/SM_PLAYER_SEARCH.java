package com.aionemu.gameserver.network.aion.serverpackets;


import com.aionemu.boot.i18n.I18n;
import lombok.extern.slf4j.Slf4j;
import java.util.ArrayList;
import java.util.List;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;
/**
 * 玩家搜索结果列表服务端包。
 * Server packet that returns a player-search result list.
 */
@Slf4j
public class SM_PLAYER_SEARCH extends AionServerPacket {

	private List<Player> players;
	private int region;

	/**
	 * @param players 匹配到的玩家列表 / matched players
	 * @param region 回退区域/地图 ID（玩家无活动区域时使用） / fallback region/map id when player has no active region
	 */
	public SM_PLAYER_SEARCH(List<Player> players, int region) {
		this.players = new ArrayList<Player>(players);
		this.region = region;
	}

	@Override
	protected void writeImpl(AionConnection con) {
		writeH(players.size());
		for (Player player : players) {
			if (player.getActiveRegion() == null) {
				// log.warn(I18n.get("log.d1be3036fb78", player.getObjectId(), // player.getX(), player.getY(), player.getZ()));
			}
			writeD(player.getActiveRegion() == null ? region : player.getActiveRegion().getMapId());
			writeF(player.getPosition().getX());
			writeF(player.getPosition().getY());
			writeF(player.getPosition().getZ());
			writeC(player.getPlayerClass().getClassId());
			writeC(player.getGender().getGenderId());
			writeC(player.getLevel());
			if (player.isInGroup2()) {
				writeC(3);
			} else if (player.isLookingForGroup()) {
				writeC(2);
			} else {
				writeC(0);
			}
			writeS(player.getName(), 56);
		}
	}
}
