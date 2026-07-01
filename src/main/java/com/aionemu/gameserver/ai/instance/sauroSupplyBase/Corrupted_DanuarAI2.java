/*
 * This file is part of Encom.
 *
 *  Encom is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Lesser Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  Encom is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Lesser Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser Public License
 *  along with Encom.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.aionemu.gameserver.ai.instance.sauroSupplyBase;

import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import com.aionemu.gameserver.ai.AggressiveNpcAI2;
import com.aionemu.gameserver.ai2.AI2Actions;
import com.aionemu.gameserver.ai2.AIName;

import java.util.concurrent.Future;

/****/
/** Author (Encom)
/****/

@AIName("corrupted_danuar")
public class Corrupted_DanuarAI2 extends AggressiveNpcAI2
{
    private Future<?> skillTask;
	
    @Override
    protected void handleSpawned() {
        super.handleSpawned();
        startpower();
    }
	
    private void startpower() {
        skillTask = GameThreadPoolServices.threadPoolManager().scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
		        AI2Actions.targetSelf(Corrupted_DanuarAI2.this);
                AI2Actions.useSkill(Corrupted_DanuarAI2.this, 21185); //Curse Of The Rune.
            }
        }, 3000, 5000);
    }
	
    private void cancelskillTask() {
        if (skillTask != null && !skillTask.isCancelled()) {
            skillTask.cancel(true);
        }
    }
	
    @Override
    protected void handleDied() {
        cancelskillTask();
        super.handleDied();
    }
	
    @Override
    protected void handleDespawned() {
        cancelskillTask();
        super.handleDespawned();
    }
}