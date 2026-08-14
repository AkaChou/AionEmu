package com.aionemu.loginserver.network.gameserver.serverpackets;

import com.aionemu.loginserver.model.Account;
import com.aionemu.loginserver.network.gameserver.GsConnection;
import com.aionemu.loginserver.network.gameserver.GsServerPacket;
import com.aionemu.loginserver.service.ptransfer.PlayerTransferRequest;
import com.aionemu.loginserver.service.ptransfer.PlayerTransferResultStatus;
import com.aionemu.loginserver.service.ptransfer.PlayerTransferTask;

/**
 * LS→GS：角色跨服转移结果/指令响应（按状态分发不同载荷）。
 * LS→GS: player transfer result/command response (payload varies by result status).
 *
 * @author KID
 */
public class SM_PTRANSFER_RESPONSE extends GsServerPacket {

    /**
     * 转移结果状态。
     * Transfer result status.
     */
    private PlayerTransferResultStatus result;
    /**
     * 目标账号（SEND_INFO 时使用）。
     * Target account (used for SEND_INFO).
     */
    private Account account;
    /**
     * 转移请求（SEND_INFO 时使用）。
     * Transfer request (used for SEND_INFO).
     */
    private PlayerTransferRequest request;
    /**
     * 任务 ID。
     * Task id.
     */
    private int taskId;
    /**
     * 错误原因（ERROR 时使用）。
     * Error reason (used for ERROR).
     */
    private String reason;
    /**
     * 转移任务（PERFORM_ACTION 时使用）。
     * Transfer task (used for PERFORM_ACTION).
     */
    private PlayerTransferTask task;

    /**
     * 构造成功类响应（仅任务 ID）。
     * Constructs an OK-style response (task id only).
     *
     * @param result 结果状态 / result status
     * @param taskId 任务 ID / task id
     */
    public SM_PTRANSFER_RESPONSE(PlayerTransferResultStatus result, int taskId) {
        this.result = result;
        this.taskId = taskId;
    }

    /**
     * 构造发送角色信息响应。
     * Constructs a SEND_INFO response with transfer request payload.
     *
     * @param result 结果状态 / result status
     * @param request 转移请求 / transfer request
     */
    public SM_PTRANSFER_RESPONSE(PlayerTransferResultStatus result, PlayerTransferRequest request) {
        this.result = result;
        this.request = request;
        this.account = request.targetAccount;
        this.taskId = request.taskId;
    }

    /**
     * 构造错误响应（含原因）。
     * Constructs an ERROR response with reason text.
     *
     * @param result 结果状态 / result status
     * @param taskId 任务 ID / task id
     * @param reason 错误原因 / error reason
     */
    public SM_PTRANSFER_RESPONSE(PlayerTransferResultStatus result, int taskId, String reason) {
        this.result = result;
        this.taskId = taskId;
        this.reason = reason;
    }

    /**
     * 构造执行转移动作响应。
     * Constructs a PERFORM_ACTION response with transfer task payload.
     *
     * @param result 结果状态 / result status
     * @param task 转移任务 / transfer task
     */
    public SM_PTRANSFER_RESPONSE(PlayerTransferResultStatus result, PlayerTransferTask task) {
        this.result = result;
        this.task = task;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeImpl(GsConnection con) {
        writeC(12);
        writeD(result.getId());
        switch (result) {
            case SEND_INFO:
                writeD(request.targetAccountId);
                writeD(taskId);
                writeS(request.name);
                writeS(account.getName());
                writeD(request.db.length);
                writeB(request.db);
                break;
            case OK:
                writeD(taskId);
                break;
            case ERROR:
                writeD(taskId);
                writeS(reason);
                break;
            case PERFORM_ACTION:
                writeC(task.sourceServerId);
                writeC(task.targetServerId);
                writeD(task.sourceAccountId);
                writeD(task.targetAccountId);
                writeD(task.playerId);
                writeD(task.id);
                break;
        }
    }
}
