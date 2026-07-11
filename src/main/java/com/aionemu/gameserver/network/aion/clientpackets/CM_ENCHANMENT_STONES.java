package com.aionemu.gameserver.network.aion.clientpackets;


import com.aionemu.boot.i18n.I18n;
import lombok.extern.slf4j.Slf4j;

import com.aionemu.gameserver.model.gameobjects.Item;
import com.aionemu.gameserver.model.gameobjects.VisibleObject;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.templates.item.ItemCategory;
import com.aionemu.gameserver.model.templates.item.actions.EnchantItemAction;
import com.aionemu.gameserver.model.templates.item.actions.EnchantStigmaAction;
import com.aionemu.gameserver.network.aion.AionClientPacket;
import com.aionemu.gameserver.network.aion.AionConnection.State;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.services.item.ItemSocketService;
import com.aionemu.gameserver.services.trade.PricesService;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * 请求强化石、镶嵌/拆除魔力石、神石或增幅的客户端包。
 * Client packet for enchanting, socketing/removing manastones, godstones, or amplification.
 */
@Slf4j
public class CM_ENCHANMENT_STONES extends AionClientPacket {

	private int npcObjId;
	private int slotNum;
	private int actionType;
	private int targetFusedSlot;
	private int stoneUniqueId;
	private int targetItemUniqueId;
	private int supplementUniqueId;
	@SuppressWarnings("unused")
	private ItemCategory actionCategory;
	@SuppressWarnings("unused")
	private int unk;
	private int toppedItemObjId;

	/**
	 * 构造客户端包实例。
	 * Constructs a new client packet instance.
	 *
	 * packet opcode
	 * @param state 连接状态 / connection state
	 * @param restStates 其余允许状态 / additional allowed states
	 */
	public CM_ENCHANMENT_STONES(int opcode, State state, State... restStates) {
		super(opcode, state, restStates);
	}

	@Override
	protected void readImpl() {
		actionType = readC();
		targetFusedSlot = readC();
		targetItemUniqueId = readD();
		switch (actionType) {
		case 1:
		case 2:
			stoneUniqueId = readD();
			supplementUniqueId = readD();
			break;
		case 3:
			slotNum = readC();
			readC();
			readH();
			npcObjId = readD();
			break;
		case 4:
			stoneUniqueId = readD();
			unk = readD();
			break;
		case 8:
			toppedItemObjId = readD();
			stoneUniqueId = readD();
			break;
		default:
			log.error(I18n.get("log.adb3551cc786", Integer.toHexString(actionType/* !!!!! */).toUpperCase()));
			break;
		}
	}

	@Override
	protected void runImpl() {
		Player player = getConnection().getActivePlayer();
		VisibleObject obj = player.getKnownList().getObject(npcObjId);
		switch (actionType) {
		case 1: // Enchant Stone.
		case 2: // Add Manastone.
			EnchantItemAction action = new EnchantItemAction();
			EnchantStigmaAction action2 = new EnchantStigmaAction();
			Item manastone = player.getInventory().getItemByObjId(stoneUniqueId);
			Item stigmaStone = player.getInventory().getItemByObjId(stoneUniqueId);
			Item targetItem = player.getEquipment().getEquippedItemByObjId(targetItemUniqueId);
			Item targetStone = player.getInventory().getItemByObjId(targetItemUniqueId);
			if (targetItem == null) {
				targetItem = player.getInventory().getItemByObjId(targetItemUniqueId);
			}
			// 强化烙印之石。 / Enchant Stigma.
			if (stigmaStone.getItemTemplate().isStigma() || stigmaStone.getItemTemplate().isEnchantmentStigmaStone()) {
				action2.act(player, stigmaStone, targetItem);
			} else {
				// 强化石。 / Enchant Stone.
				if (action.canAct(player, manastone, targetItem)) {
					Item supplement = player.getInventory().getFirstItemByItemId(supplementUniqueId);
					if (supplement != null) {
						if (supplement.getItemId() / 100000 != 1661) {
							return;
						}
					}
					action.act(player, manastone, targetItem, supplement, targetFusedSlot);
				}
			}
			break;
		case 3: // Remove Manastone.
			long price = PricesService.getPriceForService(17161, player.getRace());
			if (player.getInventory().getKinah() > price) {
				PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_NOT_ENOUGH_KINA(price));
				player.getInventory().decreaseKinah(price);
				if (targetFusedSlot == 1) {
					ItemSocketService.removeManastone(player, targetItemUniqueId, slotNum);
				} else {
					ItemSocketService.removeFusionstone(player, targetItemUniqueId, slotNum);
				}
			}
			break;
		case 4: // Godstone Socket.
			ItemSocketService.socketGodstone(player, targetItemUniqueId, stoneUniqueId);
			break;
		case 8: // Amplification.
			ItemSocketService.amplification(player, targetItemUniqueId, toppedItemObjId, stoneUniqueId);
			break;
		}
	}
}
