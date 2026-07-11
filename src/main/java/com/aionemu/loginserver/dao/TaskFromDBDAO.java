package com.aionemu.loginserver.dao;

import java.util.ArrayList;

import com.aionemu.commons.database.dao.DAO;
import com.aionemu.loginserver.taskmanager.trigger.TaskFromDBTrigger;

/**
 * 数据库驱动定时任务数据访问抽象层。
 * DAO that loads scheduled tasks defined in the database.
 *
 * @author Divinity, nrg
 */
public abstract class TaskFromDBDAO implements DAO {

    /**
     * 加载全部数据库任务触发器。
     * Returns all tasks from DB.
     *
     * @return 任务触发器列表 / All task triggers
     */
    public abstract ArrayList<TaskFromDBTrigger> getAllTasks();

    /**
     * 返回实现唯一类名标识。
     * Returns unique class name for all implementations.
     *
     * Fully qualified class name
     */
    @Override
    public final String getClassName() {
        return TaskFromDBDAO.class.getName();
    }
}
