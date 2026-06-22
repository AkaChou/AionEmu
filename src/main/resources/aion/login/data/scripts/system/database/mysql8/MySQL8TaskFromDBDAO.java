package mysql8;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.aionemu.commons.database.DatabaseFactory;
import com.aionemu.loginserver.dao.TaskFromDBDAO;
import com.aionemu.loginserver.taskmanager.handler.TaskFromDBHandler;
import com.aionemu.loginserver.taskmanager.handler.TaskFromDBHandlerHolder;
import com.aionemu.loginserver.taskmanager.trigger.TaskFromDBTrigger;
import com.aionemu.loginserver.taskmanager.trigger.TaskFromDBTriggerHolder;

/**
 * MySQL8 TaskFromDB DAO implementation
 * 
 * @author Updated for MySQL 8
 */
public class MySQL8TaskFromDBDAO extends TaskFromDBDAO {

    private static final Logger log = LoggerFactory.getLogger(MySQL8TaskFromDBDAO.class);
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
                    log.error("Error creating task from DB: " + ex.getMessage(), ex);
                }
            }
        } catch (SQLException e) {
            log.error("Loading tasks failed: ", e);
        }

        return result;
    }

    @Override
    public boolean supports(String s, int i, int i1) {
        return MySQL8DAOUtils.supports(s, i, i1);
    }
}