package com.aionemu.loginserver.service.ptransfer;

/**
 * 玩家跨服转移流程步骤状态。
 * Player cross-server transfer flow step statuses.
 *
 * @author KID
 */
public enum PlayerTransferStatus {

    /** 第一步：源服侧准备 / step 1: source-server preparation */
    STEP1,
    /** 第二步：目标服侧处理 / step 2: target-server processing */
    STEP2,
    /** 第一步完成 / step 1 completed */
    STEP1_OK,
    /** 第二步完成 / step 2 completed */
    STEP2_OK
}
