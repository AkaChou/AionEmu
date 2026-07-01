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


package com.aionemu.loginserver.taskmanager.trigger.implementations;

import lombok.extern.slf4j.Slf4j;
import com.aionemu.loginserver.service.LoginThreadPoolServices;
import com.aionemu.loginserver.taskmanager.trigger.TaskFromDBTrigger;
/**
 *
 * @author nrg
 */
@Slf4j
public class AfterRestartTrigger extends TaskFromDBTrigger {

    //Indicated wether this task should block or not block the starting progress
    private boolean isBlocking = false;

    @Override
    public boolean isValidTrigger() {
        if (params.length == 1) {
            try {
                isBlocking = Boolean.parseBoolean(this.params[0]);
                return true;
            } catch (Exception e) {
                log.warn("A time for FixedInTimeTrigger is missing or invalid", e);
            }
        }
        log.warn("Not exact 1 parameter for AfterRestartTrigger received, task is not registered");
        return false;
    }

    @Override
    public void initTrigger() {
        if (!isBlocking) {
            LoginThreadPoolServices.threadPoolManager().schedule(this, 5000);
        } else {
            this.run();
        }
    }
}
