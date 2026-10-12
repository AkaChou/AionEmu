package com.aionemu.loginserver.taskmanager;

import com.aionemu.boot.i18n.I18n;
import java.util.ArrayList;

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import com.aionemu.loginserver.Shutdown;
import com.aionemu.loginserver.persistence.dao.AccountDataDao;
import com.aionemu.loginserver.persistence.dao.TaskDao;
import com.aionemu.loginserver.persistence.entity.TaskPo;
import com.aionemu.loginserver.taskmanager.handler.TaskFromDBHandler;
import com.aionemu.loginserver.taskmanager.handler.TaskFromDBHandlerHolder;
import com.aionemu.loginserver.taskmanager.trigger.TaskFromDBTrigger;
import com.aionemu.loginserver.taskmanager.trigger.TaskFromDBTriggerHolder;
import com.aionemu.loginserver.utils.ThreadPoolManager;

import lombok.extern.slf4j.Slf4j;

/**
 * 数据库任务管理器：启动时加载全部任务并注册有效触发器。
 * Database task manager: loads all tasks at startup and registers valid triggers.
 * @author nrg
 */
@Slf4j
@Service
@Lazy
public class TaskFromDBManager {

    private final ArrayList<TaskFromDBTrigger> tasksList = new ArrayList<>();

    /**
     * 从数据库加载任务行，反射装配触发器/处理器并初始化。
     * Loads task rows from the database, reflectively builds triggers/handlers and initializes them.
     * @param tasksDao 任务 DAO / tasks DAO
     * @param threadPoolManager 登录服线程池 / login thread pool
     * @param accountDao 账号 DAO / account DAO
     * @param shutdown 关机协调器 / shutdown coordinator
     */
    public TaskFromDBManager(TaskDao tasksDao, ThreadPoolManager threadPoolManager, AccountDataDao accountDao,
            Shutdown shutdown) {
        TaskRuntime runtime = new TaskRuntime(threadPoolManager, accountDao, shutdown);
        for (TaskPo po : tasksDao.getAllTasks()) {
            TaskFromDBTrigger trigger = buildTrigger(po, runtime);
            if (trigger != null) {
                tasksList.add(trigger);
            }
        }
        log.info(I18n.get("log.8fed1ca907ff", tasksList.size(), (tasksList.size() > 1 ? "s" : "")));

        registerTaskInstances();
    }

    /**
     * 按任务行反射构建触发器；类型未注册或无法实例化时跳过该行。
     * Reflectively builds a trigger from the task row; skips rows whose types are unregistered or not instantiable.
     * @param po 任务行 / task row
     * @param runtime 运行时依赖 / runtime dependencies
     * @return 触发器，或构建失败时为 null / trigger, or null when building fails
     */
    private static TaskFromDBTrigger buildTrigger(TaskPo po, TaskRuntime runtime) {
        try {
            TaskFromDBTrigger trigger = TaskFromDBTriggerHolder.valueOf(po.getTriggerType())
                .getTriggerClass()
                .getDeclaredConstructor()
                .newInstance();
            TaskFromDBHandler handler = TaskFromDBHandlerHolder.valueOf(po.getTaskType())
                .getTaskClass()
                .getDeclaredConstructor()
                .newInstance();
            handler.setTaskId(po.getId());
            if (po.getExecParam() != null && !po.getExecParam().isEmpty()) {
                handler.setParams(po.getExecParam().split(" "));
            }
            handler.setRuntime(runtime);
            trigger.setHandlerToTrigger(handler);
            if (po.getTriggerParam() != null && !po.getTriggerParam().isEmpty()) {
                trigger.setParams(po.getTriggerParam().split(" "));
            }
            trigger.setRuntime(runtime);
            return trigger;
        } catch (Exception ex) {
            // 与旧实现一致：无法构建的行静默跳过 / same as before: rows that cannot build are skipped
            return null;
        }
    }

    /**
     * 注册并启动所有有效的任务触发器。
     * Registers and starts all valid task triggers.
     */
    private void registerTaskInstances() {
        // 用于所有来自 DB 的任务 / For all tasks from DB
        for (TaskFromDBTrigger trigger : tasksList) {
            if (trigger.isValid()) {
                trigger.initTrigger();
            } else {
                log.error(I18n.get("log.cf872b497fc9", trigger.getTaskId()));
            }
        }
    }
}
