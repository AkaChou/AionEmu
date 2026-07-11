package com.aionemu.loginserver.dao.mysql8;


import com.aionemu.boot.i18n.I18n;
import lombok.extern.slf4j.Slf4j;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

import com.aionemu.commons.database.DatabaseFactory;
import com.aionemu.loginserver.dao.TaskFromDBDAO;
import com.aionemu.loginserver.taskmanager.handler.TaskFromDBHandler;
import com.aionemu.loginserver.taskmanager.handler.TaskFromDBHandlerHolder;
import com.aionemu.loginserver.taskmanager.trigger.TaskFromDBTrigger;
import com.aionemu.loginserver.taskmanager.trigger.TaskFromDBTriggerHolder;

/**
 * 数据库定时任务 DAO 的 MySQL 8 实现。
 * MySQL 8 TaskFromDBDAO implementation.
 *
 * @author Updated for MySQL 8
 */
@Slf4j
public class MySQL8TaskFromDBDAO extends TaskFromDBDAO {

    /** 查询全部任务 SQL / Select all tasks SQL*/
    private static final String SELECT_ALL_QUERY = "SELECT * FROM tasks ORDER BY id";

    @Override
    public ArrayList<TaskFromDBTrigger> getAllTasks() {
        ArrayList<TaskFromDBTrigger> result = new ArrayList<>();

        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(SELECT_ALL_QUERY);
             ResultSet rset = stmt.executeQuery()) {

            while (rset.next()) {
                try {
                    TaskFromDBTrigger trigger = TaskFromDBTriggerHolder.valueOf(rset.getString("trigger_type")).getTriggerClass().getDeclaredConstructor().newInstance();

                    TaskFromDBHandler handler = TaskFromDBHandlerHolder.valueOf(rset.getString("task_type")).getTaskClass().getDeclaredConstructor().newInstance();

                    handler.setTaskId(rset.getInt("id"));

                    String execParamsResult = rset.getString("exec_param");
                    if (execParamsResult != null && !execParamsResult.isEmpty()) {
                        handler.setParams(execParamsResult.split(" "));
                    }

                    trigger.setHandlerToTrigger(handler);

                    String triggerParamsResult = rset.getString("trigger_param");
                    if (triggerParamsResult != null && !triggerParamsResult.isEmpty()) {
                        trigger.setParams(triggerParamsResult.split(" "));
                    }

                    result.add(trigger);
                } catch (Exception ex) {
                    log.error(I18n.get("log.51b28941ecc3", ex.getMessage(), ex));
                }
            }
        } catch (SQLException e) {
            log.error(I18n.get("log.098a63ad15f2", e));
        }

        return result;
    }

    @Override
    public boolean supports(String s, int i, int i1) {
        return MySQL8DAOUtils.supports(s, i, i1);
    }
}
