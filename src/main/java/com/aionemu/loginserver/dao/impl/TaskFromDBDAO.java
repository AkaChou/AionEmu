package com.aionemu.loginserver.dao.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import java.util.ArrayList;
import java.util.Objects;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.simple.JdbcClient;

import com.aionemu.boot.i18n.I18n;
import com.aionemu.commons.database.DatabaseFactory;
import com.aionemu.loginserver.taskmanager.handler.TaskFromDBHandler;
import com.aionemu.loginserver.taskmanager.handler.TaskFromDBHandlerHolder;
import com.aionemu.loginserver.taskmanager.trigger.TaskFromDBTrigger;
import com.aionemu.loginserver.taskmanager.trigger.TaskFromDBTriggerHolder;

import lombok.extern.slf4j.Slf4j;

/**
 * 数据库定时任务 DAO 的 MySQL 8 实现。
 * MySQL 8 TaskFromDBDAO implementation using JdbcClient.
 * @author Updated for MySQL 8
 */
@Slf4j
@Repository("loginTaskFromDBDAO")
public class TaskFromDBDAO extends com.aionemu.loginserver.dao.TaskFromDBDAO {

    /** 查询全部任务 SQL / Select all tasks SQL*/
    private static final String SELECT_ALL_QUERY = "SELECT * FROM tasks ORDER BY id";

    private final JdbcClient jdbcClient;

    public TaskFromDBDAO() {
        this(DatabaseFactory.getJdbcClient());
    }

    @Autowired

    public TaskFromDBDAO(JdbcClient jdbcClient) {
        this.jdbcClient = Objects.requireNonNull(jdbcClient, "jdbcClient must not be null");
    }

    @Override
    public ArrayList<TaskFromDBTrigger> getAllTasks() {
        ArrayList<TaskFromDBTrigger> result = new ArrayList<>();

        try {
            jdbcClient.sql(SELECT_ALL_QUERY)
                .query((rs, _) -> {
                    try {
                        TaskFromDBTrigger trigger = TaskFromDBTriggerHolder.valueOf(rs.getString("trigger_type"))
                            .getTriggerClass()
                            .getDeclaredConstructor()
                            .newInstance();

                        TaskFromDBHandler handler = TaskFromDBHandlerHolder.valueOf(rs.getString("task_type"))
                            .getTaskClass()
                            .getDeclaredConstructor()
                            .newInstance();

                        handler.setTaskId(rs.getInt("id"));

                        String execParamsResult = rs.getString("exec_param");
                        if (execParamsResult != null && !execParamsResult.isEmpty()) {
                            handler.setParams(execParamsResult.split(" "));
                        }

                        trigger.setHandlerToTrigger(handler);

                        String triggerParamsResult = rs.getString("trigger_param");
                        if (triggerParamsResult != null && !triggerParamsResult.isEmpty()) {
                            trigger.setParams(triggerParamsResult.split(" "));
                        }

                        return trigger;
                    } catch (Exception ex) {
                        log.error(I18n.get("log.51b28941ecc3", ex.getMessage()), ex);
                        return null;
                    }
                })
                .list()
                .forEach(trigger -> {
                    if (trigger != null) {
                        result.add(trigger);
                    }
                });
        } catch (DataAccessException e) {
            log.error(I18n.get("log.098a63ad15f2"), e);
        }

        return result;
    }

    @Override
    public boolean supports(String database, int majorVersion, int minorVersion) {
        return DAOUtils.supports(database, majorVersion, minorVersion);
    }
}
