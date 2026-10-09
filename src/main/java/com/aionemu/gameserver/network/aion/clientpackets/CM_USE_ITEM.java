package com.aionemu.gameserver.network.aion.clientpackets;

import com.aionemu.boot.i18n.I18n;
import com.aionemu.gameserver.configs.main.LoggingConfig;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import com.aionemu.gameserver.network.aion.serverpackets.SM_ITEM_USAGE_ANIMATION;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.questEngine.handlers.HandlerResult;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.restrictions.RestrictionsManager;
import com.aionemu.gameserver.services.toypet.MinionService;
import com.aionemu.gameserver.skillengine.model.Skill;
import com.aionemu.gameserver.skillengine.model.Skill.SkillMethod;
import com.aionemu.gameserver.services.teleport.TeleportService2;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * 客户端使用物品请求包；按 type 分支处理取消、目标物、副本重置、多回城与染色等。
 * Client packet for using an item; branches by type for cancel, target item, instance reset, multi-return, dye, etc.
 */
@Slf4j
public class CM_USE_ITEM extends AionClientPacket {
	/** 任务追踪日志出口，路由到 logback 的 quest logger。 / Quest trace sink for logback quest logger. */
	private static final Logger QUEST_TRACE_LOG = LoggerFactory.getLogger("quest");

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
		if ((LoggingConfig.LOG_QUEST_TRACE || player.isQuestTraceEnabled()) && item != null) {
			QUEST_TRACE_LOG.info(I18n.get("log.quest_trace.use_item",
				player.getName(),
				item.getItemId(),
				uniqueItemId));
		}
		Item targetItem = player.getInventory().getItemByObjId(targetItemId);
		HouseObject<?> targetHouseObject = null;
		if (item == null) {
			// 客户端在关闭契约窗口等场景会发来解析不到物品的使用请求；按取消处理并回取消动画，
			// 客户端才会退出「使用物品」动作状态（对齐 AL-Game 的 item==null → cancelUseItem 分支
			// 与原版 C_USE_ITEM(objId=0) → User::CancelUseItem 语义）。
			// The client may send a use request that no longer resolves (e.g. when closing the
			// contract window); treat it as a cancel and answer with the cancel animation so the
			// client leaves its "using item" action state (AL-Game's item==null → cancelUseItem
			// branch; retail C_USE_ITEM with objId=0 routes to User::CancelUseItem).
			respondCanceledUse(player, uniqueItemId, 0);
			return;
		}
		if (MinionService.isMinionContract(item)) {
			// 守护灵契约书的契约动作由 CM_MINIONS(action=0) 驱动；客户端对同一物品的使用收尾请求
			// 只回取消动画，不在此重复触发契约（原版该物品是物品动作，客户端以使用请求收尾）。
			// The minion contract itself is driven by CM_MINIONS(action=0); an item-use wind-up
			// request for the same item only gets the cancel animation back so the client's
			// "using item" action ends, without triggering a duplicate contract (retail models
			// this item as an item action).
			respondCanceledUse(player, item.getObjectId(), item.getItemTemplate().getTemplateId());
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
		if (TeleportService2.isAbyssEntryWorld(item.getItemTemplate().getReturnWorldId())
				&& !TeleportService2.meetsAbyssEntryRequirement(player)) {
			PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_CANNOT_TELEPORT_TO_ABYSS);
			return;
		}
		if (TeleportService2.isBalaureaEntryWorld(item.getItemTemplate().getReturnWorldId())
				&& !TeleportService2.meetsBalaureaEntryRequirement(player, item.getItemTemplate().getReturnWorldId())) {
			PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_CANNOT_MOVE_TO_AIRPORT_NEED_FINISH_QUEST);
			return;
		}
		if (TeleportService2.isKahrunEntryWorld(item.getItemTemplate().getReturnWorldId())
				&& !TeleportService2.meetsKahrunEntryRequirement(player)) {
			PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_CANNOT_MOVE_TO_AIRPORT_NEED_FINISH_QUEST);
			return;
		}
		if (TeleportService2.isArchDaevaEntryWorld(item.getItemTemplate().getReturnWorldId())
				&& !TeleportService2.meetsArchDaevaEntryRequirement(player,
						item.getItemTemplate().getReturnWorldId())) {
			PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_CANNOT_MOVE_TO_AIRPORT_NEED_FINISH_QUEST);
			return;
		}
		HandlerResult result = GameEngineServices.questEngine().onItemUseEvent(new QuestEnv(null, player, 0, 0), item);
		if (result == HandlerResult.FAILED) {
			return;
		}
		ItemActions itemActions = item.getItemTemplate().getActions();
		ArrayList<AbstractItemAction> actions = new ArrayList<>();
		if (itemActions == null) {
			return;
		}
		for (AbstractItemAction itemAction : itemActions.getItemActions()) {
			// 放入冷却列表前检查物品是否可用。 / check if the item can be used before placing it on the cooldown list.
			if (targetHouseObject != null && itemAction instanceof HouseDyeAction action) {
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
		if (type == 6) {
			for (AbstractItemAction itemAction : actions) {
				if (itemAction instanceof MultiReturnAction action && !action.canAct(player, returnId)) {
					return;
				}
			}
		}
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
			if (targetHouseObject != null && itemAction instanceof HouseDyeAction action) {
				action.act(player, item, targetHouseObject);
			} else if (type == 5) {
				if (itemAction instanceof InstanceTimeClear action) {
					int SelectedSyncId = syncId;
					action.act(player, item, SelectedSyncId);
				}
			} else if (type == 6) {
				if (itemAction instanceof MultiReturnAction action) {
					int SelectedMapIndex = returnId;
					action.act(player, item, SelectedMapIndex);
				}
			} else if (type == 7) {
				if (itemAction instanceof DyeAction action) {
					action.act(player, item, targetItem, customDyeColor);
				}
			} else {
				itemAction.act(player, item, targetItem);
			}
		}
	}

	/**
	 * 回应对客户端使用/取消收尾请求的取消动画：已有挂起使用任务时由该任务自身的结束动画收尾，
	 * 否则补发取消动画（result=3）并清除「使用中物品」状态，客户端才会退出「使用物品」动作。
	 * Answers a client use/cancel wind-up request with the cancel animation: a pending use task
	 * already ends through its own animation, otherwise the cancel animation (result=3) is
	 * broadcast and the "using item" state is cleared so the client leaves its use action.
	 * @param player 玩家 / player
	 * @param itemObjId 请求中的物品对象 ID / item object id from the request
	 * @param itemId 请求中的物品模板 ID（未知时传 0）/ item template id from the request (0 when unknown)
	 */
	private void respondCanceledUse(Player player, int itemObjId, int itemId) {
		if (player.getController().hasTask(TaskId.ITEM_USE)) {
			// 使用/契约进行中：其结束动画会自行下发。 / A use is in flight; its own end animation follows.
			return;
		}
		Item usingItem = player.getUsingItem();
		player.getController().cancelUseItem();
		if (usingItem != null) {
			// 以服务端记录的「使用中物品」为准回取消动画。 / Prefer the server-side "using item" when present.
			itemObjId = usingItem.getObjectId();
			itemId = usingItem.getItemTemplate().getTemplateId();
		}
		PacketSendUtility.broadcastPacket(player, new SM_ITEM_USAGE_ANIMATION(player.getObjectId(),
				itemObjId, itemId, 0, 3, 0), true);
	}
}
