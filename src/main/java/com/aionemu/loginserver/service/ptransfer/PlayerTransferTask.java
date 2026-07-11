package com.aionemu.loginserver.service.ptransfer;

/**
 * 玩家跨服转移任务数据模型（对应数据库任务行）。
 * Player cross-server transfer task data model (maps to a DB task row).
 *
 * @author KID
 */
public class PlayerTransferTask {

    /** 源账号 ID / source account id */
    public int sourceAccountId;
    /** 目标账号 ID / target account id */
    public int targetAccountId;
    /** 角色 ID / player id */
    public int playerId;
    /** 源游戏服 ID / source game-server id */
    public byte sourceServerId;
    /** 目标游戏服 ID / target game-server id */
    public byte targetServerId;
    /** 任务主键 ID / task primary key */
    public int id;
    /** 任务状态（见 STATUS_* 常量） / task status (see STATUS_* constants) */
    public byte status;
    /** 备注/错误说明 / comment or error description */
    public String comment;
    /** 等待执行 / waiting to run */
    public final static byte STATUS_WAIT = 0;
    /** 执行中 / active */
    public final static byte STATUS_ACTIVE = 1;
    /** 已完成 / done */
    public final static byte STATUS_DONE = 2;
    /** 失败 / error */
    public final static byte STATUS_ERROR = 3;
}
