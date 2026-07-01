/**
 * This file is part of Aion-Lightning <aion-lightning.org>.
 *
 *  Aion-Lightning is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  Aion-Lightning is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details. *
 *  You should have received a copy of the GNU General Public License
 *  along with Aion-Lightning.
 *  If not, see <http://www.gnu.org/licenses/>.
 */


package com.aionemu.loginserver.taskmanager.handler.implementations;

import lombok.extern.slf4j.Slf4j;
import com.aionemu.commons.database.dao.DAOManager;
import com.aionemu.loginserver.dao.AccountDAO;
import com.aionemu.loginserver.taskmanager.handler.TaskFromDBHandler;
/**
 *
 * @author nrg
 */
@Slf4j
public class CleanAccountsHandler extends TaskFromDBHandler {

    private int daysOfInactivity;

    @Override
    public boolean isValid() {
        if (params.length != 1) {
            log.warn("CleanAccountHandler has not exactly one parameter (daysOfInactivity) - handler is not registered");
            return false;
        }
        return true;
    }

    @Override
    public void trigger() {
        daysOfInactivity = Integer.parseInt(params[0]);
        log.info("Deleting all accounts, older as " + daysOfInactivity + " days");
        DAOManager.getDAO(AccountDAO.class).deleteInactiveAccounts(daysOfInactivity);
    }
}
