package com.aionemu.loginserver.network.gameserver.clientpackets;

import com.aionemu.loginserver.network.gameserver.GsClientPacket;
import com.aionemu.loginserver.service.LoginTransferServices;

/**
 * GS→LS：角色转移任务控制（请求/错误/成功/停止）。
 * GS→LS: player-transfer task control (request/error/ok/stop).
 *
 * @author KID
 */
public class CM_PTRANSFER_CONTROL extends GsClientPacket {

    /**
     * 动作 ID：1=请求转移，2=错误，3=成功，4=停止任务。
     * Action id: 1 = request transfer, 2 = error, 3 = ok, 4 = task stop.
     */
    private byte actionId;

    /**
     * 按 actionId 读取并立即分发到 PlayerTransferService。
     * Reads payload by actionId and dispatches immediately to PlayerTransferService.
     */
    @Override
    protected void readImpl() {
        actionId = this.readSC();
        switch (actionId) {
            case 1: // request transfer
            {
                int taskId = readD();
                String name = readS();
                int bytes = this.getRemainingBytes();
                byte[] db = this.readB(bytes);
                LoginTransferServices.playerTransferService().requestTransfer(taskId, name, db);
            }
            break;
            case 2: // ERROR
            {
                int taskId = readD();
                String reason = readS();
                LoginTransferServices.playerTransferService().onError(taskId, reason);
            }
            break;
            case 3: // ok
            {
                int taskId = readD();
                int playerId = readD();
                LoginTransferServices.playerTransferService().onOk(taskId, playerId);
            }
            break;
            case 4: // Task stop
            {
                int taskId = readD();
                String reason = readS();
                LoginTransferServices.playerTransferService().onTaskStop(taskId, reason);
            }
        }
    }

    /**
     * 无运行期逻辑（已在 readImpl 中处理）。
     * No runtime actions (handled in readImpl).
     */
    @Override
    protected void runImpl() {
        // 无需动作 / no actions required
    }
}
