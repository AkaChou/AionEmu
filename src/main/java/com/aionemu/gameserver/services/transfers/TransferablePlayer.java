package com.aionemu.gameserver.services.transfers;

import com.aionemu.gameserver.model.gameobjects.player.Player;

/**
 * 可转移玩家任务载体，保存跨服角色迁移过程中的源/目标账户与任务信息。
 * Transferable player task holder storing source/target account and task info during cross-server transfer.
 *
 * @author KID
 */
public class TransferablePlayer {
	public int playerId;
	public int accountId;
	public int targetAccountId;
	public Player player;
	public byte targetServerId;
	public int taskId;

	/**
	 * 构造可转移玩家记录。
	 * Construct a transferable player record.
	 *
	 * Player ID
	 * Source account ID
	 * Target account ID
	 */
	public TransferablePlayer(int playerId, int accountId, int targetAccountId) {
		this.playerId = playerId;
		this.accountId = accountId;
		this.targetAccountId = targetAccountId;
	}
}
