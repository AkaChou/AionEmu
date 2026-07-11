package com.aionemu.gameserver.network.aion.serverpackets;

import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;

/**
 * 向客户端更新经验值、可恢复经验与加成经验。
 * Server packet updating experience, recoverable exp, and boost exp on the client.
 */
public class SM_STATUPDATE_EXP extends AionServerPacket {
	private long currentExp;
	private long recoverableExp;
	private long maxExp;
	private long curBoostExp = 0;
	private long maxBoostExp = 0;
	private long goldenStar;
	private long auraGrowth;

	/**
	 * 使用给定参数构造 SM_STATUPDATE_EXP 包。
	 * Creates a SM_STATUPDATE_EXP packet with the given parameters.
	 *
	 * current exp
	 * @param recoverableExp 可恢复经验 / recoverable exp
	 * max exp
	 * @param curBoostExp 当前加成经验 / current boost exp
	 * @param maxBoostExp 最大加成经验 / max boost exp
	 */
	public SM_STATUPDATE_EXP(long currentExp, long recoverableExp, long maxExp, long curBoostExp, long maxBoostExp) {
		this.currentExp = currentExp;
		this.recoverableExp = recoverableExp;
		this.maxExp = maxExp;
		this.curBoostExp = curBoostExp;
		this.maxBoostExp = maxBoostExp;
	}

	/**
	 * 使用给定参数构造 SM_STATUPDATE_EXP 包。
	 * Creates a SM_STATUPDATE_EXP packet with the given parameters.
	 *
	 * current exp
	 * @param recoverableExp 可恢复经验 / recoverable exp
	 * max exp
	 * @param curBoostExp 当前加成经验 / current boost exp
	 * @param maxBoostExp 最大加成经验 / max boost exp
	 * goldenStar
	 * @param auraGrowth 光环成长值 / auraGrowth
	 */
	public SM_STATUPDATE_EXP(long currentExp, long recoverableExp, long maxExp, long curBoostExp, long maxBoostExp,
			long goldenStar, long auraGrowth) {
		this.currentExp = currentExp;
		this.recoverableExp = recoverableExp;
		this.maxExp = maxExp;
		this.curBoostExp = curBoostExp;
		this.maxBoostExp = maxBoostExp;
		this.goldenStar = goldenStar;
		this.auraGrowth = auraGrowth;
	}

	@Override
	protected void writeImpl(AionConnection con) {
		writeQ(currentExp);
		writeQ(recoverableExp);
		writeQ(maxExp);
		writeQ(curBoostExp);
		writeQ(maxBoostExp);
		writeQ(goldenStar);
		writeQ(auraGrowth);
	}
}
