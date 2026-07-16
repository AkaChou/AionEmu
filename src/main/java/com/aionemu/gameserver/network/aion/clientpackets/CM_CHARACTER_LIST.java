package com.aionemu.gameserver.network.aion.clientpackets;

import lombok.extern.slf4j.Slf4j;
import com.aionemu.gameserver.configs.administration.AdminConfig;
import com.aionemu.gameserver.model.account.Account;
import com.aionemu.gameserver.network.aion.AionClientPacket;
import com.aionemu.gameserver.network.aion.AionConnection.State;
import com.aionemu.gameserver.network.aion.serverpackets.SM_0x14F;
import com.aionemu.gameserver.network.aion.serverpackets.SM_ACCOUNT_PROPERTIES;
import com.aionemu.gameserver.network.aion.serverpackets.SM_CHARACTER_LIST;
import com.aionemu.gameserver.network.aion.serverpackets.SM_CHAR_BM_PACK_LIST;
import com.aionemu.gameserver.network.aion.serverpackets.SM_NP_AUTH_TOKEN;

/**
 * 请求账号角色列表的客户端包。
 * Client packet requesting the account character list.
 */
@Slf4j

public class CM_CHARACTER_LIST extends AionClientPacket {

	private int playOk2;

	public CM_CHARACTER_LIST(int opcode, State state, State... restStates) {
		super(opcode, state, restStates);
	}

	@Override
	protected void readImpl() {
		playOk2 = readD();
	}

	@Override
	protected void runImpl() {
		Account account = getConnection().getAccount();
		boolean isGM = account.getAccessLevel() >= AdminConfig.GM_PANEL;
		sendPacket(new SM_ACCOUNT_PROPERTIES(isGM));
		sendPacket(new SM_0x14F());
		// TODO: STS 尚未实现完成，暂不触发客户端 STS 流程。 / STS is not fully implemented; keep it disabled.
		// sendPacket(new SM_NP_AUTH_TOKEN());
		// Char-select VIP stage: encode score in BM duration for client Game.dll patch
		// (STS GetLevel is often skipped on private-server launches).
		sendPacket(SM_CHAR_BM_PACK_LIST.vipForCharSelect(account.getVipLevel(), account.getVipExp()));
		sendPacket(new SM_CHARACTER_LIST(0, playOk2));
		sendPacket(new SM_CHARACTER_LIST(2, playOk2));
	}
}
