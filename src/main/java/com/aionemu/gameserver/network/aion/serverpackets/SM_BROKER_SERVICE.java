package com.aionemu.gameserver.network.aion.serverpackets;

import java.sql.Timestamp;
import java.util.Calendar;

import com.aionemu.gameserver.model.gameobjects.BrokerItem;
import com.aionemu.gameserver.model.gameobjects.Item;
import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;
import com.aionemu.gameserver.network.aion.iteminfo.ItemInfoBlob;
import com.aionemu.gameserver.network.aion.iteminfo.ItemInfoBlob.ItemBlobType;

/**
 * 交易行（经纪行）服务多模式服务端包：搜索结果、已登记物品、登记反馈、结算列表/图标、均价高低价等。
 * Multi-mode broker-service server packet: search results, registered items, registration feedback,
 * settled list/icon, and average/low/high price data.
 */
public class SM_BROKER_SERVICE extends AionServerPacket {
	private enum BrokerPacketType {
		SEARCHED_ITEMS(0), REGISTERED_ITEMS(1), REGISTER_ITEM(3), SHOW_SETTLED_ICON(5), SETTLED_ITEMS(5),
		REMOVE_SETTLED_ICON(6), AVE_LOW_HIGH_ITEM(7);

		private int id;

		private BrokerPacketType(int id) {
			this.id = id;
		}

		private int getId() {
			return id;
		}
	}

	private BrokerPacketType type;
	private BrokerItem[] brokerItems;
	private int itemsCount;
	private int startPage;
	private int message;
	private long settled_kinah;
	private int itemUniqueId;
	private long Ave7day;
	private long CurrentLow;
	private long CurrentHigh;
	private boolean IsLowHighSame;

	/**
	 * 登记物品成功/结果反馈。
	 * Registration result feedback for a broker item.
	 *
	 * @param brokerItem 登记的物品 / registered broker item
	 * @param message 结果消息码 / result message code
	 * @param itemsCount 当前已登记数量 / current registered item count
	 */
	public SM_BROKER_SERVICE(BrokerItem brokerItem, int message, int itemsCount) {
		this.type = BrokerPacketType.REGISTER_ITEM;
		this.brokerItems = new BrokerItem[] { brokerItem };
		this.message = message;
		this.itemsCount = itemsCount;
	}

	/**
	 * 仅发送登记结果消息码（无物品详情）。
	 * Sends only a registration result message code (no item details).
	 *
	 * @param message 结果消息码 / result message code
	 */
	public SM_BROKER_SERVICE(int message) {
		this.type = BrokerPacketType.REGISTER_ITEM;
		this.message = message;
	}

	/**
	 * 同步玩家当前已登记在交易行的物品列表。
	 * Synchronizes the player's currently registered broker items.
	 *
	 * @param brokerItems 已登记物品数组 / registered broker items
	 */
	public SM_BROKER_SERVICE(BrokerItem[] brokerItems) {
		this.type = BrokerPacketType.REGISTERED_ITEMS;
		this.brokerItems = brokerItems;
	}

	/**
	 * 同步可结算物品列表与已结算基纳。
	 * Synchronizes the settled-items list and settled kinah amount.
	 *
	 * @param brokerItems 可结算物品 / settled broker items
	 * @param settled_kinah 已结算基纳 / settled kinah
	 */
	public SM_BROKER_SERVICE(BrokerItem[] brokerItems, long settled_kinah) {
		this.type = BrokerPacketType.SETTLED_ITEMS;
		this.brokerItems = brokerItems;
		this.settled_kinah = settled_kinah;
	}

	/**
	 * 交易行搜索结果分页同步。
	 * Paged broker search-result synchronization.
	 *
	 * @param brokerItems 当前页物品 / items on the current page
	 * total hit count
	 * start page
	 */
	public SM_BROKER_SERVICE(BrokerItem[] brokerItems, int itemsCount, int startPage) {
		this.type = BrokerPacketType.SEARCHED_ITEMS;
		this.brokerItems = brokerItems;
		this.itemsCount = itemsCount;
		this.startPage = startPage;
	}

	/**
	 * 显示或移除交易行结算提示图标。
	 * Shows or removes the broker settled-items notification icon.
	 *
	 * @param showSettledIcon 是否显示图标 / whether to show the icon
	 * @param settled_kinah 已结算基纳 / settled kinah
	 */
	public SM_BROKER_SERVICE(boolean showSettledIcon, long settled_kinah) {
		this.type = showSettledIcon ? BrokerPacketType.SHOW_SETTLED_ICON : BrokerPacketType.REMOVE_SETTLED_ICON;
		this.settled_kinah = settled_kinah;
	}

	/**
	 * 同步指定物品近 7 日均价与当前最低/最高价。
	 * Synchronizes a given item's 7-day average price and current low/high prices.
	 *
	 * item unique id
	 * 7-day average price
	 * @param CurrentLow 当前最低价 / current low price
	 * @param CurrentHigh 当前最高价 / current high price
	 * @param IsLowHighSame 最低价是否等于最高价 / whether low equals high
	 */
	public SM_BROKER_SERVICE(int itemUniqueId, long Ave7day, long CurrentLow, long CurrentHigh, boolean IsLowHighSame) {
		this.type = BrokerPacketType.AVE_LOW_HIGH_ITEM;
		this.itemUniqueId = itemUniqueId;
		this.Ave7day = Ave7day;
		this.CurrentLow = CurrentLow;
		this.CurrentHigh = CurrentHigh;
		this.IsLowHighSame = IsLowHighSame;
	}

	@Override
	protected void writeImpl(AionConnection con) {
		switch (type) {
		case SEARCHED_ITEMS:
			writeSearchedItems();
			break;
		case REGISTERED_ITEMS:
			writeRegisteredItems();
			break;
		case REGISTER_ITEM:
			writeRegisterItem();
			break;
		case SHOW_SETTLED_ICON:
			writeShowSettledIcon();
			break;
		case REMOVE_SETTLED_ICON:
			writeRemoveSettledIcon();
			break;
		case SETTLED_ITEMS:
			writeShowSettledItems();
			break;
		case AVE_LOW_HIGH_ITEM:
			writeItemAveLowHigh();
			break;
		}
	}

	private void writeItemAveLowHigh() {
		writeC(type.getId());
		writeC(0x00);
		writeD(itemUniqueId);
		writeQ(Ave7day);
		writeC(IsLowHighSame ? 0x02 : 0x00);
		writeQ(CurrentLow);
		writeQ(CurrentHigh);
	}

	private void writeSearchedItems() {
		writeC(type.getId());
		writeD(itemsCount);
		writeC(0);
		writeH(startPage);
		writeH(brokerItems.length);
		for (BrokerItem item : brokerItems) {
			writeItemInfo(item);
		}
	}

	private void writeRegisteredItems() {
		writeC(type.getId());
		writeD(0x00);
		writeH(brokerItems.length);
		for (BrokerItem brokerItem : brokerItems) {
			writeRegisteredItemInfo(brokerItem);
		}
	}

	private void writeRegisterItem() {
		writeC(type.getId());
		writeC(message);
		if (message == 0) {
			writeC(itemsCount + 1);
			BrokerItem itemForRegistration = brokerItems[0];
			writeRegisteredItemInfo(itemForRegistration);
		} else {
			writeB(new byte[107]);
		}
	}

	private void writeShowSettledIcon() {
		writeC(type.getId());
		writeQ(settled_kinah);
		writeD(0x00);
		writeH(0x00);
		writeH(0x01);
		writeC(0x00);
	}

	private void writeRemoveSettledIcon() {
		writeH(type.getId());
	}

	private void writeShowSettledItems() {
		writeC(type.getId());
		writeQ(settled_kinah);
		writeH(brokerItems.length);
		writeD(0x00);
		writeC(0x00);
		writeH(brokerItems.length);
		for (BrokerItem settledItem : brokerItems) {
			writeD(settledItem.getItemId());
			if (settledItem.isSold()) {
				writeQ(settledItem.getPrice());
			} else {
				writeQ(0);
			}
			writeQ(settledItem.getItemCount());
			writeQ(settledItem.getItemCount());
			writeD((int) ((settledItem.getSettleTime().getTime() / 60000) & 0xffffffffl));
			Item item = settledItem.getItem();
			if (item != null) {
				ItemInfoBlob.newBlobEntry(ItemBlobType.MANA_SOCKETS, null, item).writeThisBlob(getBuf());
			} else {
				writeB(new byte[187]);
			}
			writeS(settledItem.getItemCreator());

			writeB(new byte[8]);// 5.5
		}
	}

	private void writeRegisteredItemInfo(BrokerItem brokerItem) {
		Item item = brokerItem.getItem();
		writeD(brokerItem.getItemUniqueId());
		writeD(brokerItem.getItemId());
		writeQ(brokerItem.getPrice());
		writeQ(item.getItemCount());
		writeQ(item.getItemCount());
		Timestamp currentTime = new Timestamp(Calendar.getInstance().getTimeInMillis());
		int daysLeft = (int) ((brokerItem.getExpireTime().getTime() - currentTime.getTime()) / 86400000);
		writeC(daysLeft);
		ItemInfoBlob.newBlobEntry(ItemBlobType.MANA_SOCKETS, null, item).writeThisBlob(getBuf());
		writeS(brokerItem.getItemCreator());
		ItemInfoBlob.newBlobEntry(ItemBlobType.PREMIUM_OPTION, null, item).writeThisBlob(getBuf());
		ItemInfoBlob.newBlobEntry(ItemBlobType.IDIAN_INFO, null, item).writeThisBlob(getBuf());
		writeC(0x00);
		writeC(brokerItem.isSplitSell() ? 0x01 : 0x00);

		writeD(188); // unk 5.5
		writeD(0x00); // unk 5.5
	}

	private void writeItemInfo(BrokerItem brokerItem) {
		Item item = brokerItem.getItem();
		writeD(item.getObjectId());
		writeD(item.getItemTemplate().getTemplateId());
		writeQ(brokerItem.getPrice());
		writeQ(brokerItem.getPrice());
		writeQ(item.getItemCount());
		ItemInfoBlob.newBlobEntry(ItemBlobType.MANA_SOCKETS, null, item).writeThisBlob(getBuf());
		writeS(brokerItem.getSeller());
		writeS(brokerItem.getItemCreator());
		ItemInfoBlob.newBlobEntry(ItemBlobType.PREMIUM_OPTION, null, item).writeThisBlob(getBuf());
		ItemInfoBlob.newBlobEntry(ItemBlobType.IDIAN_INFO, null, item).writeThisBlob(getBuf());
		writeC(0x00);
		writeC(brokerItem.isSplitSell() ? 0x01 : 0x00);
	}
}
