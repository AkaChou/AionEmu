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
package ai.instance.steelRake;

import ai.ActionItemNpcAI2;
import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.TeleportAnimation;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.services.teleport.TeleportService2;
import com.aionemu.gameserver.utils.PacketSendUtility;

/****/
/** Author (Encom)
/****/

@AIName("accountant_cabin_exit")
public class AccountantCabinExitAI2 extends ActionItemNpcAI2 {

	@Override
	protected void handleUseItemFinish(Player player) {
		switch (getNpcId()) {
		    case 730766: //Accountant's Cabin Exit.
				switch (player.getWorldId()) {
					case 300100000: //Steel Rake 1.5
					    if (player.getCommonData().getRace() == Race.ASMODIANS) {
						    PacketSendUtility.sendMessage(player, "you enter <Beluslan>");
						    TeleportService2.teleportTo(player, 220040000, 2685.6497f, 998.47107f, 378.00754f, (byte) 60, TeleportAnimation.BEAM_ANIMATION);
			            } else if (player.getCommonData().getRace() == Race.ELYOS) {
						    PacketSendUtility.sendMessage(player, "you enter <Heiron>");
							TeleportService2.teleportTo(player, 210040000, 1246.5492f, 2557.4895f, 138.75f, (byte) 60, TeleportAnimation.BEAM_ANIMATION);
						}
			        break;
					case 300460000: //Steel Rake Cabin 3.0
					    if (player.getCommonData().getRace() == Race.ASMODIANS) {
						    PacketSendUtility.sendMessage(player, "you enter <Beluslan>");
						    TeleportService2.teleportTo(player, 220040000, 2685.6497f, 998.47107f, 378.00754f, (byte) 60, TeleportAnimation.BEAM_ANIMATION);
			            } else if (player.getCommonData().getRace() == Race.ELYOS) {
						    PacketSendUtility.sendMessage(player, "you enter <Heiron>");
							TeleportService2.teleportTo(player, 210040000, 1246.5492f, 2557.4895f, 138.75f, (byte) 60, TeleportAnimation.BEAM_ANIMATION);
						}
			        break;
				}
		    break;
		}
	}
}