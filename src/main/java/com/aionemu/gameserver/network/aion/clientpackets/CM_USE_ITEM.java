package com.aionemu.gameserver.network.aion.clientpackets;

import com.aionemu.gameserver.lifecycle.GameEngineServices;

import java.util.ArrayList;

import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.TaskId;
import com.aionemu.gameserver.model.gameobjects.HouseObject;
import com.aionemu.gameserver.model.gameobjects.Item;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.templates.item.actions.AbstractItemAction;
import com.aionemu.gameserver.model.templates.item.actions.DyeAction;
import com.aionemu.gameserver.model.templates.item.actions.HouseDyeAction;
import com.aionemu.gameserver.model.templates.item.actions.InstanceTimeClear;
import com.aionemu.gameserver.model.templates.item.actions.ItemActions;
import com.aionemu.gameserver.model.templates.item.actions.MultiReturnAction;
import com.aionemu.gameserver.network.aion.AionClientPacket;
import com.aionemu.gameserver.network.aion.AionConnection.State;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.questEngine.QuestEngine;
import com.aionemu.gameserver.questEngine.handlers.HandlerResult;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.restrictions.RestrictionsManager;
import com.aionemu.gameserver.skillengine.model.Skill;
import com.aionemu.gameserver.skillengine.model.Skill.SkillMethod;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * 客户端使用物品请求包；按 type 分支处理取消、目标物、副本重置、多回城与染色等。
 * Client packet for using an item; branches by type for cancel, target item, instance reset, multi-return, dye, etc.
 */
public class CM_USE_ITEM extends AionClientPacket {
	public int uniqueItemId;
	public int type, targetItemId, syncId, returnId, customDyeColor;

	/**
	 * packet opcode
	 * @param state 连接状态 / connection state
	 * @param restStates 其余允许状态 / additional allowed states
	 */
	public CM_USE_ITEM(int opcode, State state, State... restStates) {
		super(opcode, state, restStates);
	}

	@Override
	protected void readImpl() {
		uniqueItemId = readD();
		type = readC();
		if (type == 2) {
			targetItemId = readD();
		} else if (type == 5) {
			syncId = readD();
		} else if (type == 6) {
			returnId = readD();
		} else if (type == 7) {
			targetItemId = readD();
			customDyeColor = readD();
		}
	}

	@Override
	protected void runImpl() {
		Player player = getConnection().getActivePlayer();
		/**
		 * 5.0 物品使用取消系统 / 5.0 ITEM_USE Cancel System
		 */
		if (type == 0) {
			// Aion 5.8 客户端也会以 type 0 发起独立物品使用；
			// 只有存在活动物品动作时才解释为取消。
			// The Aion 5.8 client also starts standalone items with type 0; treat it as cancellation only while
			// an item action is active.
			Skill castingSkill = player.getCastingSkill();
			boolean hasScheduledItemUse = player.getController().hasTask(TaskId.ITEM_USE);
			boolean hasItemSkillCast = castingSkill != null && castingSkill.getSkillMethod() == SkillMethod.ITEM;
			if (hasScheduledItemUse) {
				player.getController().cancelUseItem();
			}
			if (hasItemSkillCast) {
				player.getController().cancelCurrentSkill(castingSkill);
			}
			if (hasScheduledItemUse || hasItemSkillCast) {
				return;
			}
		}
		if (player.isProtectionActive()) {
			player.getController().stopProtectionActiveTask();
		}
		Item item = player.getInventory().getItemByObjId(uniqueItemId);
		Item targetItem = player.getInventory().getItemByObjId(targetItemId);
		HouseObject<?> targetHouseObject = null;
		if (item == null) {
			return;
		}
		if (targetItem == null) {
			targetItem = player.getEquipment().getEquippedItemByObjId(targetItemId);
		}
		if (targetItem == null && player.getHouseRegistry() != null) {
			targetHouseObject = player.getHouseRegistry().getObjectByObjId(targetItemId);
		}
		if (item.getItemTemplate().getTemplateId() == 165000001 && targetItem.getItemTemplate().canExtract()) {
			PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_ITEM_COLOR_ERROR);
			return;
		}
		// 检查使用物品组播延迟利用施法（刷屏） / check use item multicast delay exploit cast (spam)
		if (player.isCasting()) {
			player.getController().cancelCurrentSkill();
		}
		if (!RestrictionsManager.canUseItem(player, item)) {
			return;
		}
		if (item.getItemTemplate().getRace() != Race.PC_ALL && item.getItemTemplate().getRace() != player.getRace()) {
			PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_CANNOT_USE_ITEM_INVALID_RACE);
			return;
		}
		int requiredLevel = item.getItemTemplate().getRequiredLevel(player.getCommonData().getPlayerClass());
		if (requiredLevel == -1) {
			PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_CANNOT_USE_ITEM_INVALID_CLASS);
			return;
		}
		if (requiredLevel > player.getLevel()) {
			PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE
					.STR_CANNOT_USE_ITEM_TOO_LOW_LEVEL_MUST_BE_THIS_LEVEL(item.getNameId(), requiredLevel));
			return;
		}
		HandlerResult result = GameEngineServices.questEngine().onItemUseEvent(new QuestEnv(null, player, 0, 0), item);
		if (result == HandlerResult.FAILED) {
			return;
		}
		ItemActions itemActions = item.getItemTemplate().getActions();
		ArrayList<AbstractItemAction> actions = new ArrayList<AbstractItemAction>();
		if (itemActions == null) {
			return;
		}
		for (AbstractItemAction itemAction : itemActions.getItemActions()) {
			// 放入冷却列表前检查物品是否可用。 / check if the item can be used before placing it on the cooldown list.
			if (targetHouseObject != null && itemAction instanceof HouseDyeAction) {
				HouseDyeAction action = (HouseDyeAction) itemAction;
				if (action != null && action.canAct(player, item, targetHouseObject)) {
					actions.add(itemAction);
				}
			} else if (itemAction.canAct(player, item, targetItem)) {
				actions.add(itemAction);
			}
		}
		if (actions.size() == 0) {
			PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_ITEM_IS_NOT_USABLE);
			return;
		}
		// 将物品 CD 存于服务端 Player 变量。 / Store Item CD in server Player variable.
		// 防止药水刷屏，以及重登使用 Kisk/奥德果冻/长 CD。 / Prevents potion spamming, and relogging to use kisks/aether jelly/long CD
		// 物品。 / items.
		if (player.isItemUseDisabled(item.getItemTemplate().getUseLimits())) {
			PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_ITEM_CANT_USE_UNTIL_DELAY_TIME);
			return;
		}
		int useDelay = player.getItemCooldown(item.getItemTemplate());
		if (useDelay > 0) {
			player.addItemCoolDown(item.getItemTemplate().getUseLimits().getDelayId(),
					System.currentTimeMillis() + useDelay, useDelay / 1000);
		}
		// 通知物品使用观察者 / notify item use observer
		player.getObserveController().notifyItemuseObservers(item);
		for (AbstractItemAction itemAction : actions) {
			if (targetHouseObject != null && itemAction instanceof HouseDyeAction) {
				HouseDyeAction action = (HouseDyeAction) itemAction;
				action.act(player, item, targetHouseObject);
			} else if (type == 5) {
				if (itemAction instanceof InstanceTimeClear) {
					InstanceTimeClear action = (InstanceTimeClear) itemAction;
					int SelectedSyncId = syncId;
					action.act(player, item, SelectedSyncId);
				}
			} else if (type == 6) {
				if (itemAction instanceof MultiReturnAction) {
					MultiReturnAction action = (MultiReturnAction) itemAction;
					int SelectedMapIndex = returnId;
					action.act(player, item, SelectedMapIndex);
				}
			} else if (type == 7) {
				if (itemAction instanceof DyeAction) {
					DyeAction action = (DyeAction) itemAction;
					action.act(player, item, targetItem, customDyeColor);
				}
			} else {
				itemAction.act(player, item, targetItem);
			}
		}
	}
}
