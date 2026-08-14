package com.aionemu.loginserver.taskmanager.handler.implementations;


import com.aionemu.boot.i18n.I18n;
import lombok.extern.slf4j.Slf4j;
import com.aionemu.commons.database.dao.DAOManager;
import com.aionemu.loginserver.dao.AccountDAO;
import com.aionemu.loginserver.taskmanager.handler.TaskFromDBHandler;

/**
 * 清理长期未活跃账号的数据库任务处理器。
 * DB task handler that deletes accounts inactive for a configured number of days.
 *
 * @author nrg
 */
@Slf4j
public class CleanAccountsHandler extends TaskFromDBHandler {

    /** 不活跃天数配置。 / Days of inactivity. */
    private int daysOfInactivity;

    /**
     * 校验参数：需要恰好一个表示不活跃天数的参数。
     * Validates params: requires exactly one parameter (days of inactivity).
     *
     * @return 参数是否有效 / whether params are valid
     */
    @Override
    public boolean isValid() {
        if (params.length != 1) {
            log.warn(I18n.get("log.0c023861a7d8"));
            return false;
        }
        return true;
    }

    /**
     * 按配置天数删除不活跃账号。
     * Deletes inactive accounts for the configured number of days.
     */
    @Override
    public void trigger() {
        daysOfInactivity = Integer.parseInt(params[0]);
        log.info(I18n.get("log.18943e91d563", daysOfInactivity));
        DAOManager.getDAO(AccountDAO.class).deleteInactiveAccounts(daysOfInactivity);
    }
}
