package com.aionemu.gameserver.network.aion.clientpackets;


import java.util.ArrayList;
import java.util.List;

import lombok.extern.slf4j.Slf4j;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.AionClientPacket;
import com.aionemu.gameserver.network.aion.AionConnection.State;
import com.aionemu.gameserver.services.BrokerService;

/**
 * 按条件搜索交易行物品的客户端包。
 * Client packet to search broker items by filters.
 */
@Slf4j
public class CM_BROKER_SEARCH extends AionClientPacket {

	@SuppressWarnings("unused")
	private int brokerId;
	private int sortType;
	private int page;
	private int mask;
	private int itemCount;
	private List<Integer> itemList;


	public CM_BROKER_SEARCH(int opcode, State state, State... restStates) {
		super(opcode, state, restStates);
	}

	@Override
	protected void readImpl() {
		this.brokerId = readD();
		this.sortType = readC();
		this.page = readH();
		readC();
		readH();
		this.mask = readH();
		readD();
		readH();
		readH();
		readC();
		readC();
		readH();

		this.itemCount = readH();
		this.itemList = new ArrayList<>();
		for (int index = 0; index < this.itemCount; index++) {
			this.itemList.add(readD());
		}
	}

	@Override
	protected void runImpl() {
		Player player = getConnection().getActivePlayer();
		BrokerService.getInstance().showRequestedItems(player, mask, sortType, page, itemList);
		// log.info(I18n.get("log.922e1d1969ac", brokerId, sortType, page, unk1, unk2, mask, unk3, minLvl, maxLvl, minUnk, maxUnk, unk4, itemCount));
	}
}
