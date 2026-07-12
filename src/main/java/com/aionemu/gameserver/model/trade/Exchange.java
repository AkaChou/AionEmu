package com.aionemu.gameserver.model.trade;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.aionemu.gameserver.model.gameobjects.Item;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import lombok.Getter;

/**
 * 交换，用于交易相关逻辑。
 * Exchange for trade logic.
 *
 * @author ATracer
 */
@Getter
public class Exchange {

	private Player activeplayer;
	private Player targetPlayer;

	private boolean confirmed;
	private boolean locked;

	private long kinahCount;

	private Map<Integer, ExchangeItem> items = new HashMap<Integer, ExchangeItem>();
	private List<Item> itemsToUpdate = new ArrayList<Item>();

	public Exchange(Player activeplayer, Player targetPlayer) {
		super();
		this.activeplayer = activeplayer;
		this.targetPlayer = targetPlayer;
	}

	/** 确认 / confirm. */
	public void confirm() {
		confirmed = true;
	}

	/** 锁定。 / Lock. */
	public void lock() {
		this.locked = true;
	}

	/**
	 * @param parentItemObjId
	 */
	public void addItem(int parentItemObjId, ExchangeItem exchangeItem) {
		this.items.put(parentItemObjId, exchangeItem);
	}

	/**
	 * @param countToAdd
	 */
	public void addKinah(long countToAdd) {
		this.kinahCount += countToAdd;
	}

	/** 交易列表是否已满 / Whether exchange list full */
	public boolean isExchangeListFull() {
		return items.size() > 18;
	}

	/**
	 * @param item
	 */
	public void addItemToUpdate(Item item) {
		itemsToUpdate.add(item);
	}
}
