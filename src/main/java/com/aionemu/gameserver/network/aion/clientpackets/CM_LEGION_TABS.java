package com.aionemu.gameserver.network.aion.clientpackets;

import com.aionemu.boot.i18n.I18n;
import lombok.extern.slf4j.Slf4j;
import java.util.Collection;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.team.legion.LegionHistory;
import com.aionemu.gameserver.network.aion.AionClientPacket;
import com.aionemu.gameserver.network.aion.AionConnection.State;
import com.aionemu.gameserver.network.aion.serverpackets.SM_LEGION_TABS;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * 请求军团历史/仓库历史分页标签的客户端包。
 * Client packet requesting legion history/warehouse-history tab pages.
 *
 * @author Simple, xTz
 */
@Slf4j
public class CM_LEGION_TABS extends AionClientPacket {

	private int page;
	private int tab;
	/**
	 * 构造该客户端包。
	 * Constructs this client packet.
	 *
	 * packet opcode
	 * @param state 连接状态 / connection state
	 * @param restStates 其余合法状态 / additional valid states
	 */
	public CM_LEGION_TABS(int opcode, State state, State... restStates) {
		super(opcode, state, restStates);
	}
	/**
	 * 读取军团页签类型与页码。
	 * Reads legion tab type and page number.
	 */
	@Override
	protected void readImpl() {
		page = readD();
		tab = readC();
	}
	/**
	 * 发送军团历史或仓库历史分页数据。
	 * Sends legion history or warehouse-history page data.
	 */
	@Override
	protected void runImpl() {
		Player activePlayer = getConnection().getActivePlayer();

		if (activePlayer.getLegion() != null) {

			/**
	 * 军团历史最多 16 页 / Max page is 16 for legion history
	 */
			if (page < 0 || page > 16) {
				return;
			}
			switch (tab) {
			/**
	 * 历史页签 / History Tab
	 */
			case 0: // legion history
			case 2: // legion WH history
				Collection<LegionHistory> history = activePlayer.getLegion().getLegionHistoryByTabId(tab);
				/**
	 * 若历史条数不足 page*8 则返回 / If history size is less than page*8 return
	 */
				if (history.size() < page * 8) {
					return;
				}
				if (!history.isEmpty()) {
					PacketSendUtility.sendPacket(activePlayer, new SM_LEGION_TABS(history, page, tab));
				}
				break;
			}
		} else {
			log.warn(I18n.get("log.a169a1174c51", activePlayer.getName()));
		}
	}
}
