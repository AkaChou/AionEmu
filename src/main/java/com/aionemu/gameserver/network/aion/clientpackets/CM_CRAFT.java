package com.aionemu.gameserver.network.aion.clientpackets;

import lombok.extern.slf4j.Slf4j;
import com.aionemu.gameserver.model.gameobjects.VisibleObject;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.AionClientPacket;
import com.aionemu.gameserver.network.aion.AionConnection.State;
import com.aionemu.gameserver.services.craft.CraftService;
import com.aionemu.gameserver.utils.MathUtil;

import java.util.HashMap;
import java.util.Map;
/**
 * 请求制作/合成物品的客户端包。
 * Client packet requesting crafting of an item.
 */
@Slf4j

public class CM_CRAFT extends AionClientPacket {
	private int unk;
	private int targetTemplateId;
	private int recipeId;
	private int targetObjId;
	private int materialsCount;
	private int craftType;
	private final Map<Integer, Long> requestedComponents = new HashMap<>();
	private boolean componentsOk = true;

	/**
	 * 构造客户端包实例。
	 * Constructs a new client packet instance.
	 *
	 * packet opcode
	 * @param state 连接状态 / connection state
	 * @param restStates 其余允许状态 / additional allowed states
	 */
	public CM_CRAFT(int opcode, State state, State... restStates) {
		super(opcode, state, restStates);
	}

	@Override
	protected void readImpl() {
		unk = readC();
		targetTemplateId = readD();
		recipeId = readD();
		targetObjId = readD();
		materialsCount = readH();
		craftType = readC();
		if (craftType == 0) {
			requestedComponents.clear();
			componentsOk = true;
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
		int craftCount = 1;
		if (craftType == 0) {
			if (!componentsOk || (craftCount = CraftService.consumeComponents(player, recipeId, requestedComponents)) < 1) {
				return;
			}
		}
		CraftService.startCrafting(player, recipeId, targetObjId, craftType, craftCount);
	}
}
