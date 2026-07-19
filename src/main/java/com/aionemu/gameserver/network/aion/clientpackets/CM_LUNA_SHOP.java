package com.aionemu.gameserver.network.aion.clientpackets;

import com.aionemu.boot.i18n.I18n;
import lombok.extern.slf4j.Slf4j;

import com.aionemu.gameserver.lifecycle.GameEventBootstrapServices;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.AionClientPacket;
import com.aionemu.gameserver.network.aion.AionConnection.State;
import com.aionemu.gameserver.network.aion.serverpackets.SM_LUNA_SHOP_LIST;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * 月之商店（Luna Shop）操作的客户端包。
 * Client packet for Luna Shop operations.
 */
@Slf4j
public class CM_LUNA_SHOP extends AionClientPacket {

	private int actionId;
	private int indun_id;
	private int indun_unk;
	private int recipe_id;
	private int material_item_id;
	private long material_item_count;
	private int teleportId;
	private int slot;
	private int ItemObjId;
	@SuppressWarnings("unused")
	private int lunaCost;
	/**
	 * 构造该客户端包。
	 * Constructs this client packet.
	 *
	 * packet opcode
	 * @param state 连接状态 / connection state
	 * @param restStates 其余合法状态 / additional valid states
	 */
	public CM_LUNA_SHOP(int opcode, State state, State... restStates) {
		super(opcode, state, restStates);
	}
	/**
	 * 按动作 ID 读取月之商店请求参数。
	 * Reads Luna Shop request parameters by action id.
	 */
	@Override
	protected void readImpl() {
		actionId = readC();
		switch (actionId) {
		case 0: // Taki's Missions Teleport.
			indun_id = readD();
			indun_unk = readC();
			break;
		case 2: // Karunerk's Workshop.
			recipe_id = readD();
			break;
		case 4: // Buy Necessary Materials.
			material_item_id = readD();
			material_item_count = readQ();
			break;
		case 6:
		case 7:
			this.teleportId = readD();
			break;
		case 8: // Dorinerk's Wardrobe.
			break;
		case 9: // Expand wardrobe slot
			break;
		case 10: // Apply wardrobe appearance
			slot = readC();
			ItemObjId = readD();
			break;
		case 11: // Modify appearance
			slot = readC();
			ItemObjId = readD();
			lunaCost = readC();
			break;
		case 12: // Open Chest.
			break;
		case 14: // Taki's Adventure.
			indun_id = readD();
			break;
		case 15: // Luna Dice Game
			break;
		case 16:// Luna Dice Game Reward
			break;
		}
	}
	/**
	 * 执行月之商店购买、合成、传送等动作。
	 * Executes Luna Shop buy, craft, teleport, and related actions.
	 */
	@Override
	protected void runImpl() {
		Player player = getConnection().getActivePlayer();
		if (player == null) {
			return;
		}
		switch (actionId) {
		case 0:
			GameEventBootstrapServices.lunaShopService().takiAdventureTeleport(player, indun_unk, indun_id);
			break;
		case 2: // Karunerk's Workshop
			GameEventBootstrapServices.lunaShopService().specialDesign(player, recipe_id);
			break;
		case 3:
			GameEventBootstrapServices.lunaShopService().craftBox(player);
			break;
		case 4: // Buy Necessary Materials
			GameEventBootstrapServices.lunaShopService().buyMaterials(player, material_item_id, material_item_count);
			break;
		case 5:
			PacketSendUtility.sendPacket(player, new SM_LUNA_SHOP_LIST(actionId));
			break;
		case 6:
		case 7:
			GameEventBootstrapServices.lunaShopService().teleport(player, actionId, teleportId);
			break;
		case 8:
			GameEventBootstrapServices.lunaShopService().dorinerkWardrobeLoad(player);
			break;
		case 9:
			GameEventBootstrapServices.lunaShopService().dorinerkWardrobeExtendSlots(player);
			break;
		case 10:
			GameEventBootstrapServices.lunaShopService().dorinerkWardrobeAct(player, slot, ItemObjId);
			break;
		case 11:
			GameEventBootstrapServices.lunaShopService().dorinerkWardrobeModifyAppearance(player, slot, ItemObjId);
			break;
		case 12:
			GameEventBootstrapServices.lunaShopService().munirunerksTreasureChamber(player);
			break;
		case 14:
			GameEventBootstrapServices.lunaShopService().takiAdventure(player, indun_id);
			break;
		case 15:
			GameEventBootstrapServices.lunaShopService().diceGame(player);
			break;
		case 16:
			GameEventBootstrapServices.lunaShopService().diceGameReward(player);
			break;
		default:
			log.warn(I18n.get("log.c40e1a78aad4", player.getObjectId(), actionId));
			break;
		}
	}
}
