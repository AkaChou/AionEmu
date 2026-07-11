package com.aionemu.gameserver.network.aion.clientpackets;

import com.aionemu.boot.i18n.I18n;
import com.aionemu.gameserver.lifecycle.GameRuntimeServices;

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

	private int unk1;
	private int unk2;
	private int unk3;
	private int minLvl;
	private int maxLvl;
	private int minUnk;
	private int maxUnk;
	private int unk4;

	public CM_BROKER_SEARCH(int opcode, State state, State... restStates) {
		super(opcode, state, restStates);
	}

	@Override
	protected void readImpl() {
		this.brokerId = readD();
		this.sortType = readC();
		this.page = readH();
		unk1 = readC();
		unk2 = readH();
		this.mask = readH();
		unk3 = readD();
		minLvl = readH();
		maxLvl = readH();
		minUnk = readC();
		maxUnk = readC();
		unk4 = readH();

		this.itemCount = readH();
		this.itemList = new ArrayList<Integer>();
		for (int index = 0; index < this.itemCount; index++) {
			this.itemList.add(readD());
		}
	}

	@Override
	protected void runImpl() {
		Player player = getConnection().getActivePlayer();
		GameRuntimeServices.brokerService().showRequestedItems(player, mask, sortType, page, itemList);
		// log.info(I18n.get("log.922e1d1969ac", brokerId, sortType, page, unk1, unk2, mask, unk3, minLvl, maxLvl, minUnk, maxUnk, unk4, itemCount));
	}
}
