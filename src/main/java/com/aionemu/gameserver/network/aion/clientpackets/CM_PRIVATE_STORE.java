package com.aionemu.gameserver.network.aion.clientpackets;

import com.aionemu.gameserver.model.DescriptionId;
import com.aionemu.gameserver.model.actions.PlayerMode;
import com.aionemu.gameserver.model.gameobjects.Item;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.trade.TradePSItem;
import com.aionemu.gameserver.network.aion.AionClientPacket;
import com.aionemu.gameserver.network.aion.AionConnection.State;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.services.PrivateStoreService;
import com.aionemu.gameserver.skillengine.effect.AbnormalState;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * 客户端个人商店开店/上架物品请求包。
 * Client packet to open a private store and list items for sale.
 *
 * @author Simple
 */
public class CM_PRIVATE_STORE extends AionClientPacket {

	/**
	 * 个人商店信息 / Private store information
	 */
	private Player activePlayer;
	private TradePSItem[] tradePSItems;
	private int itemCount;
	private boolean cancelStore;

	/**
	 * packet opcode
	 * @param state 连接状态 / connection state
	 * @param restStates 其余允许状态 / additional allowed states
	 */
	public CM_PRIVATE_STORE(int opcode, State state, State... restStates) {
		super(opcode, state, restStates);
	}

	@Override
	protected void readImpl() {
		/**
	 * 定义谁 wants 到创建 private 商店。 / Define who wants to create a private store
	 */
		activePlayer = getConnection().getActivePlayer();
		int level = activePlayer.getLevel();
		if (activePlayer == null) {
			return;
		}
		if (activePlayer.isInPrison()) {
			cancelStore = true;
			PacketSendUtility.sendMessage(activePlayer, "You can't open Private Shop in prison!");
			return;
		}
		if (level < 10) {
			// 试用账号且等级低于 10 不能开设个人商店。 / Characters under level 10 who are using a free trial cannot open a private
			// 存储 / store
			PacketSendUtility.sendPacket(activePlayer,
					SM_SYSTEM_MESSAGE.STR_FREE_EXPERIENCE_CHARACTER_CANT_OPEN_PERSONAL_SHOP("10"));
			return;
		}
		if (activePlayer.getController().isInCombat()) {
			// 战斗中无法开设个人商店。 / You cannot open a private store while fighting
			PacketSendUtility.sendPacket(activePlayer, SM_SYSTEM_MESSAGE.STR_PERSONAL_SHOP_DISABLED_IN_EXCHANGE);
			// 战斗中无法开设个人商店，商店将关闭。 / As you cannot open a private store while fighting, it will be closed
			// 自动 / automatically
			PacketSendUtility.sendPacket(activePlayer, SM_SYSTEM_MESSAGE.STR_PERSONAL_SHOP_CLOSED_FOR_COMBAT_MODE);
			PrivateStoreService.closePrivateStore(activePlayer);
			return;
		}
		if ((activePlayer.isFlying()) || (activePlayer.isUsingFlyTeleport())
				|| (activePlayer.isInPlayerMode(PlayerMode.WINDSTREAM))) {
			// 飞行中无法开设个人商店。 / You cannot open a private store while flying
			PacketSendUtility.sendPacket(activePlayer, SM_SYSTEM_MESSAGE.STR_PERSONAL_SHOP_DISABLED_IN_FLY_MODE);
			return;
		}
		if (activePlayer.isInPlayerMode(PlayerMode.RIDE)) {
			// 骑乘中无法开设个人商店。 / You cannot open a private store while mounted
			PacketSendUtility.sendPacket(activePlayer, SM_SYSTEM_MESSAGE.STR_MSG_PERSONAL_SHOP_RESTRICTION_RIDE);
			return;
		}
		if (activePlayer.getEffectController().isAbnormalSet(AbnormalState.HIDE)) {
			// 隐身中无法开设个人商店。 / You cannot open a private store while hiding
			PacketSendUtility.sendPacket(activePlayer, SM_SYSTEM_MESSAGE.STR_PERSONAL_SHOP_DISABLED_IN_HIDDEN_MODE);
			// 因处于隐身状态，个人商店已自动关闭。 / Your private store closed automatically because you are currently hiding
			PacketSendUtility.sendPacket(activePlayer, SM_SYSTEM_MESSAGE.STR_PERSONAL_SHOP_CLOSED_FOR_HIDDEN_MODE);
			PrivateStoreService.closePrivateStore(activePlayer);
			return;
		}
		itemCount = readH();
		tradePSItems = new TradePSItem[itemCount];

		if (activePlayer.getMoveController().isInMove()) {
			// 在移动物体上无法开设个人商店。 / You cannot open the private store on a moving object
			PacketSendUtility.sendPacket(activePlayer, SM_SYSTEM_MESSAGE.STR_PERSONAL_SHOP_DISABLED_IN_MOVING_OBJECT);
			cancelStore = true;
			return;
		}

		for (int i = 0; i < itemCount; i++) {
			int itemObjId = readD();
			int itemId = readD();
			int count = readH();
			long price = readD();
			readD();// unk 4.7
			Item item = activePlayer.getInventory().getItemByObjId(itemObjId);
			if ((price < 0 || item == null || item.getItemId() != itemId || item.getItemCount() < count)
					&& !cancelStore) {
				PacketSendUtility.sendMessage(activePlayer, "Invalid item.");
				cancelStore = true;
			} else if (!item.isTradeable(activePlayer)) {
				PacketSendUtility.sendPacket(activePlayer,
						new SM_SYSTEM_MESSAGE(1300344, new DescriptionId(item.getNameId())));
				cancelStore = true;
			}

			tradePSItems[i] = new TradePSItem(itemObjId, itemId, count, price);
		}
	}

	@Override
	protected void runImpl() {
		if (activePlayer == null) {
			return;
		}
		if (activePlayer.getLifeStats().isAlreadyDead()) {
			return;
		}
		if (!cancelStore && itemCount > 0) {
			PrivateStoreService.addItems(activePlayer, tradePSItems);
		} else {
			PrivateStoreService.closePrivateStore(activePlayer);
		}
	}
}
