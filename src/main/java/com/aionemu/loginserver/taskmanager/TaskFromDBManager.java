package com.aionemu.loginserver.taskmanager;


import com.aionemu.boot.i18n.I18n;
import lombok.extern.slf4j.Slf4j;
import java.util.ArrayList;
import com.aionemu.commons.database.dao.DAOManager;
import com.aionemu.loginserver.dao.TaskFromDBDAO;
import com.aionemu.loginserver.taskmanager.trigger.TaskFromDBTrigger;

/**
 * 数据库任务管理器：启动时加载全部任务并注册有效触发器。
 * Database task manager: loads all tasks at startup and registers valid triggers.
 *
 * @author nrg
 */
@Slf4j
public class TaskFromDBManager {

    private ArrayList<TaskFromDBTrigger> tasksList;

    /**
     * 从数据库加载任务列表并初始化触发器。
     * Loads the task list from the database and initializes triggers.
     */
    public TaskFromDBManager() {
        tasksList = getDAO().getAllTasks();
        log.info(I18n.get("log.8fed1ca907ff", tasksList.size(), (tasksList.size() > 1 ? "s" : "")));

        registerTaskInstances();
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

    /**
     * 获取 TaskFromDBDAO 快捷方法。
     * Shortcut to obtain {@link TaskFromDBDAO}.
     *
     * @return DAO 实例 / DAO instance
     */
    private static TaskFromDBDAO getDAO() {
        return DAOManager.getDAO(TaskFromDBDAO.class);
    }

    /**
     * 获取单例实例（已弃用，请走 boot 注入）。
     * Returns the singleton instance (deprecated; prefer boot injection).
     *
     * @return 单例实例 / singleton instance
     */
    @Deprecated(since = "boot-migration")
    public static TaskFromDBManager getInstance() {
        return TaskFromDBManager.SingletonHolder.instance;
    }

    /**
     * 单例持有者。
     * Singleton holder.
     */
    private static class SingletonHolder {

        protected static final TaskFromDBManager instance = new TaskFromDBManager();
    }
}
