package com.aionemu.gameserver.model.gameobjects;

import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Comparator;

import org.apache.commons.lang3.StringUtils;

import com.aionemu.gameserver.configs.main.BrokerConfig;
import com.aionemu.gameserver.model.broker.BrokerRace;
import lombok.Getter;
import lombok.Setter;

/**
 * 经纪行物品游戏对象。
 * Broker Item game object.
 */

@Getter
@Setter
public class BrokerItem implements Comparable<BrokerItem> {
	/** 获取物品。 / Returns the item. */
	private final Item item;
	/** 返回物品 ID / Returns the item id */
	private final int itemId;
	/** 返回物品唯一 ID / Returns the item unique id */
	private int itemUniqueId;
	/** 获取物品计数。 / Returns the item count. */
	private long itemCount;
	/** 设置物品制作者 / Sets the item creator */
	private String itemCreator;
	/** 获取价格。 / Returns the price. */
	private long price;
	/** 返回卖家 / Returns the seller */
	private final String seller;
	/** 返回卖家 ID / Returns the seller id */
	private final int sellerId;
	/** 获取物品经纪行种族。 / Returns the item broker race. */
	private final BrokerRace itemBrokerRace;
	/**
	 * -- GETTER --
	 *  是否已售出。
	 *  Whether the item is sold.
	 */
	private boolean isSold;
	/**
	 * -- GETTER --
	 *  是否已取消。
	 *  Whether the listing was canceled.
	 */
	private boolean isCanceled;
	/**
	 * 是否已结算。
	 * Whether the sale is settled.
	 */
	private boolean isSettled;
	/** 获取过期时间。 / Returns the expire time. */
	private final Timestamp expireTime;
	/** 返回结算时间 / Returns the settle time */
	private Timestamp settleTime;
	/**
	 * 是否拆分出售。
	 * Whether the item is sold in split quantities.
	 * 是否拆分出售 / whether split sell
	 */
	private final boolean isSplitSell;
	PersistentState state;
	private final int ExpireTimeinMillis = BrokerConfig.ITEMS_EXPIRE_TIME * 24 * 3600 * 1000;

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

	/** 返回物品制作者 / Returns the item creator */
	public String getItemCreator() {
		if (itemCreator == null) {
			return StringUtils.EMPTY;
		}
		return itemCreator;
	}

	/** 设置是否已取消 / Sets whether canceled */
	public void setIsCanceled(boolean isCanceled) {
		this.isCanceled = isCanceled;
	}

	/** 移除物品。 / Removes item. */
	public void removeItem() {
		this.isSold = true;
		this.isSettled = true;
		this.settleTime = new Timestamp(Calendar.getInstance().getTimeInMillis());
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
			if (this.state != PersistentState.NEW) {
				this.state = PersistentState.UPDATE_REQUIRED;
			}
			break;
		default:
			this.state = persistentState;
		}
	}

	/**
	 * 恢复交易状态（用于数据修复场景）。
	 * Restores the transaction state (used for data repair).
	 * @param itemCount 物品数量 / item count
	 * @param price 价格 / price
	 * @param sold 是否已售出 / whether sold
	 * @param settled 是否已结算 / whether settled
	 * @param settleTime 结算时间 / settle time
	 * @param persistentState 持久化状态 / persistent state
	 */
	public void restoreTransactionState(long itemCount, long price, boolean sold, boolean settled,
			Timestamp settleTime, PersistentState persistentState) {
		this.itemCount = itemCount;
		this.price = price;
		this.isSold = sold;
		this.isSettled = settled;
		this.settleTime = settleTime;
		this.state = persistentState;
	}

	/** 获取持久化状态。 / Returns the persistent state. */
	public PersistentState getPersistentState() {
		return state;
	}

	/** 标记为已结算 / Marks the sale as settled */
	public void setSettled() {
		this.isSettled = true;
		this.settleTime = new Timestamp(Calendar.getInstance().getTimeInMillis());
	}

	private int getItemLevel() {
		return item.getItemTemplate().getLevel();
	}

	/** 返回单价 / Returns the piece price */
	public long getPiecePrice() {
		return getPrice() / getItemCount();
	}

	private String getItemName() {
		return item.getItemName();
	}

	/** 比较。 / Compares to another instance. */
	@Override
	public int compareTo(BrokerItem o) {
		return itemUniqueId > o.getItemUniqueId() ? 1 : -1;
	}

	static Comparator<BrokerItem> NAME_SORT_ASC = new Comparator<>() {
		/**
		 * 比较 / compare.
		 */
		@Override
		public int compare(BrokerItem o1, BrokerItem o2) {
			if (o1 == null || o2 == null) {
				return comparePossiblyNull(o1, o2);
			}
			return o1.getItemName().compareTo(o2.getItemName());
		}
	};

	static Comparator<BrokerItem> NAME_SORT_DESC = new Comparator<>() {
		/**
		 * 比较 / compare.
		 */
		@Override
		public int compare(BrokerItem o1, BrokerItem o2) {
			if (o1 == null || o2 == null) {
				return comparePossiblyNull(o1, o2);
			}
			return o1.getItemName().compareTo(o2.getItemName());
		}
	};

	static Comparator<BrokerItem> PRICE_SORT_ASC = new Comparator<>() {
		/**
		 * 比较 / compare.
		 */
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

	static Comparator<BrokerItem> PRICE_SORT_DESC = new Comparator<>() {
		/**
		 * 比较 / compare.
		 */
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

	static Comparator<BrokerItem> PIECE_PRICE_SORT_ASC = new Comparator<>() {
		/**
		 * 比较 / compare.
		 */
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

	static Comparator<BrokerItem> PIECE_PRICE_SORT_DESC = new Comparator<>() {
		/**
		 * 比较 / compare.
		 */
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

	static Comparator<BrokerItem> LEVEL_SORT_ASC = new Comparator<>() {
		/**
		 * 比较 / compare.
		 */
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

	static Comparator<BrokerItem> LEVEL_SORT_DESC = new Comparator<>() {
        /**
         * 比较 / compare.
         */
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

	/** 按类型返回比较器 / Returns the comparator by sort type */
	public static Comparator<BrokerItem> getComparatoryByType(int sortType) {
		return switch (sortType) {
			case 0 -> NAME_SORT_ASC;
			case 1 -> NAME_SORT_DESC;
			case 2 -> LEVEL_SORT_ASC;
			case 3 -> LEVEL_SORT_DESC;
			case 4 -> PRICE_SORT_ASC;
			case 5 -> PRICE_SORT_DESC;
			case 6 -> PIECE_PRICE_SORT_ASC;
			case 7 -> PIECE_PRICE_SORT_DESC;
			default -> throw new IllegalArgumentException("Illegal sort type for broker items");
		};
	}
}
