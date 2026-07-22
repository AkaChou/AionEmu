package com.aionemu.loginserver.taskmanager.trigger.implementations;

import com.aionemu.boot.i18n.I18n;
import lombok.extern.slf4j.Slf4j;
import com.aionemu.loginserver.taskmanager.trigger.TaskFromDBTrigger;
import com.aionemu.loginserver.service.LoginThreadPoolServices;
import java.util.Calendar;

/**
 * 定点时刻触发：按 HH:MM:SS 每日固定时间执行。
 * Fixed-in-time trigger: run daily at a fixed HH:MM:SS.
 *
 * @author nrg
 */
@Slf4j
public class FixedInTimeTrigger extends TaskFromDBTrigger {

    /** 一天的毫秒数 / Milliseconds in one day */
    private final int DAY_IN_MSEC = 24 * 60 * 60 * 1000;
    private int hour, minute, second;

    /**
     * 校验参数：单一字符串 HH:MM:SS。
     * Validate params: single HH:MM:SS string.
     */
    @Override
    public boolean isValidTrigger() {
        if (params.length == 1) {
            try {
                String time[] = params[0].split(":");
                hour = Integer.parseInt(time[0]);
                minute = Integer.parseInt(time[1]);
                second = Integer.parseInt(time[2]);
                return true;
            } catch (NumberFormatException e) {
                log.warn(I18n.get("log.93bccf2b3c83", e), e);
            } catch (Exception e) {
                log.warn(I18n.get("log.1d3a1baa4ece", e), e);
            }
        }
        log.warn(I18n.get("log.0029a966209a"));
        return false;
    }

    /**
     * 按 HH:MM:SS 计算延迟并按日周期调度。
     * Compute delay to HH:MM:SS and schedule at fixed daily rate.
     */
    @Override
    public void initTrigger() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, minute);
        calendar.set(Calendar.SECOND, second);

        long delay = calendar.getTimeInMillis() - System.currentTimeMillis();

        if (delay < 0) {
            delay += DAY_IN_MSEC;
        }

        LoginThreadPoolServices.threadPoolManager().scheduleAtFixedRate(this, delay, DAY_IN_MSEC);
    }
}
