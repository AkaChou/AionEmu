package com.aionemu.loginserver.taskmanager.handler.implementations;


import com.aionemu.boot.i18n.I18n;
import lombok.extern.slf4j.Slf4j;
import com.aionemu.commons.utils.AionEmbeddedShutdownHandler;
import com.aionemu.commons.utils.AionEmbeddedShutdownMode;
import com.aionemu.commons.utils.AionRuntimeMode;
import com.aionemu.loginserver.service.LoginShutdownRequest;
import com.aionemu.loginserver.taskmanager.handler.TaskFromDBHandler;

/**
 * 重启登录服的数据库任务处理器。
 * DB task handler that restarts the login server.
 *
 * @author Divinity, nrg
 */
@Slf4j
public class RestartHandler extends TaskFromDBHandler {


    /**
     * 触发登录服重启（嵌入模式走 embedded 关停，否则标准重启）。
     * Triggers login-server restart (embedded mode uses embedded shutdown, otherwise standard restart).
     */
    @Override
    public void trigger() {
        log.info(I18n.get("log.92ee53efe389", taskId));

        if (AionRuntimeMode.isBootEmbedded()) {
            if (!AionEmbeddedShutdownHandler.requestShutdown(AionEmbeddedShutdownMode.RESTART)) {
                log.warn(I18n.get("log.a611a5bc648e"));
                LoginShutdownRequest.shutdownWithoutHalt();
            }
            return;
        }

        LoginShutdownRequest.startShutdown(true);
    }

    /**
     * 本处理器无需参数，始终有效。
     * This handler needs no params and is always valid.
     *
     * @return true
     */
    @Override
    public boolean isValid() {
        return true;
    }
}
