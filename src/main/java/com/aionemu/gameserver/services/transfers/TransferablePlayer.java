package com.aionemu.gameserver.services.transfers;

import com.aionemu.gameserver.model.gameobjects.player.Player;

/**
 * 可转移玩家任务载体，保存跨服角色迁移过程中的源/目标账户与任务信息。
 * Transferable player task holder storing source/target account and task info during cross-server transfer.
 *
 * @author KID
 */
public class TransferablePlayer {
	/** 玩家 ID / Player ID */
	public int playerId;
	/** 源账号 ID / Source account ID */
	public int accountId;
	/** 目标账号 ID / Target account ID */
	public int targetAccountId;
	/** 玩家对象 / Player object */
	public Player player;
	/** 目标服务器 ID / Target server id */
	public byte targetServerId;
	/** 任务 ID / Task id */
	public int taskId;

	/**
	 * 构造可转移玩家记录。
	 * Construct a transferable player record.
	 *
	 * @param playerId 玩家 ID / player id
	 * @param accountId 源账号 ID / source account id
	 * @param targetAccountId 目标账号 ID / target account id
	 */
	public TransferablePlayer(int playerId, int accountId, int targetAccountId) {
		this.playerId = playerId;
		this.accountId = accountId;
		this.targetAccountId = targetAccountId;
	}
}
