package com.aionemu.gameserver.network.aion.serverpackets;

import lombok.extern.slf4j.Slf4j;
import java.util.List;

import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;
import lombok.RequiredArgsConstructor;

/**
 * 月之商城列表/点数等状态同步的服务端包。
 * Server packet that synchronizes Luna shop list and related point/state data.
 * @author Made by Rinzler (Encom)
 */
@Slf4j
@RequiredArgsConstructor
public class SM_LUNA_SHOP_LIST extends AionServerPacket {

	private final int actionId;
	private int costId;
	private int tableId;
	private List<Integer> recipeIds;

	/**
	 * 下发配方 ID 列表（tableId：0=全部配方、1=每日随机制作）。
	 * Delivers recipe ids (tableId: 0=all recipes, 1=random daily craft).
	 * @param tableId 表 ID / table id
	 * @param recipeIds 配方 ID 列表 / recipe ids
	 */
	public SM_LUNA_SHOP_LIST(int tableId, List<Integer> recipeIds) {
		this.actionId = 2;
		this.tableId = tableId;
		this.recipeIds = recipeIds;
	}

	/**
	 * 同步指定表的消耗/费用 ID。
	 * Syncs cost id for a given table.
	 * @param actionId action type
	 * @param tableId table id
	 * @param costId cost id
	 */
	public SM_LUNA_SHOP_LIST(int actionId, int tableId, int costId) {
		this.actionId = actionId;
		this.tableId = tableId;
		this.costId = costId;
	}

	@Override
	protected void writeImpl(AionConnection con) {
		writeC(actionId);// actionid
		switch (actionId) {
		case 0:// luna point handler id
			writeQ(con.getAccount().getLuna());
			break;
		case 1:// taki advanture update
			writeH(tableId);// size?
			writeD(costId);
			writeD(1);
			break;
		case 2:
			writeC(tableId);// tabId
			switch (tableId) {
			case 0:
				writeD(1474466400);// Start time
				writeD(0);
				writeD(1476280799);// End time
				writeD(0);
				writeH(recipeIds.size());// size
				for (int recipeId : recipeIds) {
					writeD(recipeId);// luna recipe id
				}
				break;
			case 1:
				writeD(1482393600);
				writeD(0); // test
				writeD(1482480000);
				writeD(0);
				writeH(recipeIds.size());// size
				for (int recipeId : recipeIds) {
					writeD(recipeId);// luna recipe id
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
