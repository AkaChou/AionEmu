package com.aionemu.gameserver.network.aion.clientpackets;

import com.aionemu.boot.i18n.I18n;
import lombok.extern.slf4j.Slf4j;

import com.aionemu.gameserver.model.ChatType;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.AionClientPacket;
import com.aionemu.gameserver.network.aion.AionConnection.State;
import com.aionemu.gameserver.network.aion.serverpackets.SM_LUNA_SHOP_LIST;
import com.aionemu.gameserver.network.aion.serverpackets.SM_MESSAGE;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.services.player.LunaShopService;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * 月之商店（Luna Shop）操作的客户端包。
 * Client packet for Luna Shop operations.
 */
@Slf4j
public class CM_LUNA_SHOP extends AionClientPacket {

	private int actionId;
	private int indun_id;
	private int recipe_id;
	private int material_item_id;
	private long material_item_count;
	private int teleportId;
	private int slot;
	private int ItemObjId;
	/**
	 * 构造该客户端包。
	 * Constructs this client packet.
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
		case 0: // Taki 的任务传送 / Taki's Missions Teleport.
			indun_id = readD();
			readC(); // indun_unk
			break;
		case 2: // Karunerk 的工坊 / Karunerk's Workshop.
			recipe_id = readD();
			break;
		case 4: // 购买必需材料 / Buy Necessary Materials.
			material_item_id = readD();
			material_item_count = readQ();
			break;
		case 6:
		case 7:
			this.teleportId = readD();
			break;
		case 8: // Dorinerk 的衣柜 / Dorinerk's Wardrobe.
			break;
		case 9: // 扩展衣柜栏位 / Expand wardrobe slot
			break;
		case 10: // 应用衣柜外观 / Apply wardrobe appearance
			slot = readC();
			ItemObjId = readD();
			break;
		case 11: // 修改外观 / Modify appearance
			slot = readC();
			ItemObjId = readD();
			readC(); // lunaCost
			break;
		case 12: // 打开宝箱 / Open Chest.
			break;
		case 14: // Taki 的冒险 / Taki's Adventure.
			indun_id = readD();
			break;
		case 15: // 月之骰子游戏 / Luna Dice Game
			break;
		case 16:// 月之骰子游戏奖励 / Luna Dice Game Reward
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
			if (player.getLevel() <= 9) {
				PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_CANT_INSTANCE_ENTER_LEVEL);
				return;
			} else if (player.isInGroup2()) {
				PacketSendUtility.broadcastPacket(player,
						new SM_MESSAGE(player, "You must leave your group or alliance to enter <Luna Instance>",
								ChatType.BRIGHT_YELLOW_CENTER),
						true);
				return;
			} else {
				LunaShopService.getInstance().takiAdventureTeleport(player, indun_id);
			}
			break;
		case 2: // Karunerk 的工坊 / Karunerk's Workshop
			LunaShopService.getInstance().specialDesign(player, recipe_id);
			break;
		case 3:
			LunaShopService.getInstance().craftBox(player);
			break;
		case 4: // 购买必需材料 / Buy Necessary Materials
			LunaShopService.getInstance().buyMaterials(player, material_item_id, material_item_count);
			break;
		case 5:
			PacketSendUtility.sendPacket(player, new SM_LUNA_SHOP_LIST(actionId));
			break;
		case 6:
		case 7:
			LunaShopService.getInstance().teleport(player, actionId, teleportId);
			break;
		case 8:
			LunaShopService.getInstance().dorinerkWardrobeLoad(player);
			break;
		case 9:
			LunaShopService.getInstance().dorinerkWardrobeExtendSlots(player);
			break;
		case 10:
			LunaShopService.getInstance().dorinerkWardrobeAct(player, slot, ItemObjId);
			break;
		case 11:
			LunaShopService.getInstance().dorinerkWardrobeModifyAppearance(player, slot, ItemObjId);
			break;
		case 12:
			LunaShopService.getInstance().munirunerksTreasureChamber(player);
			break;
		case 14:
			LunaShopService.getInstance().takiAdventure(player, indun_id);
			break;
		case 15:
			LunaShopService.getInstance().diceGame(player);
			break;
		case 16:
			LunaShopService.getInstance().diceGameReward(player);
			break;
		default:
			log.warn(I18n.get("log.c40e1a78aad4", player.getObjectId(), actionId));
			break;
		}
	}
}
