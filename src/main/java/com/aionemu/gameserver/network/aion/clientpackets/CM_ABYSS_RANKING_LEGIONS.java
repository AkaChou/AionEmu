package com.aionemu.gameserver.network.aion.clientpackets;

import com.aionemu.boot.i18n.I18n;
import lombok.extern.slf4j.Slf4j;
import com.aionemu.gameserver.lifecycle.GameCoreGameplayServices;

import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.gameobjects.player.AbyssRank.AbyssRankUpdateType;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.AionClientPacket;
import com.aionemu.gameserver.network.aion.AionConnection.State;
import com.aionemu.gameserver.network.aion.serverpackets.SM_ABYSS_RANKING_LEGIONS;
import com.aionemu.gameserver.services.abyss.AbyssRankingCache;

/**
 * 请求指定种族欧比斯军团排行榜的客户端包。
 * Client packet requesting abyss legion rankings for a race.
 *
 * @author SheppeR
 */
@Slf4j
public class CM_ABYSS_RANKING_LEGIONS extends AionClientPacket {

	private Race queriedRace;
	private AbyssRankUpdateType updateType;
	private int raceId;

	public CM_ABYSS_RANKING_LEGIONS(int opcode, State state, State... restStates) {
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
			updateType = AbyssRankUpdateType.LEGION_ELYOS;
			break;
		case 1:
			queriedRace = Race.ASMODIANS;
			updateType = AbyssRankUpdateType.LEGION_ASMODIANS;
			break;
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void runImpl() {
		// 计算排名并发送数据包 / calculate rankings and send packet
		if (queriedRace != null) {
			Player player = this.getConnection().getActivePlayer();
			if (player.isAbyssRankListUpdated(updateType)) {
				sendPacket(new SM_ABYSS_RANKING_LEGIONS(GameCoreGameplayServices.abyssRankingCache().getLastUpdate(), queriedRace));
			} else {
				SM_ABYSS_RANKING_LEGIONS results = GameCoreGameplayServices.abyssRankingCache().getLegions(queriedRace);
				sendPacket(results);
				player.setAbyssRankListUpdated(updateType);
			}
		} else {
			log.warn(I18n.get("log.ef3ebf4ed8f8", raceId));
		}
	}
}
