package com.aionemu.gameserver.network.aion.serverpackets;

import lombok.extern.slf4j.Slf4j;
import com.aionemu.gameserver.lifecycle.GameRuntimeServices;

import com.aionemu.commons.database.dao.DAOManager;
import com.aionemu.gameserver.dao.MailDAO;
import com.aionemu.gameserver.model.account.Account;
import com.aionemu.gameserver.model.account.PlayerAccountData;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.gameobjects.player.PlayerCommonData;
import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.PlayerInfo;
import com.aionemu.gameserver.services.BrokerService;
import com.aionemu.gameserver.services.player.PlayerService;

/**
 * 向客户端发送账号角色列表的服务端包。
 * Server packet that sends the account character list to the client.
 *
 * @author Nemesiss, AEJTester
 */
@Slf4j
public class SM_CHARACTER_LIST extends PlayerInfo {


	/**
	 * PlayOk2 会话令牌（客户端会话相关）。
	 * PlayOk2 session token (client session related).
	 */
	private final int playOk2;
	private final int unkValue;

	/**
	 * 构造角色列表包。
	 * Constructs a character-list packet.
	 *
	 * @param unkValue 协议阶段/未知值（0 或 2 等） / protocol stage / unknown value (0 or 2, etc.)
	 * session token
	 */
	public SM_CHARACTER_LIST(int unkValue, int playOk2) {
		this.playOk2 = playOk2;
		this.unkValue = unkValue;
	}

	@Override
	protected void writeImpl(AionConnection con) {
		writeC(unkValue);// 5.0 unk protocol
		if (unkValue == 0) {
			writeD(playOk2);
			writeC(0);
		} else if (unkValue == 2) {
			writeD(playOk2);
			Account account = con.getAccount();
			writeC(account.size()); // characters count
			for (PlayerAccountData playerData : account.getSortedAccountsList()) {
				PlayerCommonData pcd = playerData.getPlayerCommonData();
				Player player = PlayerService.getPlayer(pcd.getPlayerObjId(), account);
				writePlayerInfo(playerData);
				writeD(player.getPlayerSettings().getDisplay());// display helmet 0 show, 5 dont show
				writeD(0);
				writeD(0);
				writeD(DAOManager.getDAO(MailDAO.class).haveUnread(pcd.getPlayerObjId()) ? 1 : 0); // mail
				writeD(0); // 未知 / unk
				writeD(0); // 未知 / unk
				writeQ(GameRuntimeServices.brokerService().getCollectedMoney(pcd)); // collected money from broker
				writeD(0);
				writeB(new byte[122 + 24]); // 5.1 protocol
			}
		}
	}
}
