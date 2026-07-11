package com.aionemu.loginserver.service.ptransfer;

/**
 * 玩家跨服转移结果状态码，用于登录服与游戏服之间的协议响应。
 * Player cross-server transfer result status codes used in login-server ↔ game-server protocol responses.
 *
 * @author KID
 */
public enum PlayerTransferResultStatus {

    /** 向目标服发送角色信息 / send character info to the target server */
    SEND_INFO(20),
    /** 转移成功 / transfer succeeded */
    OK(21),
    /** 转移失败 / transfer failed */
    ERROR(22),
    /** 通知源服执行转移动作 / instruct the source server to perform the transfer action */
    PERFORM_ACTION(23);
    private int id;

    /**
     * 返回协议状态码数值。
     * Return the protocol status code value.
     *
     * status id
     */
    public int getId() {
        return id;
    }

    /**
     * 以协议数值构造枚举常量。
     * Construct the enum constant with its protocol numeric id.
     *
     * @param id 协议状态码 / protocol status id
     */
    PlayerTransferResultStatus(int id) {
        this.id = id;
    }
}
