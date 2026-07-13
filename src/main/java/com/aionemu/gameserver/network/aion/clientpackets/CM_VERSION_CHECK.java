package com.aionemu.gameserver.network.aion.clientpackets;

import com.aionemu.gameserver.network.aion.AionClientPacket;
import com.aionemu.gameserver.network.aion.AionConnection.State;
import com.aionemu.gameserver.network.aion.serverpackets.SM_0x125;
import com.aionemu.gameserver.network.aion.serverpackets.SM_0x126;
import com.aionemu.gameserver.network.aion.serverpackets.SM_BLACKCLOUD_TRADE;
import com.aionemu.gameserver.network.aion.serverpackets.SM_CHAR_BM_PACK_LIST;
import com.aionemu.gameserver.network.aion.serverpackets.SM_UNK_168;
import com.aionemu.gameserver.network.aion.serverpackets.SM_VERSION_CHECK;

/**
 * 客户端版本校验握手请求包，回复版本与若干初始化服务端包。
 * Client packet for version-check handshake; replies with version and init server packets.
 *
 * @author -Nemesiss-
 */
public class CM_VERSION_CHECK extends AionClientPacket {
	private int version;
	@SuppressWarnings("unused")
	private int subversion;
	@SuppressWarnings("unused")
	private int windowsEncoding;
	@SuppressWarnings("unused")
	private int windowsVersion;
	@SuppressWarnings("unused")
	private int windowsSubVersion;
	private int unk;

	/**
	 * packet opcode
	 * @param state 连接状态 / connection state
	 * @param restStates 其余允许状态 / additional allowed states
	 */
	public CM_VERSION_CHECK(int opcode, State state, State... restStates) {
		super(opcode, state, restStates);
	}

	@Override
	protected void readImpl() {
		version = readH();
		subversion = readH();
		windowsEncoding = readD();
		windowsVersion = readD();
		windowsSubVersion = readD();
		unk = readC();
	}

	@Override
	protected void runImpl() {
		sendPacket(new SM_VERSION_CHECK(version));
		sendPacket(new SM_0x125());
		sendPacket(new SM_0x126(unk));
		sendPacket(new SM_UNK_168());
		sendPacket(new SM_BLACKCLOUD_TRADE());
		sendPacket(new SM_CHAR_BM_PACK_LIST(2));
	}
}
