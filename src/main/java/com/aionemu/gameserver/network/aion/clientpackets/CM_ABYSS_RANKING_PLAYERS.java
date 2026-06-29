/*

 *
 *  Encom is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Lesser Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  Encom is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Lesser Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser Public License
 *  along with Encom.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.aionemu.gameserver.network.aion.clientpackets;

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
			log.warn("Received invalid raceId: " + raceId);
		}
	}
}