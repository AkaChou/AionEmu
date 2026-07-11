package com.aionemu.loginserver.taskmanager.handler.implementations;


import com.aionemu.boot.i18n.I18n;
import lombok.extern.slf4j.Slf4j;
import com.aionemu.commons.utils.AionEmbeddedShutdownHandler;
import com.aionemu.commons.utils.AionRuntimeMode;
import com.aionemu.loginserver.service.LoginShutdownRequest;
import com.aionemu.loginserver.taskmanager.handler.TaskFromDBHandler;

/**
 * 关闭登录服的数据库任务处理器。
 * DB task handler that shuts down the login server.
 *
 * @author Divinity, nrg
 */
@Slf4j
public class ShutdownHandler extends TaskFromDBHandler {


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

    /**
     * 触发登录服关闭（嵌入模式走 embedded 关停，否则标准关停）。
     * Triggers login-server shutdown (embedded mode uses embedded shutdown, otherwise standard shutdown).
     */
    @Override
    public void trigger() {
        log.info(I18n.get("log.130895e4051d", taskId));

        if (AionRuntimeMode.isBootEmbedded()) {
            if (!AionEmbeddedShutdownHandler.requestShutdown()) {
                log.warn(I18n.get("log.a611a5bc648e"));
                LoginShutdownRequest.shutdownWithoutHalt();
            }
            return;
        }

        LoginShutdownRequest.startShutdown(false);
    }
}
