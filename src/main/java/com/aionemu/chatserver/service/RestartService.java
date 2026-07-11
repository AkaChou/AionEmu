package com.aionemu.chatserver.service;


import com.aionemu.boot.i18n.I18n;
import com.aionemu.chatserver.configs.Config;
import com.aionemu.chatserver.model.RestartFrequency;
import com.aionemu.commons.utils.AionRuntimeMode;
import java.util.Calendar;
import java.util.Timer;
import java.util.TimerTask;
import lombok.extern.slf4j.Slf4j;

/**
 * 聊天服定时重启服务：按配置频率与时间调度重启请求。
 * Chat-server scheduled restart service: schedules restart requests by configured frequency and time.
 *
 * @author nrg
 */
@Slf4j
public class RestartService {

    private Timer timer;

    /**
     * 读取重启频率配置并初始化定时器。
     * Read restart-frequency config and initialize the timer.
     */
    public RestartService() {
        RestartFrequency rf;
        try {
            rf = RestartFrequency.valueOf(Config.CHATSERVER_RESTART_FREQUENCY);
        } catch (Exception e) {
            log.warn(I18n.get("log.a0c8eb93ca67"));
            rf = RestartFrequency.NEVER;
        }
        setTimer(rf);
    }

    /**
     * 按频率计算下次重启时间并调度任务。
     * Compute the next restart time by frequency and schedule the task.
     *
     * Restart frequency
     */
    private synchronized void setTimer(RestartFrequency frequency) {
        if (frequency == RestartFrequency.NEVER) {
            return;
        }

        // 获取重启时间 / get time to restart
        String[] time = getRestartTime();
        int hour = Integer.parseInt(time[0]);
        int minute = Integer.parseInt(time[1]);

        // 按频率计算正确时间 / calculate the correct time based on frequency
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, minute);
        calendar.set(Calendar.SECOND, 0);
        boolean isMissed = calendar.getTimeInMillis() < System.currentTimeMillis();

        // 切换频率 / switch frequency
        switch (frequency) {
            case DAILY:
                if (isMissed) //execute next day if we missed the time today (what is mostly the case)
                {
                    calendar.add(Calendar.DAY_OF_YEAR, 1);
                }
                break;
            case WEEKLY:
                calendar.add(Calendar.WEEK_OF_YEAR, 1);
                break;
            case MONTHLY:
                calendar.add(Calendar.MONTH, 1);
        }

        // 重启计时器 / Restart timer
        timer = new Timer("ChatServerRestartTimer", AionRuntimeMode.isBootEmbedded());
        timer.schedule(new TimerTask() {
            /**
             * 到点触发重启请求。
             * Fire the restart request when due.
             */
            @Override
            public void run() {
                RestartService.log.info(I18n.get("log.35a74f206b5a"));
                ChatRestartRequest.requestRestart();
            }
        }, calendar.getTime());

        log.info(I18n.get("log.fdc0fbbe4ba1", calendar.getTime().toString()));
    }

    /**
     * 解析配置中的重启时刻；非法格式回退到 5:00。
     * Parse configured restart time; fall back to 5:00 on invalid format.
     *
     * @return [小时, 分钟] 字符串数组 / [hour, minute] string array
     */
    private String[] getRestartTime() {
        String[] time;
        if ((time = Config.CHATSERVER_RESTART_TIME.split(":")).length != 2) {
            log.warn(I18n.get("log.8b80ebdedca9"));
            return new String[]{"5", "0"};
        }
        return time;
    }

    /**
     * 获取单例（已废弃，迁移至 Boot 后请使用注入）。
     * Return the singleton (deprecated; prefer injection after Boot migration).
     *
     * Singleton instance
     * @deprecated boot-migration
     */
    @Deprecated(since = "boot-migration")
    public static RestartService getInstance() {
        return SingletonHolder.INSTANCE;
    }

    /**
     * 取消并清理重启定时器。
     * Cancel and clear the restart timer.
     */
    public synchronized void shutdown() {
        if (timer == null) {
            return;
        }
        timer.cancel();
        timer = null;
    }

    /**
     * 单例持有者。
     * Singleton holder.
     */
    private static final class SingletonHolder {

        private static final RestartService INSTANCE = new RestartService();
    }
}
