package com.aionemu.gameserver.network.aion.serverpackets;

import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * 对 CM_QUIT 的应答，通知客户端退出或进入编辑模式结果。
 * Response to CM_QUIT notifying the client of quit or edit-mode outcome.
 * @author -Nemesiss-
 */
@NoArgsConstructor
@AllArgsConstructor
public class SM_QUIT_RESPONSE extends AionServerPacket {

	// Lombok @AllArgsConstructor 依赖本字段生成构造器，禁止降级为局部变量（否则与 @NoArgsConstructor 重复）。
	// Lombok @AllArgsConstructor needs this field for its generated constructor; never demote it to a local.
	private boolean edit_mode = false;


	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void writeImpl(AionConnection con) {
		writeD(edit_mode ? 2 : 1);// 1 normal, 2 plastic surgery/gender switch
		writeC(0x00);// 未知 / unk
		writeC(0xFF);// why sometime 0x2e?
		writeC(0xFF);
		writeC(0xFF);
		writeC(0xFF);
	}
}
