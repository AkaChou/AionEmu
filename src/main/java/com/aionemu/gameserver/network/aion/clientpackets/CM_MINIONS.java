/*

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
package com.aionemu.gameserver.network.aion.clientpackets;

import lombok.extern.slf4j.Slf4j;

import com.aionemu.gameserver.lifecycle.GameEventBootstrapServices;

import java.util.ArrayList;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.AionClientPacket;
import com.aionemu.gameserver.network.aion.AionConnection.State;
import com.aionemu.gameserver.services.NameRestrictionService;
import com.aionemu.gameserver.services.toypet.MinionService;
import com.aionemu.gameserver.services.toypet.PetSpawnService;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * @author Falke_34, FrozenKiller Reworked by G-Robson26
 */
@Slf4j
public class CM_MINIONS extends AionClientPacket {

	private int actionId;
	private String minionName;
	private int objectId;
	private int itemObjectId;
	@SuppressWarnings("unused")
	private boolean isSpawned; // Should be in DB (TODO)
	private int charge;
	private int autoCharge;
	private int functId;
	private int subSwitch;
	private int minionObjectId;
	private int dopingItemId;
	private int targetSlot;
	private int destinationSlot;
	private int unk;
	private ArrayList<Integer> MaterialObjIds = new ArrayList<Integer>();
	private int lock = 0;

	public CM_MINIONS(int opcode, State state, State... restStates) {
		super(opcode, state, restStates);
	}

	@Override
	protected void readImpl() {
		actionId = readH();
		switch (actionId) {
		case 0: // add
			itemObjectId = readD(); // Item UniqueId (Minion Contract)
			break;
		case 1: // delete
			objectId = readD();
			break;
		case 2: // rename
			objectId = readD(); // Minion Unique ID
			minionName = readS(); // Name
			break;
		case 3: // locked
			objectId = readD(); // Minion Unique ID
			lock = readC(); // lock/unlock Todo
			break;
		case 4: // summon
		case 5: // unsummon
			minionObjectId = readD(); // Minion Unique ID
			break;
		case 6: // ascension
			MaterialObjIds.clear();
			objectId = readD(); // Minion Unique ID
			for (int i = 0; i < 10; i++) {
				MaterialObjIds.add(readD());
			}
			break;
		case 7: // evolution
			objectId = readD();
			break;
		case 8:// combination
			MaterialObjIds.clear();
			for (int i = 0; i < 4; i++) {
				MaterialObjIds.add(readD());
			}
			break;
		case 9: // TODO (MinionFunction Scrolls etc)
			subSwitch = readD(); // 0, 1
			log.debug("CM_MINIONS function subSwitch={}", subSwitch);
			switch (subSwitch) {
			case 0: {
				functId = readD();
				switch (functId) {
				case 0: {// add item
					minionObjectId = readD();
					dopingItemId = readD();
					targetSlot = readD();
					log.debug("CM_MINIONS add item. subSwitch={} functionId={} minionObjectId={} itemId={} targetSlot={}",
							subSwitch, functId, minionObjectId, dopingItemId, targetSlot);
					break;
				}
				case 1: {
					minionObjectId = readD();
					targetSlot = readD();
					unk = readD();
					log.debug("CM_MINIONS function. subSwitch={} functionId={} minionObjectId={} targetSlot={} unk={}",
							subSwitch, functId, minionObjectId, targetSlot, unk);
					break;
				}
				case 2: {
					minionObjectId = readD();
					targetSlot = readD();
					destinationSlot = readD();
					log.debug("CM_MINIONS move item. subSwitch={} functionId={} minionObjectId={} targetSlot={} destinationSlot={}",
							subSwitch, functId, minionObjectId, targetSlot, destinationSlot);
					break;
				}
				case 3: {// BUFF ON
					minionObjectId = readD();
					dopingItemId = readD();
					targetSlot = readD();
					log.debug("CM_MINIONS buff. subSwitch={} functionId={} minionObjectId={} itemId={} targetSlot={}",
							subSwitch, functId, minionObjectId, dopingItemId, targetSlot);
					break;
				}
				case 4: {
					minionObjectId = readD();
					log.debug("CM_MINIONS function. subSwitch={} functionId={} minionObjectId={}", subSwitch, functId,
							minionObjectId);
					break;
				}
				}
				break;
			}
			case 1: {// Auto Loot
				minionObjectId = readD();
				break;
			}
			}
			break;
		case 10: // Nothing to read (Falke Log 5.6_Minion_Function)
			break;
		case 11: // charge
			charge = readC(); // Charge 1 = true / 0 = false ?
			autoCharge = readC(); // Auto Recharge on/off Todo
			break;
		case 12:
			readC(); // Auto Function on/off
			break;
		case 13:
			readD();
			readC();
			readH();
			break;
		case 14: // BUFF ON
			readC(); // 20?
			readC();
			readC();
			break;
		default:
			break;
		}
	}

	@Override
	protected void runImpl() {
		Player player = getConnection().getActivePlayer();
		if (player == null) {
			return;
		}
		switch (actionId) {
		case 0:
			GameEventBootstrapServices.minionService().addMinion(player, itemObjectId);
			break;
		case 1:
			GameEventBootstrapServices.minionService().deleteMinion(player, objectId, false);
			break;
		case 2:
			if (NameRestrictionService.isForbiddenWord(minionName)) {
				PacketSendUtility.sendMessage(player, "You are trying to use a forbidden name. Choose another one!");
			} else {
				GameEventBootstrapServices.minionService().renameMinion(player, objectId, minionName);
			}
			break;
		case 3:
			GameEventBootstrapServices.minionService().lockMinion(player, objectId, lock);
			break;
		case 4:
			if (player.getPet() != null) {
				PetSpawnService.dismissPet(player, true);
			}
			GameEventBootstrapServices.minionService().spawnMinion(player, minionObjectId);
			break;
		case 5:
			GameEventBootstrapServices.minionService().despawnMinion(player, minionObjectId);
			break;
		case 6:
			GameEventBootstrapServices.minionService().growthUpMinion(player, objectId, MaterialObjIds);
			break;
		case 7:
			GameEventBootstrapServices.minionService().evolutionUpMinion(player, objectId);
			break;
		case 8:
			GameEventBootstrapServices.minionService().CombinationMinion(player, MaterialObjIds);
			break;
		case 9: // TODO
			switch (subSwitch) {
			case 0: {
				switch (functId) {
				case 0: { // Add Item
					log.debug("CM_MINIONS handle add item. playerId={} minionObjectId={} itemId={} targetSlot={}",
							player.getObjectId(), minionObjectId, dopingItemId, targetSlot);
					GameEventBootstrapServices.minionService().addMinionFunctionItems(player, functId, minionObjectId, dopingItemId,
							targetSlot, destinationSlot); // Scrolls etc
					break;
				}
				case 2: {
					log.debug("CM_MINIONS relocate doping. playerId={} minionObjectId={} targetSlot={} destinationSlot={}",
							player.getObjectId(), minionObjectId, targetSlot, destinationSlot);
					GameEventBootstrapServices.minionService().relocateDoping(player, minionObjectId, targetSlot, destinationSlot);
					break;
				}
				case 3: {
					log.debug("CM_MINIONS buff on. playerId={} minionObjectId={} itemId={} targetSlot={}",
							player.getObjectId(), minionObjectId, dopingItemId, targetSlot);
					GameEventBootstrapServices.minionService().buffPlayer(player, minionObjectId, dopingItemId, targetSlot); // Buff
					break;
				}
				}
				break;
			}
			case 1: {
				log.debug("CM_MINIONS autoloot toggle. playerId={} minionObjectId={}", player.getObjectId(), minionObjectId);
				GameEventBootstrapServices.minionService().activateLoot(player, true);
				break;
			}
			}
			break;
		case 10: // MinionFunction (Activate)
			GameEventBootstrapServices.minionService().activateMinionFunction(player);
			break;
		case 11:
			GameEventBootstrapServices.minionService().addMinionSkillPoints(player, charge == 1 ? true : false,
					autoCharge == 1 ? true : false);
			// TODO
			// GameEventBootstrapServices.minionService().chargeMinion(player, todo2 == 1 ? true : false);
			break;
		case 12:
			break;
		case 13:
			break;
		case 14:
			break;
		default:
			break;
		}
	}
}
