package com.aionemu.loginserver.service.ptransfer;

import com.aionemu.loginserver.model.Account;

/**
 * 玩家跨服转移请求上下文，承载源/目标服、账号与角色二进制数据。
 * Player cross-server transfer request context holding source/target servers, accounts, and character binary data.
 *
 * @author KID
 */
public class PlayerTransferRequest {

    /** 当前转移步骤状态 / current transfer step status */
    public PlayerTransferStatus status;
    /** 源游戏服 ID / source game-server id */
    public byte serverId;
    /** 目标游戏服 ID / target game-server id */
    public byte targetServerId;
    /** 目标账号实体 / target account entity */
    public Account targetAccount;
    /** 角色序列化数据 / serialized character payload */
    public byte[] db;
    /** 角色名 / character name */
    public String name;
    /** 目标账号 ID / target account id */
    public int targetAccountId;
    /** 角色 ID / player id */
    public int playerId;
    /** accountused 在 targetsideoften 相同 astargetAccount / account used on target side (often same as targetAccount) */
    public Account account;
    /** 源账号实体 / source account entity */
    public Account saccount;
    /** 关联的转移任务 ID / related transfer task id */
    public int taskId;

    /**
     * 以给定步骤状态创建转移请求。
     * Create a transfer request with the given step status.
     *
     * initial step
     */
    public PlayerTransferRequest(PlayerTransferStatus status) {
        this.status = status;
    }
}
