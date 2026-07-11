package com.aionemu.gameserver.network.aion.serverpackets;

import com.aionemu.gameserver.configs.main.SecurityConfig;
import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;

/**
 * 角色选择阶段二级密码（Passkey）相关 UI 与结果消息的服务端包。
 * Server packet for character-select passkey UI windows and result messages.
 *
 * @author cura
 */
public class SM_CHARACTER_SELECT extends AionServerPacket {

	private int type; // 0: new passkey input window, 1: passkey input window, 2: message window
	private int messageType; // 0: newpasskey complete, 2: passkey edit complete, 3: passkey input
	private int wrongCount;
	private int unk;

	/**
	 * 打开二级密码相关窗口（新建/输入）。
	 * input). / input).
	 *
	 * @param type 窗口类型：0 新建、1 输入、2 消息 / window type: 0 create, 1 input, 2 message
	 */
	public SM_CHARACTER_SELECT(int type) {
		this.type = type;
	}

	/**
	 * 返回二级密码操作结果消息。
	 * Returns a passkey operation result message.
	 *
	 * @param type 窗口类型（通常为 2 消息窗） / window type (typically 2 message)
	 * message type
	 * @param unk 未知字段 / unknown field
	 * @param wrongCount 错误输入次数 / wrong input count
	 */
	public SM_CHARACTER_SELECT(int type, int messageType, int unk, int wrongCount) {
		this.type = type;
		this.messageType = messageType;
		this.unk = unk;
		this.wrongCount = wrongCount;
	}

	@Override
	protected void writeImpl(AionConnection con) {
		writeC(type);

		switch (type) {
		case 0:
			break;
		case 1:
			break;
		case 2:
			writeH(messageType); // 0: newpasskey complete, 2: passkey edit complete, 3: passkey input
			writeC(unk);
			writeC(wrongCount > 0 ? 1 : 0); // 0: right passkey, 1: wrong passkey
			writeD(wrongCount); // wrong passkey input count
			writeD(SecurityConfig.PASSKEY_WRONG_MAXCOUNT);
			// 服务器默认值：5） / server default value: 5)
			break;
		}
	}
}
