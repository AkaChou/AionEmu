package com.aionemu.gameserver.network.aion.serverpackets;

import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;

/**
 * 向客户端同步术古扫荡（Shugo Sweep）小游戏进度。
 * Server packet synchronizing Shugo Sweep mini-game progress to the client.
 *
 * @author Ghostfur
 */
public class SM_SHUGO_SWEEP extends AionServerPacket {

	private int tableId;
	private int currentStep;
	private int diceLeft;
	private int diceGolden;
	private int unkButton;
	private int moveStep;

	@SuppressWarnings("unused")
	private int unk;

	// 清理玩家信息 / sweep player infos
	/**
	 * 使用给定参数构造 SM_SHUGO_SWEEP 包。
	 * Creates a SM_SHUGO_SWEEP packet with the given parameters.
	 *
	 * table id
	 * current step
	 * dice left
	 * golden dice
	 * unknown button
	 * move step
	 */
	public SM_SHUGO_SWEEP(int tableId, int currentStep, int diceLeft, int diceGolden, int unkButton, int moveStep) {
		this.currentStep = currentStep;
		this.diceLeft = diceLeft;
		this.diceGolden = diceGolden;
		this.unkButton = unkButton;
		this.moveStep = moveStep;
		this.tableId = tableId;
	}

	@Override
	protected void writeImpl(AionConnection con) {
		writeD(tableId); // table id
		writeD(currentStep); // current step
		writeH(0); // reward ??
		writeH(0); // reward ??
		writeD(0);
		writeD(diceLeft); // dice left
		writeD(diceGolden); // dice golden
		writeD(unkButton); // button near dice left
		writeD(432000);
		writeD(0);
		writeD(432000);
		writeD(0);
		writeD(moveStep); // move step
	}
}
