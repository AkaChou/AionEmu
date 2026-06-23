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
package ai.rvr;

import ai.AggressiveNpcAI2;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.commons.network.util.ThreadPoolManager;
import com.aionemu.commons.utils.Rnd;
import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.model.gameobjects.Npc;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.List;

/****/
/** Author (Encom)
/****/

@AIName("dynamic_iluma_monster")
public class Dynamic_Iluma_MonsterAI2 extends AggressiveNpcAI2 {	

	
	@Override
	protected void handleDied() {
		switch (Rnd.get(1, 24)) {
			case 1:
				spawnLF6EventDoor();
			break;
			case 2:
			break;
			case 3:
			break;
			case 4:
			break;
			case 5:
			break;
			case 6:
			break;
			case 7:
			break;
			case 8:
			break;
			case 9:
			break;
			case 10:
			break;
			case 11:
			break;
			case 12:
			break;
			case 13:
			break;
			case 14:
			break;
			case 15:
			break;
			case 16:
			break;
			case 17:
			break;
			case 18:
			break;
			case 19:
			break;
			case 20:
			break;
			case 21:
			break;
			case 22:
			break;
			case 23:
			break;
			case 24:
			break;
		}
		super.handleDied();
	}

	private void spawnLF6EventDoor() {
		switch (Rnd.get(1, 4)) {
			case 1:
				spawn(241053, 804.516f, 1842.256f, 305.86325f, (byte) 0); //Portal.
			    spawn(240887, 804.516f, 1842.256f, 305.86325f, (byte) 0); //Archon Warrior.
				spawn(241053, 1865.9077f, 2071.3066f, 344.01154f, (byte) 0); //Portal.
				spawn(240888, 1865.9077f, 2071.3066f, 344.01154f, (byte) 0); //Archon Mage.
				spawn(241053, 2436.7002f, 1326.0476f, 224.875f, (byte) 0); //Portal.
				spawn(240889, 2436.7002f, 1326.0476f, 224.875f, (byte) 0); //Archon Scout.
				spawn(241053, 1358.5513f, 335.7875f, 348.35382f, (byte) 0); //Portal.
				spawn(240890, 1358.5513f, 335.7875f, 348.35382f, (byte) 0); //Archon Marksman.
			break;
			case 2:
				spawn(241053, 804.516f, 1842.256f, 305.86325f, (byte) 0); //Portal.
			    spawn(240888, 804.516f, 1842.256f, 305.86325f, (byte) 0); //Archon Mage.
				spawn(241053, 1865.9077f, 2071.3066f, 344.01154f, (byte) 0); //Portal.
				spawn(240889, 1865.9077f, 2071.3066f, 344.01154f, (byte) 0); //Archon Scout.
				spawn(241053, 2436.7002f, 1326.0476f, 224.875f, (byte) 0); //Portal.
				spawn(240890, 2436.7002f, 1326.0476f, 224.875f, (byte) 0); //Archon Marksman.
				spawn(241053, 1358.5513f, 335.7875f, 348.35382f, (byte) 0); //Portal.
				spawn(240887, 1358.5513f, 335.7875f, 348.35382f, (byte) 0); //Archon Warrior.
			break;
			case 3:
				spawn(241053, 804.516f, 1842.256f, 305.86325f, (byte) 0); //Portal.
			    spawn(240889, 804.516f, 1842.256f, 305.86325f, (byte) 0); //Archon Scout.
				spawn(241053, 1865.9077f, 2071.3066f, 344.01154f, (byte) 0); //Portal.
				spawn(240890, 1865.9077f, 2071.3066f, 344.01154f, (byte) 0); //Archon Marksman.
				spawn(241053, 2436.7002f, 1326.0476f, 224.875f, (byte) 0); //Portal.
				spawn(240887, 2436.7002f, 1326.0476f, 224.875f, (byte) 0); //Archon Warrior.
				spawn(241053, 1358.5513f, 335.7875f, 348.35382f, (byte) 0); //Portal.
				spawn(240888, 1358.5513f, 335.7875f, 348.35382f, (byte) 0); //Archon Mage.
			break;
			case 4:
				spawn(241053, 804.516f, 1842.256f, 305.86325f, (byte) 0); //Portal.
			    spawn(240890, 804.516f, 1842.256f, 305.86325f, (byte) 0); //Archon Marksman.
				spawn(241053, 1865.9077f, 2071.3066f, 344.01154f, (byte) 0); //Portal.
				spawn(240887, 1865.9077f, 2071.3066f, 344.01154f, (byte) 0); //Archon Warrior.
				spawn(241053, 2436.7002f, 1326.0476f, 224.875f, (byte) 0); //Portal.
				spawn(240888, 2436.7002f, 1326.0476f, 224.875f, (byte) 0); //Archon Mage.
				spawn(241053, 1358.5513f, 335.7875f, 348.35382f, (byte) 0); //Portal.
				spawn(240889, 1358.5513f, 335.7875f, 348.35382f, (byte) 0); //Archon Scout.
			break;
		}
		ThreadPoolManager.getInstance().schedule(new Runnable() {
			@Override
			public void run() {
				despawnNpc(241053); //Portal.
			}
		}, 60000);
	}
	
	private void despawnNpc(int npcId) {
		if (getPosition().getWorldMapInstance().getNpcs(npcId) != null) {
			List<Npc> npcs = getPosition().getWorldMapInstance().getNpcs(npcId);
			for (Npc npc: npcs) {
				npc.getController().onDelete();
			}
		}
	}
}