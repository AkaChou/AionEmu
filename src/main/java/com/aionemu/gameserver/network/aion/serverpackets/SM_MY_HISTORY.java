package com.aionemu.gameserver.network.aion.serverpackets;

import com.aionemu.gameserver.model.gameobjects.player.ranking.Arena6V6Ranking;
import com.aionemu.gameserver.model.gameobjects.player.ranking.ArenaOfTenacityRank;
import com.aionemu.gameserver.model.gameobjects.player.ranking.GoldArenaRank;
import com.aionemu.gameserver.model.gameobjects.player.ranking.TowerOfChallengeRank;
import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;

/**
 * 玩家竞技/排行历史服务端包。
 * ranking history to the client. / ranking history to the client.
 * <p>
 * 按 tableId 区分排行榜类型：1=坚韧殿堂、2=试炼之塔、3=大竞技训练营、541=孤独竞技场。
 * tableId selects ranking type: 1=Hall of Tenacity, 2=Crucible Spire,
 * 3=Grand Arena Training Camp, 541=Arena of Discipline.
 *
 * @author Wnkrz
 */
public class SM_MY_HISTORY extends AionServerPacket {

	private int tableId;
	private GoldArenaRank gold;
	private TowerOfChallengeRank tower;
	private Arena6V6Ranking arena6v6;
	private ArenaOfTenacityRank arenaOfTenacity;

	/**
	 * 构造黄金竞技场（坚韧殿堂）历史包。
	 * Builds a Gold Arena (Hall of Tenacity) history packet.
	 *
	 * ranking table id
	 * @param ranking 黄金竞技场排名数据 / gold arena rank data
	 */
	public SM_MY_HISTORY(int tableId, GoldArenaRank ranking) {
		this.tableId = tableId;
		this.gold = ranking;
	}

	/**
	 * 构造试炼之塔历史包。
	 * Builds a Tower of Challenge (Crucible Spire) history packet.
	 *
	 * ranking table id
	 * @param ranking 试炼之塔排名数据 / tower rank data
	 */
	public SM_MY_HISTORY(int tableId, TowerOfChallengeRank ranking) {
		this.tableId = tableId;
		this.tower = ranking;
	}

	/**
	 * 构造 6v6 竞技场历史包。
	 * Builds an Arena 6v6 history packet.
	 *
	 * ranking table id
	 * 6v6 rank data
	 */
	public SM_MY_HISTORY(int tableId, Arena6V6Ranking ranking) {
		this.tableId = tableId;
		this.arena6v6 = ranking;
	}

	/**
	 * 构造孤独竞技场（Arena of Tenacity）历史包。
	 * Builds an Arena of Tenacity history packet.
	 *
	 * ranking table id
	 * @param ranking 孤独竞技场排名数据 / arena of tenacity rank data
	 */
	public SM_MY_HISTORY(int tableId, ArenaOfTenacityRank ranking) {
		this.tableId = tableId;
		this.arenaOfTenacity = ranking;
	}

	protected void writeImpl(AionConnection paramAionConnection) {
		writeD(tableId);
		switch (tableId) {
		case 1: // Hall Of Tenacity
			writeD(gold.getRank()); // actual Rank
			writeD(gold.getPoints()); // current Points
			writeD(0);
			writeD(0);
			writeD(gold.getBestRank());// lasted rank
			writeD(gold.getLastPoints()); // last Points
			writeD(gold.getLowPoints()); // low points
			writeD(gold.getHighPoints()); // hight points
			return;
		case 541: // Arena Of Discipline
			writeD(arenaOfTenacity.getRank()); // actual Rank
			writeD(arenaOfTenacity.getPoints()); // current Points
			writeD(1);
			writeD(arenaOfTenacity.getPossitionMatch()); // possition match
			writeD(arenaOfTenacity.getBestRank());// lasted rank
			writeD(arenaOfTenacity.getLastPoints()); // last Points
			writeD(arenaOfTenacity.getLowPoints()); // low points
			writeD(arenaOfTenacity.getHighPoints()); // hight points
			return;
		case 2: // Crucible Spire.
			writeD(tower.getRank()); // actual Rank
			writeD(tower.getCurrentTime()); // actual Time
			writeD(0);
			writeD(0);
			writeD(tower.getBestRank());// lasted rank
			writeD(tower.getLastTime()); // last time
			writeD(tower.getLowRank()); // low rank
			writeD(tower.getBestTime()); // best time
			return;
		case 3: // Grand Arena Training Camp
			writeD(arena6v6.getRank()); // actual Rank
			writeD(arena6v6.getPoints()); // current Points
			writeD(1);
			writeD(arena6v6.getPossitionMatch()); // possition match
			writeD(arena6v6.getBestRank());// lasted rank
			writeD(arena6v6.getLastPoints()); // last Points
			writeD(arena6v6.getLowPoints()); // low points
			writeD(arena6v6.getHighPoints()); // hight points
			return;
		default:
			writeD(0); // actual Rank
			writeD(0); // current Points
			writeD(0);
			writeD(0);
			writeD(0);// lasted rank
			writeD(0); // last Points
			writeD(0); // low points
			writeD(0); // hight points
			return;
		}
	}
}
