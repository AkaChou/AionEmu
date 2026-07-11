package com.aionemu.gameserver.network.aion.serverpackets;

import java.util.Collection;

import com.aionemu.gameserver.model.team.legion.LegionHistory;
import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;

/**
 * 向客户端发送军团历史记录分页标签数据的服务端包。
 * Server packet that sends paged legion history tab data to the client.
 *
 * @author Simple, KID, xTz
 */
public class SM_LEGION_TABS extends AionServerPacket {

	private int page;
	private Collection<LegionHistory> legionHistory;
	private int tabId;

	/**
	 * 使用历史记录与标签 ID 构造首页数据包。
	 * Creates a first-page packet from history entries and a tab id.
	 *
	 * @param legionHistory 军团历史记录集合 / legion history collection
	 * tab id
	 */
	public SM_LEGION_TABS(Collection<LegionHistory> legionHistory, int tabId) {
		this.legionHistory = legionHistory;
		this.page = 0;
		this.tabId = tabId;
	}

	/**
	 * 使用历史记录、页码与标签 ID 构造分页数据包。
	 * Creates a paged packet from history entries, page index and tab id.
	 *
	 * @param legionHistory 军团历史记录集合 / legion history collection
	 * @param page 页码（从 0 起） / page index (0-based)
	 * tab id
	 */
	public SM_LEGION_TABS(Collection<LegionHistory> legionHistory, int page, int tabId) {
		this.legionHistory = legionHistory;
		this.page = page;
		this.tabId = tabId;
	}

	/**
	 * 按页写出最多 8 条历史记录及标签信息。
	 * Writes up to 8 history entries for the requested page and the tab id.
	 */
	@Override
	protected void writeImpl(AionConnection con) {
		int size = legionHistory.size();
		/**
	 * 若历史条数不足 page*8 则返回 / If history size is less than page*8 return
	 */
		if (size < (page * 8)) {
			return;
		}
		int hisSize = Math.min(8, size - page * 8);
		writeD(size);
		writeD(page); // current page
		writeD(hisSize);

		int i = 0;
		for (LegionHistory history : legionHistory) {
			if (i >= (page * 8) && i <= (8 + (page * 8))) {
				writeD((int) (history.getTime().getTime() / 1000));
				writeC(history.getLegionHistoryType().getHistoryId());
				writeC(0); // 未知 / unk
				writeS(history.getName(), 64);
				writeH(0); // separator
				writeS(history.getDescription(), 64);
				writeD(0);
			}
			i++;
			if (i >= (8 + (page * 8))) {
				break;
			}
		}
		writeC(tabId);
		writeC(0);
	}
}
