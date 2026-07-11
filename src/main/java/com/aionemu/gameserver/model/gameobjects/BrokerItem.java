package com.aionemu.gameserver.model.gameobjects;

import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Comparator;

import org.apache.commons.lang3.StringUtils;

import com.aionemu.gameserver.configs.main.BrokerConfig;
import com.aionemu.gameserver.model.broker.BrokerRace;

/**
 * 经纪行物品游戏对象。
 * Broker Item game object.
 */

public class BrokerItem implements Comparable<BrokerItem> {
	private Item item;
	private int itemId;
	private int itemUniqueId;
	private long itemCount;
	private String itemCreator;
	private long price;
	private String seller;
	private int sellerId;
	private BrokerRace itemBrokerRace;
	private boolean isSold, isCanceled;
	private boolean isSettled;
	private Timestamp expireTime;
	private Timestamp settleTime;
	private boolean isSplitSell;
	PersistentState state;
	private int ExpireTimeinMillis = BrokerConfig.ITEMS_EXPIRE_TIME * 24 * 3600 * 1000;

	public BrokerItem(Item item, long price, String seller, int sellerId, BrokerRace itemBrokerRace,
			boolean isSplitSell) {
		this.item = item;
		this.itemId = item.getItemTemplate().getTemplateId();
		this.itemUniqueId = item.getObjectId();
		this.itemCount = item.getItemCount();
		this.itemCreator = item.getItemCreator();
		this.price = price;
		this.seller = seller;
		this.sellerId = sellerId;
		this.itemBrokerRace = itemBrokerRace;
		this.isSold = false;
		this.isSettled = false;
		this.expireTime = new Timestamp(Calendar.getInstance().getTimeInMillis() + ExpireTimeinMillis);
		this.settleTime = new Timestamp(Calendar.getInstance().getTimeInMillis());
		this.isSplitSell = isSplitSell;
		this.state = PersistentState.NEW;
	}

	public BrokerItem(Item item, int itemId, int itemUniqueId, long itemCount, String itemCreator, long price,
			String seller, int sellerId, BrokerRace itemBrokerRace, boolean isSold, boolean isSettled,
			Timestamp expireTime, Timestamp settleTime, boolean isSplitSell) {
		this.item = item;
		this.itemId = itemId;
		this.itemUniqueId = itemUniqueId;
		this.itemCount = itemCount;
		this.itemCreator = itemCreator;
		this.price = price;
		this.seller = seller;
		this.sellerId = sellerId;
		this.itemBrokerRace = itemBrokerRace;
		this.isSplitSell = isSplitSell;
		if (item == null) {
			this.isSold = true;
			this.isSettled = true;

		} else {
			this.isSold = isSold;
			this.isSettled = isSettled;
		}
		this.expireTime = expireTime;
		this.settleTime = settleTime;
		this.state = PersistentState.NOACTION;
	}

	/** 返回 item creator / Returns the item creator */
	public String getItemCreator() {
		if (itemCreator == null) {
			return StringUtils.EMPTY;
		}
		return itemCreator;
	}

	/** 设置物品制作者 / Sets the item creator */
	public void setItemCreator(String itemCreator) {
		this.itemCreator = itemCreator;
	}

	/** 获取物品。 / Returns the item. */
	public Item getItem() {
		return item;
	}

	/**
	 * @return Whether canceled / Whether canceled
	 */
	public boolean isCanceled() {
		return isCanceled;
	}

	/** 设置 is canceled / Sets the is canceled */
	public void setIsCanceled(boolean isCanceled) {
		this.isCanceled = isCanceled;
	}

	/** 移除物品。 / Removes item. */
	public void removeItem() {
		this.isSold = true;
		this.isSettled = true;
		this.settleTime = new Timestamp(Calendar.getInstance().getTimeInMillis());
	}

	/** 返回物品 ID / Returns the item id */
	public int getItemId() {
		return itemId;
	}

	/** 返回物品唯一 ID / Returns the item unique id */
	public int getItemUniqueId() {
		return itemUniqueId;
	}

	/** 获取价格。 / Returns the price. */
	public long getPrice() {
		return price;
	}

	/**
	 * @return Whether split sell / Whether split sell
	 */
	public boolean isSplitSell() {
		return this.isSplitSell;
	}

	/** 返回 seller / Returns the seller */
	public String getSeller() {
		return seller;
	}

	/** 返回 seller id / Returns the seller id */
	public int getSellerId() {
		return sellerId;
	}

	/** 获取物品经纪行种族。 / Returns the item broker race. */
	public BrokerRace getItemBrokerRace() {
		return itemBrokerRace;
	}

	/**
	 * @return 是否 sold / 是否 sold。 / Whether sold / Whether sold
	 */
	public boolean isSold() {
		return this.isSold;
	}

	/** 设置持久化状态。 / Sets the persistent state. */
	public void setPersistentState(PersistentState persistentState) {
		switch (persistentState) {
		case DELETED:
			if (this.state == PersistentState.NEW) {
				this.state = PersistentState.NOACTION;
			} else {
				this.state = PersistentState.DELETED;
			}
			break;
		case UPDATE_REQUIRED:
			if (this.state == PersistentState.NEW) {
				break;
			}
			break;
		default:
			this.state = persistentState;
		}
	}

	/** 获取持久化状态。 / Returns the persistent state. */
	public PersistentState getPersistentState() {
		return state;
	}

	/**
	 * @return Whether settled / Whether settled
	 */
	public boolean isSettled() {
		return isSettled;
	}

	/** 设置 settled / Sets the settled */
	public void setSettled() {
		this.isSettled = true;
		this.settleTime = new Timestamp(Calendar.getInstance().getTimeInMillis());
	}

	/** 获取过期时间。 / Returns the expire time. */
	public Timestamp getExpireTime() {
		return expireTime;
	}

	/** 返回 settle time / Returns the settle time */
	public Timestamp getSettleTime() {
		return settleTime;
	}

	/** 获取物品计数。 / Returns the item count. */
	public long getItemCount() {
		return itemCount;
	}

	private int getItemLevel() {
		return item.getItemTemplate().getLevel();
	}

	/** 返回 piece price / Returns the piece price */
	public long getPiecePrice() {
		return getPrice() / getItemCount();
	}

	private String getItemName() {
		return item.getItemName();
	}

	/** 设置物品计数。 / Sets the item count. */
	public void setItemCount(long count) {
		this.itemCount = count;
	}

	/** 设置价格。 / Sets the price. */
	public void setPrice(long ItemPrice) {
		this.price = ItemPrice;
	}

	/** 设置物品唯一 ID / Sets the item unique id */
	public void setItemUniqueId(int newObjId) {
		itemUniqueId = newObjId;
	}

	/** 比较。 / Compares to another instance. */
	@Override
	public int compareTo(BrokerItem o) {
		return itemUniqueId > o.getItemUniqueId() ? 1 : -1;
	}

	static Comparator<BrokerItem> NAME_SORT_ASC = new Comparator<BrokerItem>() {
		/** 比较 / compare. */
		@Override
		public int compare(BrokerItem o1, BrokerItem o2) {
			if (o1 == null || o2 == null) {
				return comparePossiblyNull(o1, o2);
			}
			return o1.getItemName().compareTo(o2.getItemName());
		}
	};

	static Comparator<BrokerItem> NAME_SORT_DESC = new Comparator<BrokerItem>() {
		/** 比较 / compare. */
		@Override
		public int compare(BrokerItem o1, BrokerItem o2) {
			if (o1 == null || o2 == null) {
				return comparePossiblyNull(o1, o2);
			}
			return o1.getItemName().compareTo(o2.getItemName());
		}
	};

	static Comparator<BrokerItem> PRICE_SORT_ASC = new Comparator<BrokerItem>() {
		/** 比较 / compare. */
		@Override
		public int compare(BrokerItem o1, BrokerItem o2) {
			if (o1 == null || o2 == null) {
				return comparePossiblyNull(o1, o2);
			}
			if (o1.getPrice() == o2.getPrice()) {
				return 0;
			}
			return o1.getPrice() > o2.getPrice() ? 1 : -1;
		}
	};

	static Comparator<BrokerItem> PRICE_SORT_DESC = new Comparator<BrokerItem>() {
		/** 比较 / compare. */
		@Override
		public int compare(BrokerItem o1, BrokerItem o2) {
			if (o1 == null || o2 == null) {
				return comparePossiblyNull(o1, o2);
			}
			if (o1.getPrice() == o2.getPrice()) {
				return 0;
			}
			return o1.getPrice() > o2.getPrice() ? -1 : 1;
		}
	};

	static Comparator<BrokerItem> PIECE_PRICE_SORT_ASC = new Comparator<BrokerItem>() {
		/** 比较 / compare. */
		@Override
		public int compare(BrokerItem o1, BrokerItem o2) {
			if (o1 == null || o2 == null) {
				return comparePossiblyNull(o1, o2);
			}
			if (o1.getPiecePrice() == o2.getPiecePrice()) {
				return 0;
			}
			return o1.getPiecePrice() > o2.getPiecePrice() ? 1 : -1;
		}
	};

	static Comparator<BrokerItem> PIECE_PRICE_SORT_DESC = new Comparator<BrokerItem>() {
		/** 比较 / compare. */
		@Override
		public int compare(BrokerItem o1, BrokerItem o2) {
			if (o1 == null || o2 == null) {
				return comparePossiblyNull(o1, o2);
			}
			if (o1.getPiecePrice() == o2.getPiecePrice()) {
				return 0;
			}
			return o1.getPiecePrice() > o2.getPiecePrice() ? -1 : 1;
		}
	};

	static Comparator<BrokerItem> LEVEL_SORT_ASC = new Comparator<BrokerItem>() {
		/** 比较 / compare. */
		@Override
		public int compare(BrokerItem o1, BrokerItem o2) {
			if (o1 == null || o2 == null) {
				return comparePossiblyNull(o1, o2);
			}
			if (o1.getItemLevel() == o2.getItemLevel()) {
				return 0;
			}
			return o1.getItemLevel() > o2.getItemLevel() ? 1 : -1;
		}
	};

	static Comparator<BrokerItem> LEVEL_SORT_DESC = new Comparator<BrokerItem>() {
		/** 比较 / compare. */
		@Override
		public int compare(BrokerItem o1, BrokerItem o2) {
			if (o1 == null || o2 == null) {
				return comparePossiblyNull(o1, o2);
			}
			if (o1.getItemLevel() == o2.getItemLevel()) {
				return 0;
			}
			return o1.getItemLevel() > o2.getItemLevel() ? -1 : 1;
		}
	};

	private static <T extends Comparable<T>> int comparePossiblyNull(T aThis, T aThat) {
		int result = 0;
		if (aThis == null && aThat != null) {
			result = -1;
		} else if (aThis != null && aThat == null) {
			result = 1;
		}
		return result;
	}

	/** 按 type 返回 comparatory / Returns the comparatory by type */
	public static Comparator<BrokerItem> getComparatoryByType(int sortType) {
		switch (sortType) {
		case 0:
			return NAME_SORT_ASC;
		case 1:
			return NAME_SORT_DESC;
		case 2:
			return LEVEL_SORT_ASC;
		case 3:
			return LEVEL_SORT_DESC;
		case 4:
			return PRICE_SORT_ASC;
		case 5:
			return PRICE_SORT_DESC;
		case 6:
			return PIECE_PRICE_SORT_ASC;
		case 7:
			return PIECE_PRICE_SORT_DESC;
		default:
			throw new IllegalArgumentException("Illegal sort type for broker items");
		}
	}
}
