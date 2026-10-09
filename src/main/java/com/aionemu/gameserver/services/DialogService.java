package com.aionemu.gameserver.services;

import com.aionemu.gameserver.ai.ActionItemNpcAI2;
import com.aionemu.gameserver.ai2.AI2;
import com.aionemu.gameserver.ai2.event.AIEventType;
import com.aionemu.gameserver.configs.main.CustomConfig;
import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.lifecycle.GameCoreGameplayServices;
import com.aionemu.gameserver.lifecycle.GameCraftServices;
import com.aionemu.gameserver.lifecycle.GameEngineServices;
import com.aionemu.gameserver.lifecycle.GameHousingServices;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.TeleportAnimation;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.VisibleObject;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.gameobjects.player.PlayerHouseOwnerFlags;
import com.aionemu.gameserver.model.gameobjects.player.RequestResponseHandler;
import com.aionemu.gameserver.model.team.legion.Legion;
import com.aionemu.gameserver.model.team.legion.LegionWarehouse;
import com.aionemu.gameserver.model.templates.portal.PortalPath;
import com.aionemu.gameserver.model.templates.teleport.TeleportLocation;
import com.aionemu.gameserver.model.templates.teleport.TeleporterTemplate;
import com.aionemu.gameserver.model.templates.tradelist.TradeListTemplate;
import com.aionemu.gameserver.model.templates.tradelist.TradeNpcType;
import com.aionemu.gameserver.network.aion.serverpackets.SM_ATTACK_STATUS.TYPE;
import com.aionemu.gameserver.network.aion.serverpackets.SM_DIALOG_WINDOW;
import com.aionemu.gameserver.network.aion.serverpackets.SM_PET;
import com.aionemu.gameserver.network.aion.serverpackets.SM_PLASTIC_SURGERY;
import com.aionemu.gameserver.network.aion.serverpackets.SM_QUESTION_WINDOW;
import com.aionemu.gameserver.network.aion.serverpackets.SM_REPURCHASE;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SELL_ITEM;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.network.aion.serverpackets.SM_TRADELIST;
import com.aionemu.gameserver.network.aion.serverpackets.SM_TRADE_IN_LIST;
import com.aionemu.gameserver.questEngine.definition.QuestDialogAction;
import com.aionemu.gameserver.questEngine.definition.QuestDialogPage;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.restrictions.RestrictionsManager;
import com.aionemu.gameserver.services.craft.RelinquishCraftStatus;
import com.aionemu.gameserver.services.item.ItemChargeService;
import com.aionemu.gameserver.services.teleport.PortalService;
import com.aionemu.gameserver.services.teleport.TeleportService2;
import com.aionemu.gameserver.services.trade.PricesService;
import com.aionemu.gameserver.skillengine.model.SkillTargetSlot;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.stats.AbyssRankEnum;
import lombok.extern.slf4j.Slf4j;

/**
 * NPC 对话框服务：根据 dialogId 分发商店、传送、仓库、任务、整容等交互。
 * NPC dialog service: dispatches shop, teleport, warehouse, quest, plastic-surgery and other interactions by dialogId.
 */
@Slf4j
public class DialogService {

	/**
	 * 变性手术入场券道具 ID 列表 / Gender switch ticket item IDs.
	 */
	private static final int[] GENDER_SWITCH_TICKETS = {
		169660000, 169660001, 169660002, 169660003, 169660004, 169660005
	};

	/**
	 * 整形手术入场券道具 ID 列表 / Plastic surgery ticket item IDs.
	 */
	private static final int[] PLASTIC_SURGERY_TICKETS = {
		169650000, 169650001, 169650002, 169650003, 169650004, 169650005,
		169650006, 169650007, 169650008, 169650009, 169650010, 169650011,
		169691000
	};

	/**
	 * 服务端收尾关窗：原版 {@code 0x5d8} 关窗不仅关客户端窗口，也结束 NPC 的对话态——按客户端
	 * {@code CM_CLOSE_DIALOG} 的同一条收尾链补发 {@link AIEventType#DIALOG_FINISH} 并做既有清扫。
	 * <p>
	 * 不补这一步时，行进中的对话 NPC 会**停在半路永不恢复**：开门时
	 * {@code TalkEventHandler.onSimpleTalk} 置 {@code AISubState.TALK} 并把玩家设为目标，移动 tick 随即
	 * 把该子状态判为「已到达」（{@code AbstractAI#isDestinationReached}）并 {@code abortMove()}，
	 * 而恢复巡逻的唯一常规入口就是 {@code DIALOG_FINISH}（2026-10-06 实机 NPC 203111 Spiros）。
	 * 重复触发安全：目标的清除使第二次 {@code onFinishTalk} 自然成为空操作。
	 * <p>
	 * Server-side dialog close also ends the NPC's talk state (retail {@code 0x5d8}): fire
	 * {@code DIALOG_FINISH} exactly like the client {@code CM_CLOSE_DIALOG} tail does, otherwise a
	 * walking dialog NPC stays paused mid-route forever. Firing twice is safe: the second
	 * {@code onFinishTalk} no-ops once the target is cleared.
	 *
	 * @param npc    对话 NPC，未知时传 null（只关窗） / the dialog NPC, or null when unknown
	 * @param player 玩家 / player
	 */
	public static void closeDialog(Npc npc, Player player) {
		if (npc != null) {
			AI2 ai = npc.getAi2IfPresent();
			if (ai != null) {
				ai.onCreatureEvent(AIEventType.DIALOG_FINISH, player);
			}
			onCloseDialog(npc, player);
		}
		PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(0, 0));
	}

	/**
	 * 服务端收尾关窗（按交互目标对象 id 解析 NPC）：解析不到时退化为纯关窗。
	 * Server-side close resolving the NPC from the interaction target object id; an unresolvable id
	 * degrades to a plain window close.
	 *
	 * @param player         玩家 / player
	 * @param targetObjectId 交互目标对象 ID，0 表示无 / interaction target object id, 0 for none
	 */
	public static void closeDialog(Player player, int targetObjectId) {
		VisibleObject target = targetObjectId > 0 && player.getKnownList() != null
			? player.getKnownList().getObject(targetObjectId) : null;
		closeDialog(target instanceof Npc npc ? npc : null, player);
	}

	/**
	 * 关闭与 NPC 的对话框；对军团仓库管理员释放占用。
	 * Close the NPC dialog; for legion warehouse keepers, release the warehouse user lock.
	 *
	 * @param npc    对话 NPC / dialog NPC
	 * @param player 玩家 / player
	 */
	public static void onCloseDialog(Npc npc, Player player) {
		switch (npc.getObjectTemplate().getTitleId()) {
			case 350409, 358046, 358047, 358048, 358049, 463212 -> {
				PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(npc.getObjectId(), 0));
				Legion legion = player.getLegion();
				if (legion != null) {
					LegionWarehouse lwh = legion.getLegionWarehouse();
					if (lwh.getWhUser() == player.getObjectId()) {
						lwh.setWhUser(0);
					}
				}
			}
			case 314362, 314365, 314366, 350410, 469968, 469977, 469982, 469991,
				 470033, 470042, 470047, 470056, 469969, 469976, 469983, 469990,
				 470034, 470041, 470048, 470055, 350419, 357007, 357019, 357031,
				 357043, 358063, 358493, 358523, 370111, 370112, 370243, 370503,
				 462878, 466226 -> PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(npc.getObjectId(), 0));
			default -> {
			}
		}
	}

	/**
	 * 打开烙印之石窗口（页 1）：开窗前重发进阶烙印槽位数。
	 * Opens the stigma window (page 1); the advanced stigma slot count is re-announced first.
	 *
	 * @param player         玩家 / player
	 * @param targetObjectId 对话目标对象 ID / dialog target object id
	 */
	private static void openStigmaWindow(Player player, int targetObjectId) {
		StigmaService.refreshStigmaSlots(player);
		PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(targetObjectId, 1));
	}

	/**
	 * 处理玩家选择的对话框选项（商店、传送、仓库、任务奖励等）。
	 * Handle a player-selected dialog option (shop, teleport, warehouse, quest rewards, etc.).
	 *
	 * @param dialogId            对话框选项 ID / dialog option id
	 * @param player              玩家 / player
	 * @param npc                 对话 NPC / dialog NPC
	 * @param questId             关联任务 ID，0 表示无 / related quest id, 0 if none
	 * @param extendedRewardIndex 扩展奖励索引 / extended reward index
	 */
	public static void onDialogSelect(int dialogId, final Player player, Npc npc, int questId, int extendedRewardIndex) {
		onDialogSelect(dialogId, player, npc, questId, extendedRewardIndex, true);
	}

	/**
	 * 处理客户端通用选择页上的 NPC 简单对话选项；该入口不再尝试任务路由。
	 * Handles an NPC simple-dialog option from the client's generic selection page without attempting quest routing.
	 *
	 * @param dialogId            对话框选项 ID / dialog option id
	 * @param player              玩家 / player
	 * @param npc                 对话 NPC / dialog NPC
	 * @param extendedRewardIndex 扩展奖励索引 / extended reward index
	 */
	public static void onSimpleDialogSelect(int dialogId, final Player player, Npc npc, int extendedRewardIndex) {
		onDialogSelect(dialogId, player, npc, 0, extendedRewardIndex, false);
	}

	private static void onDialogSelect(int dialogId, final Player player, Npc npc, int questId,
									   int extendedRewardIndex, boolean dispatchQuest) {
		if (dispatchQuest) {
			QuestEnv env = new QuestEnv(npc, player, questId, dialogId);
			env.setExtendedRewardIndex(extendedRewardIndex);
			if (GameEngineServices.questEngine().onDialog(env)) {
				return;
			}
			if (questId != 0 && dialogId != QuestDialogAction.QUEST_SELECT.id()
				&& !QuestDialogPage.isSelectionSubPage(dialogId)) {
				player.clearNpcQuestDialogSelection();
				closeDialog(npc, player);
				return;
			}
		}
		if (player.getAccessLevel() >= 0 && CustomConfig.ENABLE_SHOW_DIALOG_ID) {
			PacketSendUtility.sendMessage(player, "<Quest Id>" + questId);
			PacketSendUtility.sendMessage(player, "<Dialog Id>" + dialogId);
		}
		int targetObjectId = npc.getObjectId();
		int titleId = npc.getObjectTemplate().getTitleId();
		switch (dialogId) {
			case 2 -> {
				// 购买物品。 / Buy Item's.
				TradeListTemplate tradeListTemplate = DataManager.TRADE_LIST_DATA.getTradeListTemplate(npc.getNpcId());
				if (tradeListTemplate == null) {
					PacketSendUtility.sendMessage(player, "Buy <List> is missing !!");
					break;
				}
				if (player.getInventory().isFull()) {
					PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_EXCHANGE_TOO_HEAVY_TO_TRADE);
					return;
				}
				int requiredOfficerItem = getRequiredAdministrationOfficerItem(npc.getNpcId());
				if (requiredOfficerItem != 0 && player.getInventory().getItemCountByItemId(requiredOfficerItem) == 0) {
					PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(targetObjectId, 27));
					return;
				}
				int requiredLevel = getRequiredAbyssTradeLevel(npc.getNpcId());
				if (requiredLevel > 0 && player.getLevel() < requiredLevel) {
					PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(targetObjectId, 27));
					return;
				}
				int tradeModifier = tradeListTemplate.getSellPriceRate();
				PacketSendUtility.sendPacket(player, new SM_TRADELIST(player, npc, tradeListTemplate, PricesService.getVendorBuyModifier() * tradeModifier / 100));
				if (tradeListTemplate.getTradeNpcType() == TradeNpcType.ABYSS) {
					PacketSendUtility.sendPacket(player, new SM_QUESTION_WINDOW(SM_QUESTION_WINDOW.STR_MSGBOX_BUY_RANKITEM_WITH_RANKDOWN_CONFIRM, 0, 0));
				} else {
					PacketSendUtility.sendPacket(player, new SM_TRADELIST(player, npc, tradeListTemplate, PricesService.getVendorBuyModifier() * tradeModifier / 100));
				}
			}
			case 3 -> {
				// 出售物品。 / Sell Item's.
				int level = player.getLevel();
				switch (npc.getNpcId()) {
					case 798509, 798510, 799224 -> {
						if (level < 45) {
							PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(targetObjectId, 27));
							return;
						}
					}
				}
				PacketSendUtility.sendPacket(player, new SM_SELL_ITEM(targetObjectId, PricesService.getVendorSellModifier(player.getRace())));
			}
			case 4 -> handleStigmaDialog(player, targetObjectId, titleId, false);
			case 5 -> PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(targetObjectId, 2));
			case 6 -> GameCoreGameplayServices.legionService().requestDisbandLegion(npc, player);
			case 7 -> GameCoreGameplayServices.legionService().recreateLegion(npc, player);
			case 26 -> {
				// 仓库。 / Warehouse.
				if (player.getLevel() < 10) {
					PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_FREE_EXPERIENCE_CHARACTER_CANT_USE_WAREHOUSE("10"));
					return;
				}
				if (!RestrictionsManager.canUseWarehouse(player)) {
					return;
				}
				PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(targetObjectId, 26));
				WarehouseService.sendWarehouseInfo(player, true);
			}
			case 31 -> {
				// 任务。 / Quest.
				int pageId = 0;
				if (questId != 0) {
					pageId = 10;
					QuestState qs = player.getQuestStateList().getQuestState(questId);
					if (qs != null && (qs.getStatus() == QuestStatus.START || qs.getStatus() == QuestStatus.REWARD)
						&& npc.getAi2() instanceof ActionItemNpcAI2) {
						pageId = 0;
					}
				}
				PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(targetObjectId, pageId));
			}
			case 33 -> {
				// 交易经纪人。 / Trade Broker.
				if (player.getLevel() < 10) {
					PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_FREE_EXPERIENCE_CHARACTER_CANT_USE_VENDOR("10"));
					return;
				}
				PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(targetObjectId, 13));
			}
			case 35 -> handleSoulHealing(player, npc, false);
			case 36 -> {
				// 竞技场城市传送者。 / Arena City Teleporter.
				int level = player.getLevel();
				switch (npc.getNpcId()) {
					case 204089 ->
						TeleportService2.teleportTo(player, 120010000, 984.000f, 1543.000f, 222.100f, (byte) 0);
					case 203764 ->
						TeleportService2.teleportTo(player, 110010000, 1462.500f, 1326.100f, 564.100f, (byte) 0);
					case 203981 -> {
						if (player.getRace() == Race.ELYOS) {
							QuestState qs = player.getQuestStateList().getQuestState(1346);
							if (qs == null || qs.getStatus() != QuestStatus.COMPLETE) {
								PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_HOUSING_CANT_OWN_NOT_COMPLETE_QUEST(1346));
								PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(targetObjectId, 27));
							} else {
								TeleportService2.teleportTo(player, 210020000, 439.300f, 422.200f, 274.300f, (byte) 0);
							}
						}
					}
					case 730048 -> {
						if (level < 27) {
							PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(targetObjectId, 27));
						}
						if (player.getRace() == Race.ELYOS) {
							TeleportService2.teleportTo(player, 210020000, 1343.200f, 350.900f, 348.600f, (byte) 0);
						} else {
							TeleportService2.teleportTo(player, 220020000, 1592.100f, 977.200f, 140.700f, (byte) 0);
						}
					}
					case 730049 -> {
						if (level < 35) {
							PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(targetObjectId, 27));
							PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_CANT_INSTANCE_ENTER_LEVEL);
						} else {
							TeleportService2.teleportTo(player, 220020000, 2434.500f, 553.600f, 334.600f, (byte) 0);
						}
					}
					case 730052 -> {
						if (level < 30) {
							PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(targetObjectId, 27));
							PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_CANT_INSTANCE_ENTER_LEVEL);
						} else {
							TeleportService2.teleportTo(player, 220020000, 1252.900f, 266.000f, 534.200f, (byte) 0);
						}
					}
					case 730053 -> {
						if (level < 30) {
							PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(targetObjectId, 27));
						} else {
							TeleportService2.teleportTo(player, 220020000, 1243.500f, 263.900f, 531.300f, (byte) 0);
						}
					}
				}
			}
			case 37 -> {
				// 竞技场城市传送者。 / Arena City Teleporter.
				switch (npc.getNpcId()) {
					case 204087 ->
						TeleportService2.teleportTo(player, 120010000, 1005.100f, 1528.900f, 222.100f, (byte) 0);
					case 203875 ->
						TeleportService2.teleportTo(player, 110010000, 1470.300f, 1343.500f, 563.700f, (byte) 0);
					case 203982 ->
						TeleportService2.teleportTo(player, 210020000, 446.200f, 431.100f, 274.500f, (byte) 0);
				}
			}
			case 42 -> PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(targetObjectId, 20));
			case 43 -> PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(targetObjectId, 19));
			case 44 -> {
				// 飞行与传送。 / Flight & Teleport.
				if (CustomConfig.ENABLE_SIMPLE_2NDCLASS) {
					TeleportService2.showMap(player, targetObjectId, npc.getNpcId());
				} else {
					int level = player.getLevel();
					switch (npc.getNpcId()) {
						case 730040 -> {
							if (level < 45) {
								PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(targetObjectId, 27));
								PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_CANT_INSTANCE_ENTER_LEVEL);
							} else {
								TeleportService2.teleportTo(player, 210040000, 2599.130f, 2182.7583f, 154.74075f, (byte) 27);
							}
						}
						case 800245, 804561, 804562 -> {
							if (level < 55) {
								PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(targetObjectId, 27));
								PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_CANT_INSTANCE_ENTER_LEVEL);
							} else {
								TeleportService2.showMap(player, targetObjectId, npc.getNpcId());
							}
						}
						case 805748, 805773 -> {
							if (level < 66) {
								PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(targetObjectId, 27));
								PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_CANT_INSTANCE_ENTER_LEVEL);
							} else {
								TeleportService2.showMap(player, targetObjectId, npc.getNpcId());
							}
						}
						case 805750, 805775 -> {
							if (level < 66) {
								PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(targetObjectId, 1011));
								PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_CANT_INSTANCE_ENTER_LEVEL);
							} else {
								TeleportService2.showMap(player, targetObjectId, npc.getNpcId());
							}
						}
						default -> TeleportService2.showMap(player, targetObjectId, npc.getNpcId());
					}
				}
			}
			case 45, 46 -> {
				// 学习制作与提取。 / Learn Craft & Extraction.
				if (player.getLevel() < 10) {
					PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_CRAFT_MSG_CAN_WORK_ONLY_DEVA);
					return;
				}
				GameCraftServices.craftSkillUpdateService().learnSkill(player, npc);
			}
			case 47 -> CubeExpandService.expandCube(player, npc);
			case 48 -> WarehouseService.expandWarehouse(player, npc);
			case 53 -> {
				// 军团仓库。 / Legion Warehouse.
				if (player.getLevel() < 10) {
					PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_FREE_EXPERIENCE_CHARACTER_CANT_USE_GUILD_WAREHOUSE("10"));
					return;
				}
				if (player.getLegion() == null) {
					PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_GUILD_LEAVE_I_AM_NOT_BELONG_TO_GUILD);
					return;
				}
				GameCoreGameplayServices.legionService().openLegionWarehouse(player, npc);
			}
			case 56, 107, 127 -> {
				// 关闭军团仓库(56) / 折价升级(107) / 任务黄金奖励(127)：保持空操作。
				// Close legion warehouse(56) / trade in upgrade(107) / quest gold reward(127): keep no-op.
			}
			case 58 -> PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(targetObjectId, 28));
			case 59 -> PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(targetObjectId, 3));
			case 61, 62 -> {
				// 整形与变性手术。 / Plastic surgery & gender switch.
				byte changesex = (byte) (dialogId == 62 ? 1 : 0);
				int[] tickets = dialogId == 62 ? GENDER_SWITCH_TICKETS : PLASTIC_SURGERY_TICKETS;
				byte checkTicket = (byte) (hasAnyItem(player, tickets) ? 1 : 2);
				PacketSendUtility.sendPacket(player, new SM_PLASTIC_SURGERY(player, checkTicket, changesex));
				player.setEditMode(true);
			}
			case 66 -> PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(targetObjectId, 29));
			case 67 -> PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(targetObjectId, 30));
			case 68 -> player.getNpcFactions().enterGuild(npc);
			case 69 -> player.getNpcFactions().leaveNpcFaction(npc);
			case 70 -> PacketSendUtility.sendPacket(player, new SM_REPURCHASE(player, npc.getObjectId()));
			case 71 -> PacketSendUtility.sendPacket(player, new SM_PET(6));
			case 72 -> PacketSendUtility.sendPacket(player, new SM_PET(7));
			case 73 -> PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(targetObjectId, 32));
			case 74 -> PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(targetObjectId, 33));
			case 75 -> PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(targetObjectId, 35));
			case 76 -> ItemChargeService.startChargingEquippedItems(player, targetObjectId, 1);
			case 78 -> {
				// 资讯模组购买/交换。 / News Mod Buy/Exchange.
				TradeListTemplate tradeListTemplate = DataManager.TRADE_LIST_DATA.getTradeInListTemplate(npc.getNpcId());
				if (tradeListTemplate == null) {
					PacketSendUtility.sendMessage(player, "Buy <Trade In List> is missing !!");
					break;
				}
				PacketSendUtility.sendPacket(player, new SM_TRADE_IN_LIST(npc, tradeListTemplate, 100));
			}
			case 79 -> {
				GameCraftServices.relinquishCraftStatus();
				RelinquishCraftStatus.relinquishExpertStatus(player, npc);
			}
			case 80 -> {
				GameCraftServices.relinquishCraftStatus();
				RelinquishCraftStatus.relinquishMasterStatus(player, npc);
			}
			case 84 -> {
				// 买卖房屋。 / Sell & Buy House.
				if ((player.getBuildingOwnerStates() & PlayerHouseOwnerFlags.BIDDING_ALLOWED.getId()) == 0) {
					int questIdToReport = player.getRace() == Race.ELYOS ? 18802 : 28802;
					PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_HOUSING_CANT_OWN_NOT_COMPLETE_QUEST(questIdToReport));
					return;
				}
				PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(targetObjectId, 38));
			}
			case 92 -> PacketSendUtility.sendPacket(player, new SM_PET(16));
			case 93 -> PacketSendUtility.sendPacket(player, new SM_PET(17));
			case 94 -> PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(targetObjectId, 42));
			case 95 -> ItemChargeService.startChargingEquippedItems(player, targetObjectId, 2);
			case 96 -> GameHousingServices.housingService().recreatePlayerStudio(player);
			case 100 -> PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(targetObjectId, 43));
			case 103 -> {
				// 购买列表。 / Purchase List.
				TradeListTemplate tradeListTemplate = DataManager.TRADE_LIST_DATA.getPurchaseListTemplate(npc.getNpcId());
				if (tradeListTemplate == null) {
					PacketSendUtility.sendMessage(player, "Buy <Purchase List> is missing !!");
					break;
				}
				switch (npc.getNpcId()) {
					case 805976, 806012 -> {
						if (player.getLevel() < 66) {
							PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(targetObjectId, 27));
							return;
						}
					}
				}
				int tradeModifier = tradeListTemplate.getApBuyPriceRate() / 10;
				PacketSendUtility.sendPacket(player, new SM_SELL_ITEM(targetObjectId, tradeModifier, tradeListTemplate));
			}
			case 104 -> handleSimpleTeleport(player, npc, targetObjectId);
			case 106 -> PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(targetObjectId, 51));
			case 109 -> PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(targetObjectId, 52));
			case 125 -> handleStigmaDialog(player, targetObjectId, titleId, true);
			case 126 -> PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(targetObjectId, 55));
			case 128 -> handleSoulHealing(player, npc, true);
			case 10000, 10001, 10002, 10003, 10004, 10005, 10006, 10007, 10008, 10009,
				 10010, 10011, 10012, 10013, 20006, 20007, 20008, 20009, 20010 -> {
				if (questId == 0) {
					TeleporterTemplate template = DataManager.TELEPORTER_DATA.getTeleporterTemplateByNpcId(npc.getNpcId());
					PortalPath portalPath = DataManager.PORTAL2_DATA.getPortalDialog(npc.getNpcId(), dialogId, player.getRace());
					if (portalPath != null) {
						PortalService.port(portalPath, player, targetObjectId);
					} else if (template != null) {
						TeleportLocation loc = template.getTeleLocIdData().getTelelocations().getFirst();
						if (loc != null) {
							TeleportService2.teleport(template, loc.getLocId(), player, npc,
								npc.getAi2().getName().equals("general") ? TeleportAnimation.JUMP_ANIMATION : TeleportAnimation.BEAM_ANIMATION);
						}
					}
				} else {
					PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(targetObjectId, dialogId, questId));
				}
			}
			default -> {
				if (dialogId == 23 && player.getInventory().isFull()) {
					PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(targetObjectId, 0));
				} else {
					PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(targetObjectId, dialogId));
				}
			}
		}
	}

	/**
	 * 检查玩家背包是否拥有指定道具列表中的任意一种。
	 * Checks whether the player's inventory contains at least one item from the given ID list.
	 *
	 * @param player  玩家 / player
	 * @param itemIds 道具 ID 数组 / item ID array
	 * @return 拥有至少一种时返回 true / true if player has at least one matching item
	 */
	private static boolean hasAnyItem(Player player, int[] itemIds) {
		for (int itemId : itemIds) {
			if (player.getInventory().getItemCountByItemId(itemId) > 0) {
				return true;
			}
		}
		return false;
	}

	/**
	 * 判定称号 ID 是否属于烙印之石大师。
	 * Checks whether the given title ID belongs to a Stigma Master.
	 *
	 * @param titleId   称号 ID / title ID
	 * @param isEnchant 是否为强化模式 / whether this is enchant mode
	 * @return 匹配时返回 true / true if matches stigma master title
	 */
	private static boolean isStigmaMasterTitle(int titleId, boolean isEnchant) {
		return switch (titleId) {
			case 314362, 314365, 314366, 350410, 357007, 357019, 357031, 357043,
				 358493, 358523, 370111, 370112 -> true;
			case 370503 -> !isEnchant;
			default -> false;
		};
	}

	/**
	 * 处理烙印之石开启与强化对话。
	 * Handles stigma opening and enchanting dialog interactions.
	 *
	 * @param player         玩家 / player
	 * @param targetObjectId 对话目标对象 ID / dialog target object ID
	 * @param titleId        NPC 称号 ID / NPC title ID
	 * @param isEnchant      是否为强化窗口 / whether this is enchant window
	 */
	private static void handleStigmaDialog(Player player, int targetObjectId, int titleId, boolean isEnchant) {
		if (!isStigmaMasterTitle(titleId, isEnchant)) {
			return;
		}
		int requiredQuestId = switch (player.getRace()) {
			case ELYOS -> 1929;
			case ASMODIANS -> 2900;
			default -> 0;
		};
		if (requiredQuestId == 0) {
			return;
		}
		QuestState qs = player.getQuestStateList().getQuestState(requiredQuestId);
		if (qs == null || qs.getStatus() != QuestStatus.COMPLETE) {
			PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_HOUSING_CANT_OWN_NOT_COMPLETE_QUEST(requiredQuestId));
			PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(targetObjectId, 27));
		} else if (isEnchant) {
			PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(targetObjectId, 53));
		} else {
			openStigmaWindow(player, targetObjectId);
		}
	}

	/**
	 * 获取各行政官 NPC 所需验证的道具 ID。
	 * Returns the required item ID for administration officer NPCs.
	 *
	 * @param npcId NPC ID
	 * @return 所需道具 ID，非行政官 NPC 返回 0 / required item ID, or 0 if none
	 */
	private static int getRequiredAdministrationOfficerItem(int npcId) {
		return switch (npcId) {
			case 203179, 203330 -> 186000001; // 钢铁战斗行政官 天族
			case 203659, 203689 -> 186000006; // 钢铁战斗行政官 魔族
			case 203931, 798111 -> 186000002; // 青铜战斗行政官 天族
			case 204360, 204368, 204425, 204426 -> 186000007; // 青铜战斗行政官 魔族
			case 203964, 204011, 204515, 204571, 204637, 204639, 798112, 798113, 801158, 801161 ->
				186000003; // 白银行政官 天族
			case 204359, 204427, 204781, 204789, 801170, 801173 -> 186000008; // 白银行政官 魔族
			case 204548, 204638, 801164, 801167, 801182, 801185 -> 186000004; // 黄金行政官 天族
			case 204761, 204762, 204766, 204767, 801179, 801196, 801199 -> 186000009; // 黄金行政官 魔族
			case 798172, 798173, 798174, 798175, 798195, 798196, 798197, 798198, 801188, 801192 ->
				186000005; // 白金行政官 天族
			case 205160, 205161, 205162, 205163, 205183, 205184, 205185, 205186, 801202, 801206 ->
				186000010; // 白金行政官 魔族
			case 798914, 798915, 798916, 798917 -> 186000018; // 秘银行政官 天族
			case 799213, 799214, 799215, 799216 -> 186000019; // 秘银行政官 魔族
			case 805136, 805137, 805138, 805139 -> 186000100; // 雪花石膏教团管家 天族
			case 805047, 805048, 805049, 805050 -> 186000103; // 战地看守管家 魔族
			case 805140, 805141, 805142 -> 186000102; // 幸运者教团管家 天族
			case 805051, 805052, 805053, 805054 -> 186000105; // 查里伦克的恶魔管家 魔族
			case 805144 -> 186000101; // 光辉行动管家 天族
			case 805055 -> 186000104; // 血十字军管家 魔族
			case 833744, 833745, 833746, 833747, 833748, 833749, 833750, 833751 -> 186000237; // 大师钱币学家
			default -> 0;
		};
	}

	/**
	 * 获取欧比斯商人 NPC 所需的最低等级。
	 * Returns the minimum level required for Abyss trader NPCs.
	 *
	 * @param npcId NPC ID
	 * @return 最低等级限制，无限制时返回 0 / minimum required level, or 0 if none
	 */
	private static int getRequiredAbyssTradeLevel(int npcId) {
		return switch (npcId) {
			case 203708, 203710 -> 25;
			case 798505, 798509, 798510, 799224, 802210, 805130 -> 45;
			case 798507, 798508 -> 50;
			case 798506 -> 55;
			default -> 0;
		};
	}

	/**
	 * 处理普通灵魂治疗与神圣灵魂治疗。
	 * Handles regular and divine soul healing.
	 *
	 * @param player   玩家 / player
	 * @param npc      对话 NPC / dialog NPC
	 * @param isDivine 是否为神圣灵魂治疗 / whether divine soul healing
	 */
	private static void handleSoulHealing(Player player, Npc npc, boolean isDivine) {
		final long expLost = player.getCommonData().getExpRecoverable();
		if (expLost == 0) {
			player.getEffectController().removeAbnormalEffectsByTargetSlot(SkillTargetSlot.SPEC2);
			player.getCommonData().setDeathCount(0);
		}
		final double factor = (expLost < 1000000 ? 0.25 - (0.00000015 * expLost) : 0.1);
		final int price = (int) (expLost * factor);
		RequestResponseHandler responseHandler = new RequestResponseHandler(npc) {
			@Override
			public void acceptRequest(Creature requester, Player responder) {
				if (player.getInventory().getKinah() >= price) {
					PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_GET_EXP2(expLost));
					PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_SUCCESS_RECOVER_EXPERIENCE);
					player.getCommonData().resetRecoverableExp();
					if (isDivine) {
						player.getLifeStats().increaseHp(TYPE.HP, player.getLifeStats().getMaxHp() + 1);
						player.getLifeStats().increaseMp(TYPE.MP, player.getLifeStats().getMaxMp() + 1);
						player.getLifeStats().increaseFp(TYPE.AUTO_HEAL_FP, player.getLifeStats().getMaxFp() + 1);
					}
					player.getInventory().decreaseKinah(price);
					player.getEffectController().removeAbnormalEffectsByTargetSlot(SkillTargetSlot.SPEC2);
					player.getCommonData().setDeathCount(0);
				} else {
					PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_NOT_ENOUGH_KINA(price));
				}
			}

			@Override
			public void denyRequest(Creature requester, Player responder) {
			}
		};
		if (player.getCommonData().getExpRecoverable() > 0) {
			boolean result = player.getResponseRequester().putRequest(SM_QUESTION_WINDOW.STR_ASK_RECOVER_EXPERIENCE, responseHandler);
			if (result) {
				PacketSendUtility.sendPacket(player, new SM_QUESTION_WINDOW(SM_QUESTION_WINDOW.STR_ASK_RECOVER_EXPERIENCE, 0, 0, String.valueOf(price)));
			}
		} else {
			PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_DONOT_HAVE_RECOVER_EXPERIENCE);
		}
	}

	/**
	 * 处理简单传送（成名之路与军团领地传送）。
	 * Handles simple teleportation (Walk of Fame and Legion Area entries).
	 *
	 * @param player         玩家 / player
	 * @param npc            对话 NPC / dialog NPC
	 * @param targetObjectId 目标对象 ID / target object ID
	 */
	private static void handleSimpleTeleport(Player player, Npc npc, int targetObjectId) {
		if (player.getRace() == Race.ELYOS) {
			switch (npc.getNpcId()) {
				case 802437 -> {
					if (player.getAbyssRank().getRank().getId() < AbyssRankEnum.GENERAL.getId()) {
						PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(targetObjectId, 27));
						return;
					}
					TeleportService2.teleportTo(player, 110070000, 503.80746f, 417.2141f, 126.789635f, (byte) 68);
				}
				case 805162, 805164, 805166, 805168, 805170, 805172 -> {
					if (player.getLegion() == null) {
						PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_GUILD_LEAVE_I_AM_NOT_BELONG_TO_GUILD);
						return;
					}
					switch (npc.getNpcId()) {
						case 805162 ->
							TeleportService2.teleportTo(player, 210070000, 1326.1046f, 640.8399f, 584.82385f, (byte) 26);
						case 805164 ->
							TeleportService2.teleportTo(player, 210070000, 1378.3633f, 655.79205f, 584.82385f, (byte) 54);
						case 805166 ->
							TeleportService2.teleportTo(player, 210070000, 1228.2413f, 1628.6283f, 470.35565f, (byte) 100);
						case 805168 ->
							TeleportService2.teleportTo(player, 210070000, 1218.8335f, 1576.2424f, 470.35565f, (byte) 14);
						case 805170 ->
							TeleportService2.teleportTo(player, 210070000, 2327.8035f, 1535.8595f, 440.83572f, (byte) 17);
						case 805172 ->
							TeleportService2.teleportTo(player, 210070000, 2381.8564f, 1549.5217f, 440.83572f, (byte) 51);
					}
				}
			}
		} else {
			switch (npc.getNpcId()) {
				case 802439 -> {
					if (player.getAbyssRank().getRank().getId() < AbyssRankEnum.GENERAL.getId()) {
						PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(targetObjectId, 27));
						return;
					}
					TeleportService2.teleportTo(player, 120080000, 385.92166f, 251.25146f, 93.129425f, (byte) 24);
				}
				case 805174, 805176, 805178, 805180, 805182, 805184 -> {
					if (player.getLegion() == null) {
						PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_GUILD_LEAVE_I_AM_NOT_BELONG_TO_GUILD);
						return;
					}
					switch (npc.getNpcId()) {
						case 805174 ->
							TeleportService2.teleportTo(player, 220080000, 1769.7422f, 2596.248f, 301.87747f, (byte) 12);
						case 805176 ->
							TeleportService2.teleportTo(player, 220080000, 1810.1752f, 2559.1487f, 301.87747f, (byte) 25);
						case 805178 ->
							TeleportService2.teleportTo(player, 220080000, 1435.401f, 1742.898f, 332.56546f, (byte) 38);
						case 805180 ->
							TeleportService2.teleportTo(player, 220080000, 1466.7686f, 1787.5085f, 332.56546f, (byte) 55);
						case 805182 ->
							TeleportService2.teleportTo(player, 220080000, 768.7018f, 1316.1646f, 254.92737f, (byte) 1);
						case 805184 ->
							TeleportService2.teleportTo(player, 220080000, 803.8293f, 1274.7317f, 254.92737f, (byte) 18);
					}
				}
			}
		}
	}
}
