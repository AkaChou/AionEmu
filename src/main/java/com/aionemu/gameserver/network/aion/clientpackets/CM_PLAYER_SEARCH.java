package com.aionemu.gameserver.network.aion.clientpackets;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.aionemu.gameserver.configs.main.CustomConfig;
import com.aionemu.gameserver.model.gameobjects.player.FriendList.Status;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.AionClientPacket;
import com.aionemu.gameserver.network.aion.AionConnection.State;
import com.aionemu.gameserver.network.aion.serverpackets.SM_PLAYER_SEARCH;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.Util;
import com.aionemu.gameserver.world.World;

/**
 * 客户端玩家搜索请求包，按名称/区域/职业/等级/LFG 等条件筛选在线玩家。
 * Client packet to search online players by name, region, class, level, LFG, etc.
 */
public class CM_PLAYER_SEARCH extends AionClientPacket {
	public static final int MAX_RESULTS = 104;
	private String name;
	private int region;
	private int classMask;
	private int minLevel;
	private int maxLevel;
	private int lfgOnly;

	/**
	 * packet opcode
	 * @param state 连接状态 / connection state
	 * @param restStates 其余允许状态 / additional allowed states
	 */
	public CM_PLAYER_SEARCH(int opcode, State state, State... restStates) {
		super(opcode, state, restStates);
	}

	@Override
	protected void readImpl() {
		name = readS(52);
		if (name != null) {
			name = Util.convertName(name);
		}
		region = readD();
		classMask = readD();
		minLevel = readC();
		maxLevel = readC();
		lfgOnly = readC();
		readC();
	}

	@Override
	protected void runImpl() {
		Player activePlayer = getConnection().getActivePlayer();
		Iterator<Player> it = com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().getPlayersIterator();
		List<Player> matches = new ArrayList<Player>(MAX_RESULTS);
		if (activePlayer.getLevel() < 10) {
			// 10 级以下角色无法使用搜索功能。 / Characters under level 10 cannot use the search function.
			PacketSendUtility.sendPacket(activePlayer, SM_SYSTEM_MESSAGE.STR_CANT_WHO_LEVEL("10"));
			return;
		}
		while (it.hasNext() && matches.size() < MAX_RESULTS) {
			Player player = it.next();
			if (!player.isSpawned()) {
				continue;
			} else if (player.getFriendList().getStatus() == Status.OFFLINE) {
				continue;
			} else if (player.isGM() && !CustomConfig.SEARCH_GM_LIST) {
				continue;
			} else if (lfgOnly == 1 && !player.isLookingForGroup()) {
				continue;
			} else if (!name.isEmpty() && !player.getName().toLowerCase().contains(name.toLowerCase())) {
				continue;
			} else if (minLevel != 0xFF && player.getLevel() < minLevel) {
				continue;
			} else if (maxLevel != 0xFF && player.getLevel() > maxLevel) {
				continue;
			} else if (classMask > 0 && (player.getPlayerClass().getMask() & classMask) == 0) {
				continue;
			} else if (region > 0 && player.getActiveRegion().getMapId() != region) {
				continue;
			} else if ((player.getRace() != activePlayer.getRace()) && (CustomConfig.FACTIONS_SEARCH_MODE == false)) {
				continue;
			} else if (player.getName() == activePlayer.getName()) {
				continue;
			} else {
				matches.add(player);
			}
		}
		sendPacket(new SM_PLAYER_SEARCH(matches, region));
	}
}
