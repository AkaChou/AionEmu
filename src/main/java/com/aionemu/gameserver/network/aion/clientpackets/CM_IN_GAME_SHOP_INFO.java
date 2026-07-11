package com.aionemu.gameserver.network.aion.clientpackets;

import com.aionemu.gameserver.lifecycle.GameRuntimeServices;

import com.aionemu.gameserver.configs.main.InGameShopConfig;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.ingameshop.InGameShopEn;
import com.aionemu.gameserver.network.aion.AionClientPacket;
import com.aionemu.gameserver.network.aion.AionConnection.State;
import com.aionemu.gameserver.network.aion.serverpackets.SM_IN_GAME_SHOP_CATEGORY_LIST;
import com.aionemu.gameserver.network.aion.serverpackets.SM_IN_GAME_SHOP_ITEM;
import com.aionemu.gameserver.network.aion.serverpackets.SM_IN_GAME_SHOP_LIST;
import com.aionemu.gameserver.network.aion.serverpackets.SM_TOLL_INFO;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * 游戏内商城浏览/购买/赠送的客户端包。
 * Client packet for in-game shop browse, purchase, or gift actions.
 */
public class CM_IN_GAME_SHOP_INFO extends AionClientPacket {
	private int actionId;
	private int categoryId;
	private int listInCategory;
	private String senderName;
	private String senderMessage;
	/**
	 * 构造该客户端包。
	 * Constructs this client packet.
	 *
	 * packet opcode
	 * @param state 连接状态 / connection state
	 * @param restStates 其余合法状态 / additional valid states
	 */
	public CM_IN_GAME_SHOP_INFO(int opcode, State state, State... restStates) {
		super(opcode, state, restStates);
	}
	/**
	 * 按动作读取商城分类/物品/赠送参数。
	 * Reads shop category/item/gift parameters by action.
	 */
	@Override
	protected void readImpl() {
		actionId = readC();
		categoryId = readD();
		listInCategory = readD();
		senderName = readS();
		senderMessage = readS();
	}
	/**
	 * 返回商城列表/物品信息或处理购买赠送。
	 * Returns shop lists/item info or handles purchase/gift.
	 */
	@Override
	protected void runImpl() {
		if (InGameShopConfig.ENABLE_IN_GAME_SHOP) {
			Player player = getConnection().getActivePlayer();
			switch (actionId) {
			case 0x01:
				PacketSendUtility.sendPacket(player, new SM_IN_GAME_SHOP_ITEM(player, categoryId));
				break;
			case 0x02:
				PacketSendUtility.sendPacket(player, new SM_IN_GAME_SHOP_CATEGORY_LIST(2, categoryId));
				player.inGameShop.setCategory((byte) categoryId);
				break;
			case 0x04:
				PacketSendUtility.sendPacket(player, new SM_IN_GAME_SHOP_CATEGORY_LIST(0, categoryId));
				break;
			case 0x08:
				if (categoryId > 1) {
					player.inGameShop.setSubCategory((byte) categoryId);
				}
				PacketSendUtility.sendPacket(player, new SM_IN_GAME_SHOP_LIST(player, listInCategory, 1));
				PacketSendUtility.sendPacket(player, new SM_IN_GAME_SHOP_LIST(player, listInCategory, 0));
				break;
			case 0x10:
				PacketSendUtility.sendPacket(player,
						new SM_TOLL_INFO(player.getClientConnection().getAccount().getToll()));
				break;
			case 0x20:
				GameRuntimeServices.inGameShopEn().acceptRequest(player, categoryId);
				break;
			case 0x40:
				GameRuntimeServices.inGameShopEn().sendRequest(player, senderName, senderMessage, categoryId);
				break;
			}
		}
	}
}
