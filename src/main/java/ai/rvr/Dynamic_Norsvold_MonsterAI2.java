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
import com.aionemu.commons.network.util.ThreadPoolManager;
import com.aionemu.commons.utils.Rnd;
import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.model.gameobjects.Npc;

import java.util.List;

/****/
/** Author (Encom)
/****/

@AIName("dynamic_norsvold_monster")
public class Dynamic_Norsvold_MonsterAI2 extends AggressiveNpcAI2
{
	@Override
	protected void handleDied() {
		switch (Rnd.get(1, 24)) {
			case 1:
				spawnDF6EventDoor();
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
	
	private void spawnDF6EventDoor() {
		switch (Rnd.get(1, 4)) {
			case 1:
			    spawn(241054, 1026.9843f, 1356.3972f, 284.44946f, (byte) 0); //Portal.
				spawn(240971, 1026.9843f, 1356.3972f, 284.44946f, (byte) 0); //Guardian Warrior.
				spawn(241054, 1477.9161f, 1006.49695f, 231.65082f, (byte) 0); //Portal.
				spawn(240972, 1477.9161f, 1006.49695f, 231.65082f, (byte) 0); //Guardian Mage.
				spawn(241054, 2227.855f, 1868.7146f, 235.28452f, (byte) 0); //Portal.
				spawn(240973, 2227.855f, 1868.7146f, 235.28452f, (byte) 0); //Guardian Scout.
				spawn(241054, 1133.7673f, 2570.3909f, 235.72313f, (byte) 0); //Portal.
				spawn(240974, 1133.7673f, 2570.3909f, 235.72313f, (byte) 0); //Guardian Marksman.
			break;
			case 2:
				spawn(241054, 1026.9843f, 1356.3972f, 284.44946f, (byte) 0); //Portal.
				spawn(240972, 1026.9843f, 1356.3972f, 284.44946f, (byte) 0); //Guardian Mage.
				spawn(241054, 1477.9161f, 1006.49695f, 231.65082f, (byte) 0); //Portal.
				spawn(240973, 1477.9161f, 1006.49695f, 231.65082f, (byte) 0); //Guardian Scout.
				spawn(241054, 2227.855f, 1868.7146f, 235.28452f, (byte) 0); //Portal.
				spawn(240974, 2227.855f, 1868.7146f, 235.28452f, (byte) 0); //Guardian Marksman.
				spawn(241054, 1133.7673f, 2570.3909f, 235.72313f, (byte) 0); //Portal.
				spawn(240971, 1133.7673f, 2570.3909f, 235.72313f, (byte) 0); //Guardian Warrior.
			break;
			case 3:
				spawn(241054, 1026.9843f, 1356.3972f, 284.44946f, (byte) 0); //Portal.
				spawn(240973, 1026.9843f, 1356.3972f, 284.44946f, (byte) 0); //Guardian Scout.
				spawn(241054, 1477.9161f, 1006.49695f, 231.65082f, (byte) 0); //Portal.
				spawn(240974, 1477.9161f, 1006.49695f, 231.65082f, (byte) 0); //Guardian Marksman.
				spawn(241054, 2227.855f, 1868.7146f, 235.28452f, (byte) 0); //Portal.
				spawn(240971, 2227.855f, 1868.7146f, 235.28452f, (byte) 0); //Guardian Warrior.
				spawn(241054, 1133.7673f, 2570.3909f, 235.72313f, (byte) 0); //Portal.
				spawn(240972, 1133.7673f, 2570.3909f, 235.72313f, (byte) 0); //Guardian Mage.
			break;
			case 4:
				spawn(241054, 1026.9843f, 1356.3972f, 284.44946f, (byte) 0); //Portal.
				spawn(240974, 1026.9843f, 1356.3972f, 284.44946f, (byte) 0); //Guardian Marksman.
				spawn(241054, 1477.9161f, 1006.49695f, 231.65082f, (byte) 0); //Portal.
				spawn(240971, 1477.9161f, 1006.49695f, 231.65082f, (byte) 0); //Guardian Warrior.
				spawn(241054, 2227.855f, 1868.7146f, 235.28452f, (byte) 0); //Portal.
				spawn(240972, 2227.855f, 1868.7146f, 235.28452f, (byte) 0); //Guardian Mage.
				spawn(241054, 1133.7673f, 2570.3909f, 235.72313f, (byte) 0); //Portal.
				spawn(240973, 1133.7673f, 2570.3909f, 235.72313f, (byte) 0); //Guardian Scout.
			break;
		}
		ThreadPoolManager.getInstance().schedule(new Runnable() {
			@Override
			public void run() {
				despawnNpc(241054); //Portal.
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