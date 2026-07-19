package com.aionemu.gameserver.network.aion.serverpackets;

import lombok.extern.slf4j.Slf4j;
import java.util.List;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;

/**
 * 月之商城列表/点数等状态同步的服务端包。
 * Server packet that synchronizes Luna shop list and related point/state data.
 *
 * @author Made by Rinzler (Encom)
 */
@Slf4j
public class SM_LUNA_SHOP_LIST extends AionServerPacket {

	private int actionId;
	private long points;
	private int keys;
	private int costId;
	private int entryCount;
	private int tableId;
	private List<Integer> idList;
	private List<Integer> randomDailyCraft;

	/**
	 * 通用动作构造，仅指定 actionId。
	 * Generic action constructor with action id only.
	 *
	 * action type
	 */
	public SM_LUNA_SHOP_LIST(int actionId) {
		this.actionId = actionId;
	}

	/**
	 * 同步月之点数。
	 * Syncs Luna points.
	 *
	 * action type
	 * Luna points
	 */
	public SM_LUNA_SHOP_LIST(int actionId, long points) {
		this.actionId = actionId;
		this.points = points;
	}

	/**
	 * 同步钥匙数量。
	 * Syncs key count.
	 *
	 * action type
	 * @param keys 钥匙数量 / key count
	 */
	public SM_LUNA_SHOP_LIST(int actionId, int keys) {
		this.actionId = actionId;
		this.keys = keys;
	}

	/**
	 * 下发配方 ID 列表（tableId = 0）。
	 * Delivers recipe id list (tableId = 0).
	 *
	 * @param actionId 动作类型（构造内固定为 2） / action type (forced to 2 inside)
	 * @param tableId 表 ID（构造内固定为 0） / table id (forced to 0 inside)
	 * recipe id list
	 */
	public SM_LUNA_SHOP_LIST(int actionId, int tableId, List<Integer> idList) {
		this.actionId = 2;
		this.tableId = 0;
		this.idList = idList;
	}

	/**
	 * 下发每日随机制作配方列表（tableId = 1）。
	 * Delivers random daily craft recipe list (tableId = 1).
	 *
	 * @param randomDailyCraft 每日随机配方 ID 列表 / random daily craft recipe ids
	 */
	public SM_LUNA_SHOP_LIST(List<Integer> randomDailyCraft) {
		this.actionId = 2;
		this.tableId = 1;
		this.randomDailyCraft = randomDailyCraft;
	}

	/**
	 * 同步指定表的消耗/费用 ID。
	 * Syncs cost id for a given table.
	 *
	 * action type
	 * table id
	 * cost id
	 */
	public SM_LUNA_SHOP_LIST(int actionId, int tableId, int costId) {
		this(actionId, tableId, costId, 1);
	}

	public SM_LUNA_SHOP_LIST(int actionId, int tableId, int costId, int entryCount) {
		this.actionId = actionId;
		this.tableId = tableId;
		this.costId = costId;
		this.entryCount = entryCount;
	}

	@Override
	protected void writeImpl(AionConnection con) {
		Player player = con.getActivePlayer();
		writeC(actionId);// actionid
		switch (actionId) {
		case 0:// luna point handler id
			writeQ(con.getAccount().getLuna());
			break;
		case 1:// taki advanture update
			writeH(tableId);// size?
			writeD(costId);
			writeD(entryCount);
			break;
		case 2:
			writeC(tableId);// tabId
			switch (tableId) {
			case 0:
				writeD(1474466400);// Start time
				writeD(0);
				writeD(1476280799);// End time
				writeD(0);
				writeH(idList.size());// size
				for (int i = 0; i < idList.size(); i++) {
					writeD(idList.get(i));// luna recipe id
				}
				break;
			case 1:
				writeD(1482393600);
				writeD(0); // test
				writeD(1482480000);
				writeD(0);
				writeH(randomDailyCraft.size());// size
				for (int i = 0; i < randomDailyCraft.size(); i++) {
					writeD(randomDailyCraft.get(i));// luna recipe id
				}
				break;
			}
			break;
		case 4:// munirunerk's keys
			writeD(con.getActivePlayer().getMuniKeys());
			break;
		case 5:// luna consume point spent
			writeD(con.getActivePlayer().getLunaConsumePoint());
			break;
		case 6:// update taki's mission?
			break;
		case 7:
			writeC(0);
			writeH(100);
			break;
		case 8:// Updated for 5.8 on 16.05.2018
			writeH(tableId <= 5 ? tableId : (tableId - 1));
			writeD(10 + tableId);
			writeC((tableId + 10) == 16 ? 1 : 0);
			break;
		case 9:
			writeH(-1);
			writeC(0);
		}
	}
}
