package com.aionemu.gameserver.network.aion.clientpackets;

import com.aionemu.boot.i18n.I18n;
import lombok.extern.slf4j.Slf4j;
import com.aionemu.gameserver.lifecycle.GameCoreGameplayServices;

import java.util.List;

import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.gameobjects.player.AbyssRank.AbyssRankUpdateType;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.AionClientPacket;
import com.aionemu.gameserver.network.aion.AionConnection.State;
import com.aionemu.gameserver.network.aion.serverpackets.SM_ABYSS_RANKING_PLAYERS;
import com.aionemu.gameserver.services.abyss.AbyssRankingCache;

/**
 * 请求指定种族欧比斯玩家排行榜的客户端包。
 * Client packet requesting abyss player rankings for a race.
 *
 * @author SheppeR
 */
@Slf4j
public class CM_ABYSS_RANKING_PLAYERS extends AionClientPacket {

	private Race queriedRace;
	private int raceId;
	private AbyssRankUpdateType updateType;

	public CM_ABYSS_RANKING_PLAYERS(int opcode, State state, State... restStates) {
		super(opcode, state, restStates);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void readImpl() {
		raceId = readC();
		switch (raceId) {
		case 0:
			queriedRace = Race.ELYOS;
			updateType = AbyssRankUpdateType.PLAYER_ELYOS;
			break;
		case 1:
			queriedRace = Race.ASMODIANS;
			updateType = AbyssRankUpdateType.PLAYER_ASMODIANS;
			break;
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void runImpl() {
		if (queriedRace != null) {
			Player player = this.getConnection().getActivePlayer();
			if (player.isAbyssRankListUpdated(updateType)) {
				sendPacket(new SM_ABYSS_RANKING_PLAYERS(GameCoreGameplayServices.abyssRankingCache().getLastUpdate(), queriedRace));
			} else {
				List<SM_ABYSS_RANKING_PLAYERS> results = GameCoreGameplayServices.abyssRankingCache().getPlayers(queriedRace);
				for (SM_ABYSS_RANKING_PLAYERS packet : results) {
					sendPacket(packet);
				}
				player.setAbyssRankListUpdated(updateType);
			}
		} else {
			log.warn(I18n.get("log.ef3ebf4ed8f8", raceId));
		}
	}
}
