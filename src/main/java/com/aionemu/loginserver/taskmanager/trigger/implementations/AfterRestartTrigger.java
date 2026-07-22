package com.aionemu.loginserver.taskmanager.trigger.implementations;

import com.aionemu.boot.i18n.I18n;
import lombok.extern.slf4j.Slf4j;
import com.aionemu.loginserver.service.LoginThreadPoolServices;
import com.aionemu.loginserver.taskmanager.trigger.TaskFromDBTrigger;

/**
 * 重启后触发：启动完成后延迟或阻塞执行任务。
 * After-restart trigger: run the task delayed or blocking once startup finishes.
 *
 * @author nrg
 */
@Slf4j
public class AfterRestartTrigger extends TaskFromDBTrigger {

    /** 是否阻塞启动流程 / Whether this task should block startup */
    private boolean isBlocking = false;

    /**
     * 校验参数：单一布尔值表示是否阻塞启动。
     * Validate params: single boolean for blocking startup.
     */
    @Override
    public boolean isValidTrigger() {
        if (params.length == 1) {
            try {
                isBlocking = Boolean.parseBoolean(this.params[0]);
                return true;
            } catch (Exception e) {
                log.warn(I18n.get("log.1d3a1baa4ece", e), e);
            }
        }
        log.warn(I18n.get("log.39cfac3b050e"));
        return false;
    }

    /**
     * 初始化：非阻塞则 5 秒后调度，阻塞则立即执行。
     * Init: schedule after 5s if non-blocking, otherwise run immediately.
     */
    @Override
    public void initTrigger() {
        if (!isBlocking) {
            LoginThreadPoolServices.threadPoolManager().schedule(this, 5000);
        } else {
            this.run();
        }
    }
}
