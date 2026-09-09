package com.aionemu.gameserver.model.broker;

import java.util.ArrayList;
import java.util.List;

import com.aionemu.gameserver.model.gameobjects.BrokerItem;
import lombok.Getter;
import lombok.Setter;

/**
 * 经纪行玩家 Cache 模型。
 * Broker Player Cache model.
 *
 * @author ATracer
 */
public class BrokerPlayerCache {

	/**
	 * @return the brokerListCache
	 */
	@Getter
	@Setter
	private BrokerItem[] brokerListCache = new BrokerItem[0];
	/**
	 * @return the brokerMaskCache
	 */
	@Getter
	@Setter
	private int brokerMaskCache;
	private int brokerSoftTypeCache;
	/**
	 * @return the brokerStartPageCache
	 */
	@Getter
	@Setter
	private int brokerStartPageCache;
	private List<Integer> itemList = new ArrayList<Integer>();

	/**
	 * @return the brokerSoftTypeCache
	 */
	public int getBrokerSortTypeCache() {
		return brokerSoftTypeCache;
	}

	/**
	 * @param brokerSoftTypeCache the brokerSoftTypeCache to set
	 */
	public void setBrokerSortTypeCache(int brokerSoftTypeCache) {
		this.brokerSoftTypeCache = brokerSoftTypeCache;
	}

	/**
	 * @return the searched item list
	 */
	public List<Integer> getSearchItemList() {
		if (this.itemList == null)
			return null;
		return this.itemList;
	}

	/**
	 * @param itemList the searched item list to set
	 */
	public void setSearchItemsList(List<Integer> itemList) {
		this.itemList = itemList;
	}
}
