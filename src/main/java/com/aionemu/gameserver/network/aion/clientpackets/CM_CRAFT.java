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
import com.aionemu.gameserver.model.gameobjects.VisibleObject;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.AionClientPacket;
import com.aionemu.gameserver.network.aion.AionConnection.State;
import com.aionemu.gameserver.services.craft.CraftService;
import com.aionemu.gameserver.utils.MathUtil;
@Slf4j

public class CM_CRAFT extends AionClientPacket {
	private int itemID;
	private long itemCount;
	private int unk;
	private int targetTemplateId;
	private int recipeId;
	private int targetObjId;
	private int materialsCount;
	private int craftType;
	private int craftCount = 1;
	private boolean componentsOk = true;

	public CM_CRAFT(int opcode, State state, State... restStates) {
		super(opcode, state, restStates);
	}

	@Override
	protected void readImpl() {
		Player player = getConnection().getActivePlayer();
		unk = readC();
		targetTemplateId = readD();
		recipeId = readD();
		targetObjId = readD();
		materialsCount = readH();
		craftType = readC();
		if (craftType == 0) {
			craftCount = 0;
			componentsOk = true;
			for (int i = 0; i < materialsCount; i++) {
				itemID = readD();
				itemCount = readQ();
				int materialCraftCount = CraftService.checkComponents(player, recipeId, itemID, itemCount);
				if (materialCraftCount < 1) {
					componentsOk = false;
				} else {
					craftCount = craftCount == 0 ? materialCraftCount : Math.min(craftCount, materialCraftCount);
				}
			}
		}
	}

	@Override
	protected void runImpl() {
		Player player = getConnection().getActivePlayer();
		if (player == null || !player.isSpawned()) {
			return;
		}
		if (player.getController().isInShutdownProgress()) {
			return;
		}
		if (unk != 129) {
			VisibleObject staticObject = player.getKnownList().getKnownObjects().get(targetObjId);
			if (staticObject == null || !MathUtil.isIn3dRange(player, staticObject, 10)
					|| staticObject.getObjectTemplate().getTemplateId() != targetTemplateId) {
				return;
			}
		}
		if (craftType == 0 && (!componentsOk || craftCount < 1)) {
			return;
		}
		CraftService.startCrafting(player, recipeId, targetObjId, craftType, craftCount);
	}
}
