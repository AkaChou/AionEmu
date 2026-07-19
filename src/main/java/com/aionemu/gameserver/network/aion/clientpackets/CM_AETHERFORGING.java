package com.aionemu.gameserver.network.aion.clientpackets;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.AionClientPacket;
import com.aionemu.gameserver.network.aion.AionConnection.State;
import com.aionemu.gameserver.network.aion.serverpackets.SM_AETHERFORGING_PLAYER;
import com.aionemu.gameserver.services.craft.CraftService;
import com.aionemu.gameserver.utils.PacketSendUtility;

import java.util.HashMap;
import java.util.Map;

/**
 * 奥德锻造：开始或停止制作的客户端包。
 * Client packet to start or stop aetherforging craft.
 *
 * @author Ranastic
 */
public class CM_AETHERFORGING extends AionClientPacket {
	private int actionId;
	private int targetTemplateId;
	private int recipeId;
	private int targetObjId;
	private int materialsCount;
	private int craftType;
	private final Map<Integer, Long> requestedComponents = new HashMap<>();
	private boolean componentsOk = true;

	public CM_AETHERFORGING(int opcode, State state, State... restStates) {
		super(opcode, state, restStates);
	}

	@Override
	protected void readImpl() {
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
			requestedComponents.clear();
			componentsOk = true;
			targetTemplateId = readD();
			recipeId = readD();
			targetObjId = readD();
			materialsCount = readH();
			craftType = readC();
			for (int i = 0; i < materialsCount; i++) {
				int itemId = readD();
				long itemCount = readQ();
				if (itemCount < 1) {
					componentsOk = false;
					continue;
				}
				try {
					requestedComponents.merge(itemId, itemCount, Math::addExact);
				} catch (ArithmeticException e) {
					componentsOk = false;
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
			int craftCount;
			if (!componentsOk || (craftCount = CraftService.consumeComponents(player, recipeId, requestedComponents)) < 1) {
				return;
			}
			CraftService.startAetherforging(player, recipeId, craftType, craftCount);
			PacketSendUtility.sendPacket(player, new SM_AETHERFORGING_PLAYER(player, actionId));
			break;
		}
	}
}
