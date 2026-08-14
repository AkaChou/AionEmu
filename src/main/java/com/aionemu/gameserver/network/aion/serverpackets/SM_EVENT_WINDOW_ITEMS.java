package com.aionemu.gameserver.network.aion.serverpackets;


import com.aionemu.boot.i18n.I18n;
import lombok.extern.slf4j.Slf4j;
import java.sql.Timestamp;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;

import com.aionemu.commons.database.dao.DAOManager;
import com.aionemu.gameserver.dao.PlayerEventsWindowDAO;
import com.aionemu.gameserver.model.templates.event.EventsWindow;
import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;

/**
 * 活动窗口条目列表包：下发各活动奖励物品、剩余时间、等级与周期。
 * Event-window items list: rewards, remaining time, level range and period per event.
 */
@Slf4j
public class SM_EVENT_WINDOW_ITEMS extends AionServerPacket {

	private Collection<EventsWindow> active_events_packet;

	/**
	 * 按活跃活动集合构造条目列表包（拷贝副本保持顺序）。
	 * Creates an event-window items packet from the active events (copies the collection).
	 *
	 * @param active_events_packet 活跃活动集合 / collection of active events
	 */
	public SM_EVENT_WINDOW_ITEMS(Collection<EventsWindow> active_events_packet) {
		this.active_events_packet = new ArrayList<EventsWindow>(active_events_packet);
	}

	@Override
	protected void writeImpl(AionConnection aionConnection) {
		int playerAccountId = aionConnection.getActivePlayer().getPlayerAccount().getId();
		PlayerEventsWindowDAO playerEventsWindowDAO = DAOManager.getDAO(PlayerEventsWindowDAO.class);
		writeC(1); // 请勿修改！！！ / Do not Change !!!
		writeH(active_events_packet.size());
		for (EventsWindow eventsWindow : active_events_packet) {
			int dbRecivedCount = playerEventsWindowDAO.getRewardRecivedCount(playerAccountId, eventsWindow.getId());
			int elapsed = playerEventsWindowDAO.getElapsed(playerAccountId, eventsWindow.getId());
			int displayTime = (eventsWindow.getRemainingTime() - elapsed);

			long periodStartMillis = eventsWindow.getPeriodStart().toInstant().toEpochMilli();
			long periodEndMillis = eventsWindow.getPeriodEnd().toInstant().toEpochMilli();

			log.info(I18n.get("log.860897574830", eventsWindow.getId(), eventsWindow.getRemainingTime(), new Timestamp(periodStartMillis).getTime() / 1000, new Timestamp(periodEndMillis).getTime() / 1000, active_events_packet.size()));

			writeD(eventsWindow.getId()); // Id
			writeD(dbRecivedCount); // reward recived count
			writeD(displayTime * 60); // Displayed Remaining Time
			writeD(0); // 请勿修改！！！ / Do not Change !!!
			writeD(eventsWindow.getMaxCountOfDay());// This is Max Count of Day
			writeD((int) (System.currentTimeMillis() / 1000)); // PlayerLoginTime
			writeC(1); // 请勿修改！！！ / Do not Change !!!
			writeD(5); // 请勿修改！！！ / Do not Change !!!
			writeD(1); // 请勿修改！！！ / Do not Change !!!
			writeC(-104); // 请勿修改！！！ / Do not Change !!!
			writeC(98); // 请勿修改！！！ / Do not Change !!!
			writeC(21); // 请勿修改！！！ / Do not Change !!!
			writeC(0); // 请勿修改！！！ / Do not Change !!!
			writeD(displayTime * 60); // Remaining Time
			writeD(eventsWindow.getItemId()); // ItemId
			writeQ(eventsWindow.getCount()); // ItemCount
			writeD(eventsWindow.getMaxCountOfDay()); // This is Max Count of Day
			writeD((int) (periodStartMillis / 1000)); // Period Start TimeStamp
			writeD(0);
			writeD((int) (periodEndMillis / 1000)); // Period End TimeSTamp
			writeD(0);
			writeD(0);// Does something
			writeD(0); // If player has this Item already in inventory it's ItemId
			writeD(1090157056); // 请勿修改！！！ / Do not Change !!!
			writeD(eventsWindow.getMinLevel()); // StartLevel
			writeD(eventsWindow.getMaxLevel()); // EndLevel
			writeD(-1);// 请勿修改！！！ / Do not Change !!!
			writeB(new byte[84]); // 请勿修改！！！ / Do not Change !!!
			writeD(-1);// 请勿修改！！！ / Do not Change !!!
			writeB(new byte[16]);
			writeD(2147483647);// 请勿修改！！！ / Do not Change !!!
			writeB(new byte[7]);// 请勿修改！！！ / Do not Change !!!
			writeD(-1);// 请勿修改！！！ / Do not Change !!!
			writeD(0);// 请勿修改！！！ / Do not Change !!!
		}
	}
}
