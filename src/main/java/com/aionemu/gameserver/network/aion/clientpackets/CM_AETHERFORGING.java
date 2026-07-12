package com.aionemu.gameserver.network.aion.clientpackets;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.AionClientPacket;
import com.aionemu.gameserver.network.aion.AionConnection.State;
import com.aionemu.gameserver.network.aion.serverpackets.SM_AETHERFORGING_PLAYER;
import com.aionemu.gameserver.services.craft.CraftService;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * 奥德锻造：开始或停止制作的客户端包。
 * Client packet to start or stop aetherforging craft.
 *
 * @author Ranastic
 */
public class CM_AETHERFORGING extends AionClientPacket {
	private int itemID;
	@SuppressWarnings("unused")
	private long itemCount;
	private int actionId;
	private int targetTemplateId;
	private int recipeId;
	private int targetObjId;
	private int materialsCount;
	private int craftType;
	private int craftCount = 1;
	private boolean componentsOk = true;

	public CM_AETHERFORGING(int opcode, State state, State... restStates) {
		super(opcode, state, restStates);
	}

	@Override
	protected void readImpl() {
		Player player = getConnection().getActivePlayer();
		actionId = readC();
		switch (actionId) {
		case 0:
			targetTemplateId = readD();
			recipeId = readD();
			targetObjId = readD();
			materialsCount = readH();
			craftType = readC();
			break;
		case 1:
			craftCount = 0;
			componentsOk = true;
			targetTemplateId = readD();
			recipeId = readD();
			targetObjId = readD();
			materialsCount = readH();
			craftType = readC();
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
			break;
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
		switch (actionId) {
		case 0:
			CraftService.stopAetherforging(player, recipeId);
			PacketSendUtility.sendPacket(player, new SM_AETHERFORGING_PLAYER(player, actionId));
			break;
		case 1:
			if (!componentsOk || craftCount < 1) {
				return;
			}
			CraftService.startAetherforging(player, recipeId, craftType, craftCount);
			PacketSendUtility.sendPacket(player, new SM_AETHERFORGING_PLAYER(player, actionId));
			break;
		}
	}
}
