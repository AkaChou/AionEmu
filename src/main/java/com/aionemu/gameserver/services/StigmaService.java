package com.aionemu.gameserver.services;


import com.aionemu.boot.i18n.I18n;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List;

import com.aionemu.gameserver.configs.main.LoggingConfig;
import com.aionemu.gameserver.configs.main.MembershipConfig;
import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.lifecycle.GameEngineServices;
import com.aionemu.gameserver.model.DescriptionId;
import com.aionemu.gameserver.model.DialogPage;
import com.aionemu.gameserver.model.gameobjects.Item;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.gameobjects.player.QuestStateList;
import com.aionemu.gameserver.model.items.ItemSlot;
import com.aionemu.gameserver.model.skill.PlayerSkillEntry;
import com.aionemu.gameserver.model.templates.item.RequireSkill;
import com.aionemu.gameserver.model.templates.item.Stigma;
import com.aionemu.gameserver.network.aion.serverpackets.SM_CUBE_UPDATE;
import com.aionemu.gameserver.network.aion.serverpackets.SM_INVENTORY_UPDATE_ITEM;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SKILL_LIST;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.questEngine.definition.QuestMetadata;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.skillengine.model.SkillLearnTemplate;
import com.aionemu.gameserver.skillengine.model.SkillTemplate;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.audit.AuditLogger;

/**
 * 烙印之石（Stigma）服务，处理装备/卸下烙印之石、技能授予、套装强化与登录校验。
 * Stigma service handling equip/unequip, skill grants, set enchant bonuses, and login validation.
 * @author Wnkrz (Encom)
 */
@Slf4j
public class StigmaService {
	/** 烙印槽位追踪日志出口，与任务追踪同路由到 logback 的 quest logger。 / Stigma-slot trace sink, routed to the logback quest logger. */
	private static final Logger STIGMA_TRACE_LOG = LoggerFactory.getLogger("quest");

	/**
	 * 按品质返回烙印装备消耗基纳。
	 * Returns the kinah cost to equip a stigma by item quality.
	 * @param item 烙印道具 / stigma item
	 * price
	 */
	private static int getPriceByQuality(Item item) {
		int price = 0;
		switch (item.getItemTemplate().getItemQuality()) {
		case RARE:
			price = 35312;
			break;
		case LEGEND:
			price = 70625;
			break;
		case UNIQUE:
			price = 141250;
			break;
		default:
			break;
		}
		return price;
	}

	/**
	 * 装备烙印时校验槽位/职业/基纳，授予对应技能并检查连结与套装。
	 * On stigma equip, validates slot/class/kinah, grants skills, and checks linked skills and set bonuses.
	 * 玩家 / player
	 * @param resultItem 装备的烙印 / equipped stigma item
	 * @param slot 装备槽位 / equipment slot
	 * whether successful
	 */
	public static boolean notifyEquipAction(final Player player, Item resultItem, long slot) {
		if (resultItem.getItemTemplate().isStigma()) {
			if (ItemSlot.isRegularStigma(slot)) {
				if (getPossibleStigmaCount(player) <= player.getEquipment().getEquippedItemsRegularStigma().size()) {
					AuditLogger.info(player, "Possible client hack stigma count big :O");
					return false;
				}
			}
			if (!resultItem.getItemTemplate().isClassSpecific(player.getCommonData().getPlayerClass())) {
				AuditLogger.info(player, "Possible client hack not valid for class.");
				return false;
			}
			Stigma stigmaInfo = resultItem.getItemTemplate().getStigma();
			if (stigmaInfo == null) {
				log.warn(I18n.get("log.eb1508f439a1", resultItem.getItemTemplate().getTemplateId()));
				return false;
			}
			if (player.getInventory().getKinah() < getPriceByQuality(resultItem)) {
				PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_STIGMA_NOT_ENOUGH_MONEY);
				return false;
			} else {
				player.getInventory().decreaseKinah(getPriceByQuality(resultItem));
			}
			for (int i = 1; i <= player.getLevel(); i++) {
				SkillLearnTemplate[] skillTemplates = DataManager.SKILL_TREE_DATA
						.getTemplatesFor(player.getPlayerClass(), i, player.getRace());
				for (SkillLearnTemplate skillTree : skillTemplates) {
					if (resultItem.getSkillGroup().equals(skillTree.getSkillGroup())) {
						// PacketSendUtility.sendPacket(player, new SM_SYSTEM_MESSAGE(1300401, new
						// DescriptionId(resultItem.getNameId()), skillTree.getSkillLevel() +
						// resultItem.getEnchantLevel()));
						player.getSkillList().addStigmaSkill(player, skillTree.getSkillId(),
								skillTree.getSkillLevel() + resultItem.getEnchantLevel());
						PacketSendUtility.sendPacket(player,
								new SM_SKILL_LIST(player, player.getSkillList().getStigmaSkills()));
					}
				}
			}
			List<Integer> sStigma = player.getEquipment().getEquippedItemsAllStigmaIds();
			sStigma.add(resultItem.getItemId());
			StigmaLinkedService.checkEquipConditions(player, sStigma);
			checkStigmaEnchant(player, sStigma);
			if (player.getStigmaSet() != 0) {
				addStigmaSetEnchant(player, resultItem.getEnchantLevel());
			}
		}
		return true;
	}

	/**
	 * 卸下烙印时移除技能、连结技能与套装加成。
	 * On stigma unequip, removes skills, linked skills, and set bonuses as needed.
	 * @param player 玩家 / player
	 * @param resultItem 卸下的烙印 / unequipped stigma item
	 * @return 是否允许卸下 / whether unequip is allowed
	 */
	public static boolean notifyUnequipAction(Player player, Item resultItem) {
		return notifyUnequipAction(player, resultItem, true);
	}

	/** Applies unequip side effects without opening an independent stigma-list transaction. */
	public static boolean notifyUnequipActionInTransaction(Player player, Item resultItem) {
		return notifyUnequipAction(player, resultItem, false);
	}

	private static boolean notifyUnequipAction(Player player, Item resultItem, boolean storeStigmaListImmediately) {
		if (player.getEquipment().isSlotEquipped(ItemSlot.STIGMA_SPECIAL.getSlotIdMask())
				&& resultItem.getEquipmentSlot() != ItemSlot.STIGMA_SPECIAL.getSlotIdMask()) {
			return false;
		}
		if (player.getStigmaSet() != 0 && player.getEquipment().getEquippedItemsAllStigmaIds().size() == 6) {
			removeStigmaSetEnchant(player);
		}
		if (resultItem.getItemTemplate().isStigma()) {
			int itemId = resultItem.getItemId();
			// 卸下时同样下发按等级/资格计算后的槽位数，不能再回落到存储字段（常规角色恒为 0）。
			// Unequip also pushes the derived slot count instead of the raw stored field, which is 0 for
			// regular characters.
			refreshStigmaSlots(player);
			PacketSendUtility.sendPacket(player, new SM_INVENTORY_UPDATE_ITEM(player, resultItem));
			for (int i = 1; i <= player.getLevel(); i++) {
				SkillLearnTemplate[] skillTemplates = DataManager.SKILL_TREE_DATA
						.getTemplatesFor(player.getPlayerClass(), i, player.getRace());
				for (SkillLearnTemplate skillTree : skillTemplates) {
					if (resultItem.getSkillGroup().equals(skillTree.getSkillGroup())) {
						// PacketSendUtility.sendPacket(player, new SM_SYSTEM_MESSAGE(1300401, new
						// DescriptionId(resultItem.getNameId()), resultItem.getEnchantLevel()));
						player.getSkillList().addStigmaSkill(player, skillTree.getSkillId(), skillTree.getSkillLevel());
						PacketSendUtility.sendPacket(player,
								new SM_SKILL_LIST(player, player.getSkillList().getStigmaSkills()));
						SkillLearnService.removeSkill(player, skillTree.getSkillId());
					}
				}
			}
			// PacketSendUtility.sendPacket(player, new SM_SYSTEM_MESSAGE(1300403, new
			// DescriptionId(resultItem.getNameId())));
			if (storeStigmaListImmediately) {
				player.getEquipedStigmaList().remove(player, itemId);
			} else {
				player.getEquipedStigmaList().removeInTransaction(itemId);
			}
			if (player.getEquipment().getEquippedItemsAllStigma().size() <= 6 && player.getLinkedSkill() != 0) {
				SkillTemplate linked = DataManager.SKILL_DATA.getSkillTemplate(player.getLinkedSkill());
				PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_STIGMA_DELETE_LINKED_SKILL(
						new DescriptionId(DataManager.SKILL_DATA.getSkillTemplate(linked.getSkillId()).getNameId()),
						1));
				StigmaLinkedService.DeleteLinkedSkills(player);
			}
			if (player.getEquipment().getEquippedItemsAllStigma().size() <= 6 && player.getStigmaSet() != 0) {
				player.setStigmaSet(0);
			}
		}
		return true;
	}

	/**
	 * 按套装等级为已有烙印技能叠加强化等级。
	 * Applies set-bonus enchant levels to existing stigma skills.
	 * @param player 玩家 / player
	 * @param enchantLevel 强化等级 / enchant level
	 */
	public static void addStigmaSetEnchant(Player player, int enchantLevel) {
		for (PlayerSkillEntry skill : player.getSkillList().getStigmaSkills()) {
			player.getSkillList().addStigmaSkill(player, skill.getSkillId(), 1 + enchantLevel + player.getStigmaSet());
			PacketSendUtility.sendPacket(player, new SM_SKILL_LIST(player, player.getSkillList().getStigmaSkills()));
		}
	}

	/**
	 * 强化连结技能相关烙印技能等级。
	 * Updates stigma skill levels for linked-skill enchant.
	 * @param player 玩家 / player
	 * @param enchantLevel 强化等级 / enchant level
	 */
	public static void enchanteLinkedSkill(Player player, int enchantLevel) {
		for (PlayerSkillEntry skill : player.getSkillList().getStigmaSkills()) {
			player.getSkillList().addStigmaSkill(player, skill.getSkillId(), 1 + enchantLevel + player.getStigmaSet());
			PacketSendUtility.sendPacket(player, new SM_SKILL_LIST(player, player.getSkillList().getStigmaSkills()));
		}
	}

	/**
	 * 移除套装加成后，按单件强化等级重建烙印技能。
	 * After set-bonus removal, rebuilds stigma skills from per-item enchant levels.
	 * @param player 玩家 / player
	 */
	public static void removeStigmaSetEnchant(Player player) {
		for (Item resultItem : player.getEquipment().getEquippedItemsAllStigma()) {
			for (int i = 1; i <= player.getLevel(); i++) {
				SkillLearnTemplate[] skillTemplates = DataManager.SKILL_TREE_DATA
						.getTemplatesFor(player.getPlayerClass(), i, player.getRace());
				for (SkillLearnTemplate skillTree : skillTemplates) {
					if (resultItem.getSkillGroup().equals(skillTree.getSkillGroup())) {
						player.getSkillList().addStigmaSkill(player, skillTree.getSkillId(),
								skillTree.getSkillLevel() + resultItem.getEnchantLevel());
						PacketSendUtility.sendPacket(player,
								new SM_SKILL_LIST(player, player.getSkillList().getStigmaSkills()));
					}
				}
			}
		}
	}

	/**
	 * 根据 6 件烙印强化等级设置套装加成值。
	 * Sets the stigma set bonus value from the enchant levels of 6 equipped stigmas.
	 * @param player 玩家 / player
	 * @param list 已装备烙印模板 ID 列表 / equipped stigma template ids
	 */
	public static void checkStigmaEnchant(Player player, List<Integer> list) {
		for (Item item : player.getEquipment().getEquippedItemsAllStigma()) {
			if (list.size() >= 6) {
				if (item.getEnchantLevel() == 6) {
					player.setStigmaSet(1);
				} else if (item.getEnchantLevel() == 7) {
					player.setStigmaSet(2);
				} else if (item.getEnchantLevel() == 8) {
					player.setStigmaSet(3);
				} else if (item.getEnchantLevel() == 9) {
					player.setStigmaSet(3);
				} else if (item.getEnchantLevel() >= 10) {
					player.setStigmaSet(5);
				} else {
					player.setStigmaSet(0);
				}
			}
		}
	}

	/**
	 * 登录时重建烙印技能、校验槽位/前置技能/职业，并检查连结条件。
	 * On login, rebuilds stigma skills, validates slots/prereqs/class, and checks linked conditions.
	 * @param player 玩家 / player
	 */
	public static void onPlayerLogin(Player player) {
		List<Item> equippedItems = player.getEquipment().getEquippedItemsAllStigma();
		List<Integer> Stigma = player.getEquipment().getEquippedItemsAllStigmaIds();
		checkStigmaEnchant(player, Stigma);
		for (Item item : equippedItems) {
			for (int i = 1; i <= player.getLevel(); i++) {
				SkillLearnTemplate[] skillTemplates = DataManager.SKILL_TREE_DATA
						.getTemplatesFor(player.getPlayerClass(), i, player.getRace());
				for (SkillLearnTemplate skillTree : skillTemplates) {
					if (item.getItemTemplate().isStigma() && item.getSkillGroup().equals(skillTree.getSkillGroup())) {
						player.getSkillList().addStigmaSkill(player, skillTree.getSkillId(),
								skillTree.getSkillLevel() + item.getEnchantLevel() + player.getStigmaSet());
						PacketSendUtility.sendPacket(player,
								new SM_SKILL_LIST(player, player.getSkillList().getStigmaSkills()));
					}
				}
			}
		}
		for (Item item : equippedItems) {
			if (item.getItemTemplate().isStigma()) {
				if (!isPossibleEquippedStigma(player, item)) {
					AuditLogger.info(player, "Possible client hack stigma count big :O");
					player.getEquipment().unEquipItem(item.getObjectId(), 0);
					continue;
				}
				Stigma stigmaInfo = item.getItemTemplate().getStigma();
				if (stigmaInfo == null) {
					player.getEquipment().unEquipItem(item.getObjectId(), 0);
					continue;
				}
				int needSkill = stigmaInfo.getRequireSkill().size();
				for (RequireSkill rs : stigmaInfo.getRequireSkill()) {
					for (int id : rs.getSkillIds()) {
						if (player.getSkillList().isSkillPresent(id)) {
							needSkill--;
							break;
						}
					}
				}
				if (needSkill != 0) {
					AuditLogger.info(player, "Possible client hack advenced stigma skill.");
					player.getEquipment().unEquipItem(item.getObjectId(), 0);
					continue;
				}
				if (!item.getItemTemplate().isClassSpecific(player.getCommonData().getPlayerClass())) {
					AuditLogger.info(player, "Possible client hack not valid for class.");
					player.getEquipment().unEquipItem(item.getObjectId(), 0);
					continue;
				}
			}
		}
        StigmaLinkedService.checkEquipConditions(player, Stigma);
	}

	/**
	 * 重算并推送可用烙印之石槽位数：计算值高于已存值时按 GM 解锁同语义**持久化**，
	 * 并按「任务开启」向客户端发一次开启通知（真端 {@code STR_MSG_STIGMA_OPEN_SLOT_BY_QUEST} 1402942）。
	 * <p>客户端只在收到槽位数与开启通知后展开窗口槽位；持久化保证重登后登录路径下发的值同样正确，
	 * 通知只在数值首次抬升时发送一次（幂等），不会在每次任务推进时重复刷屏。</p>
	 * Recomputes and pushes the stigma-slot count. When the computed value exceeds the stored one it is
	 * persisted (same semantics as the GM unlock) and the retail "slot opened by quest" notification
	 * (1402942) is sent once, because the client expands its window sockets only after both the count and
	 * the notification arrive. Persisting keeps the value correct for logins too, and the notification is
	 * emitted only when the count first rises, so quest progress cannot spam it.
	 * @param player 玩家 / player
	 */
	public static void refreshStigmaSlots(Player player) {
		if (player == null) {
			return;
		}
		int stored = player.getCommonData().getAdvancedStigmaSlotSize();
		boolean entitled = hasStigmaSlotEntitlement(player);
		int computed = stigmaSlotCount(player.getLevel(),
				player.havePermission(MembershipConfig.STIGMA_SLOT_QUEST), entitled);
		int slots = Math.max(stored, computed);
		if (slots > stored) {
			player.getCommonData().setAdvancedStigmaSlotSize(slots);
			if (entitled) {
				notifySlotOpened(player, stored, slots);
			}
		}
		if (LoggingConfig.LOG_QUEST_TRACE || player.isQuestTraceEnabled()) {
			STIGMA_TRACE_LOG.info(I18n.get("log.stigma_trace.slot_push", player.getName(),
				player.getLevel(), stored, computed, entitled, slots));
		}
		PacketSendUtility.sendPacket(player, SM_CUBE_UPDATE.stigmaSlots(slots));
	}

	/**
	 * 发送「凹槽已开启」通知：真端 1402942（任务开启）+ 1402933（普通槽扩展）。
	 * <p>客户端按凹槽类型/来源分别处理开启通知与槽位数，二者都发才覆盖它的展开判据。</p>
	 * Sends the slot-open notifications: retail 1402942 (opened by quest) and 1402933 (normal slot
	 * expanded). The client handles the open notification and the slot count per socket type, so both
	 * are required to cover its expansion rule.
	 * @param player 玩家 / player
	 * @param before 开启前的存储值 / stored value before opening
	 * @param after 开启后的存储值 / stored value after opening
	 */
	private static void notifySlotOpened(Player player, int before, int after) {
		PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_STIGMA_OPEN_SLOT_BY_QUEST);
		PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_STIGMA_OPEN_NORMAL_SLOT);
		if (LoggingConfig.LOG_QUEST_TRACE || player.isQuestTraceEnabled()) {
			STIGMA_TRACE_LOG.info(I18n.get("log.stigma_trace.slot_opened", player.getName(), before, after));
		}
	}

	/**
	 * 被任务数据标记「扩展烙印槽」的任务状态提交后：重算推送槽位数并补发真端「任务开启凹槽」通知。
	 * <p>通知在玩家处于世界内、正在与任务/烙印窗口交互时下发，避免只在登录早期发送而丢失；
	 * 仅标记任务会走到这里（同步口已按元数据门控），其余任务不产生任何额外包。</p>
	 * After a quest flagged by the data commits, recompute and push the slot count and send the retail
	 * "slot opened by quest" notification while the player is in-world and interacting with the quest, so
	 * it cannot be lost to the early-login window. Only flagged quests reach here (the sync port gates on
	 * the metadata), so other quests produce no extra packets.
	 * @param player 玩家 / player
	 */
	public static void onStigmaSlotQuestCommitted(Player player) {
		if (player == null) {
			return;
		}
		refreshStigmaSlots(player);
		if (LoggingConfig.LOG_QUEST_TRACE || player.isQuestTraceEnabled()) {
			STIGMA_TRACE_LOG.info(I18n.get("log.stigma_trace.slot_quest_notify", player.getName(),
				player.getCommonData().getAdvancedStigmaSlotSize()));
		}
		PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_STIGMA_OPEN_SLOT_BY_QUEST);
	}

	/**
	 * 按等级、教学任务资格与会员权限计算可用常规烙印槽数量。
	 * Computes the available regular stigma slot count from level, tutorial quest entitlement, and membership.
	 * @param player 玩家 / player
	 * @return 可用槽位数 / available slot count
	 */
	private static int getPossibleStigmaCount(Player player) {
		if (player == null) {
			return 0;
		}
		return stigmaSlotCount(player.getLevel(), player.havePermission(MembershipConfig.STIGMA_SLOT_QUEST),
				hasStigmaSlotEntitlement(player));
	}

	/**
	 * 纯计算：常规烙印槽随等级开启（20/30/40/45/50/55 档），但必须先完成烙印教学任务链。
	 * Pure computation: regular stigma slots open by level (20/30/40/45/50/55 tiers) once the stigma
	 * tutorial quest chain has been entered.
	 * @param level 玩家等级 / player level
	 * @param membershipPerk 是否持有会员烙印槽权限 / whether the membership stigma-slot perk is held
	 * @param entitled 任务数据声明的烙印槽资格 / the entitlement declared by quest data
	 * @return 可用槽位数 / available slot count
	 */
	static int stigmaSlotCount(int level, boolean membershipPerk, boolean entitled) {
		if (level < 20) {
			return 0;
		}
		if (membershipPerk) {
			return 7;
		}
		if (!entitled) {
			return 0;
		}
		if (level < 30) {
			return 2;
		}
		if (level < 40) {
			return 3;
		}
		if (level < 45) {
			return 4;
		}
		if (level < 50) {
			return 5;
		}
		if (level < 55) {
			return 6;
		}
		return 7;
	}

	/**
	 * 判定玩家是否已获得常规烙印槽资格：任一被任务数据标记扩展烙印槽的任务处于进行、待交付或完成态。
	 * <p>资格完全由任务数据（真端 {@code reward_extend_stigma1}）驱动，不依赖任务 ID、步数或结晶道具：
	 * 教学任务进行中即开启槽位，领取结晶后的装备、战斗与报告步都保持开启，否则装备中的烙印会在登录
	 * 校验中被卸下；放弃或未接取时数据不满足，槽位随之关闭。</p>
	 * Whether the player earned the regular stigma slots: any quest flagged by the quest data (retail
	 * {@code reward_extend_stigma1}) is in progress, waiting for hand-in, or complete. The gate is driven by
	 * quest data instead of quest ids, step numbers, or tutorial items: accepting the tutorial opens the slots
	 * and keeps them open through the equip, fight, and report steps, otherwise login validation would unequip
	 * the equipped stigma; abandoning the quest closes them again.
	 * @param player 玩家 / player
	 * @return 是否具备槽位资格 / whether the slots are entitled
	 */
	private static boolean hasStigmaSlotEntitlement(Player player) {
		QuestStateList questStates = player.getQuestStateList();
		if (questStates == null) {
			return false;
		}
		for (QuestState questState : questStates.getAllQuestState()) {
			if (questState == null || !keepsStigmaSlotsOpen(questState.getStatus())) {
				continue;
			}
			QuestMetadata metadata = GameEngineServices.questEngine().questCatalog()
				.findMetadata(questState.getQuestId()).orElse(null);
			if (extendsStigmaSlots(metadata)) {
				return true;
			}
		}
		return false;
	}

	/** 进行、待交付与完成状态都保持槽位开启；未接取与锁定不开启。 / In-progress, waiting-for-hand-in, and complete states keep the slots open; none and locked do not. */
	private static boolean keepsStigmaSlotsOpen(QuestStatus status) {
		return status == QuestStatus.START || status == QuestStatus.REWARD || status == QuestStatus.COMPLETE;
	}

	/**
	 * 元数据是否声明扩展烙印槽位（真端 {@code reward_extend_stigma1}，见定义 XML 的
	 * {@code extend-stigma-slots} 属性）。
	 * Whether the metadata declares a stigma-slot extension (retail {@code reward_extend_stigma1}, the
	 * {@code extend-stigma-slots} attribute of the definition XML).
	 * @param metadata 任务元数据 / quest metadata
	 * @return 是否扩展烙印槽位 / whether the quest extends stigma slots
	 */
	static boolean extendsStigmaSlots(QuestMetadata metadata) {
		return metadata != null && metadata.extendStigmaSlots();
	}

	/**
	 * 按任务 ID 解析「该任务数据是否声明扩展烙印槽」：供对话口在讲这个任务的每一页时重新确立槽位协议事实。
	 * Resolves "does this quest declare the stigma-slot extension" by quest id: the dialog port uses it to
	 * re-establish the slot protocol fact on every page of that quest's conversation.
	 * @param questId 任务 ID / quest id
	 * @return 声明扩展时为 true / true when the extension is declared
	 */
	public static boolean declaresStigmaSlotExtension(int questId) {
		QuestMetadata metadata = GameEngineServices.questEngine().questCatalog()
			.findMetadata(questId).orElse(null);
		return extendsStigmaSlots(metadata);
	}

	/**
	 * 对话页下发前的槽位再通告：重发槽位数；目标页是烙印窗口（页 1）时补发真端「任务开启凹槽」通知。
	 * <p>窗口正是玩家核对凹槽来源的界面，而客户端对「开启通知」与「槽位数」分别处理凹槽展开；
	 * 其余页面只重发槽位数，避免刷提示。</p>
	 * Slot re-announce before a dialog page goes out: the slot count is re-sent, and when the target page
	 * is the stigma window (page 1) the retail "slot opened by quest" notice goes with it. The window is
	 * where the player checks where the socket came from, and the client handles the open notice and the
	 * slot count separately when expanding sockets; other pages only re-send the count to avoid repeats.
	 * @param player 玩家 / player
	 * @param dialogId 目标对话页 ID / target dialog page id
	 */
	public static void reannounceStigmaSlots(Player player, int dialogId) {
		refreshStigmaSlots(player);
		if (dialogId == DialogPage.STIGMA.id()) {
			PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_STIGMA_OPEN_SLOT_BY_QUEST);
		}
	}

	/**
	 * 判断当前已装备烙印是否落在玩家可用槽位范围内。
	 * Returns whether the equipped stigma is within the player's available slot range.
	 * 玩家 / player
	 * @param item 烙印道具 / stigma item
	 * whether valid
	 */
	private static boolean isPossibleEquippedStigma(Player player, Item item) {
		if (player == null || item == null || !item.getItemTemplate().isStigma()) {
			return false;
		}
		long itemSlotToEquip = item.getEquipmentSlot();
		if (ItemSlot.isRegularStigma(itemSlotToEquip)) {
			int stigmaCount = getPossibleStigmaCount(player);
			if (stigmaCount > 0) {
				if (stigmaCount == 1) {
					return itemSlotToEquip == ItemSlot.STIGMA1.getSlotIdMask();
				} else if (stigmaCount == 2) {
					return itemSlotToEquip == ItemSlot.STIGMA1.getSlotIdMask()
						|| itemSlotToEquip == ItemSlot.STIGMA2.getSlotIdMask();
				} else if (stigmaCount == 3) {
					return itemSlotToEquip == ItemSlot.STIGMA1.getSlotIdMask()
						|| itemSlotToEquip == ItemSlot.STIGMA2.getSlotIdMask()
						|| itemSlotToEquip == ItemSlot.STIGMA3.getSlotIdMask();
				} else if (stigmaCount == 4) {
					return itemSlotToEquip == ItemSlot.STIGMA1.getSlotIdMask()
						|| itemSlotToEquip == ItemSlot.STIGMA2.getSlotIdMask()
						|| itemSlotToEquip == ItemSlot.STIGMA3.getSlotIdMask()
						|| itemSlotToEquip == ItemSlot.STIGMA4.getSlotIdMask();
				} else if (stigmaCount == 5) {
					return itemSlotToEquip == ItemSlot.STIGMA1.getSlotIdMask()
						|| itemSlotToEquip == ItemSlot.STIGMA2.getSlotIdMask()
						|| itemSlotToEquip == ItemSlot.STIGMA3.getSlotIdMask()
						|| itemSlotToEquip == ItemSlot.STIGMA4.getSlotIdMask()
						|| itemSlotToEquip == ItemSlot.STIGMA5.getSlotIdMask();
				} else if (stigmaCount == 6) {
					return itemSlotToEquip == ItemSlot.STIGMA1.getSlotIdMask()
						|| itemSlotToEquip == ItemSlot.STIGMA2.getSlotIdMask()
						|| itemSlotToEquip == ItemSlot.STIGMA3.getSlotIdMask()
						|| itemSlotToEquip == ItemSlot.STIGMA4.getSlotIdMask()
						|| itemSlotToEquip == ItemSlot.STIGMA5.getSlotIdMask()
						|| itemSlotToEquip == ItemSlot.STIGMA6.getSlotIdMask();
				} else if (stigmaCount == 7) {
					return itemSlotToEquip == ItemSlot.STIGMA1.getSlotIdMask()
						|| itemSlotToEquip == ItemSlot.STIGMA2.getSlotIdMask()
						|| itemSlotToEquip == ItemSlot.STIGMA3.getSlotIdMask()
						|| itemSlotToEquip == ItemSlot.STIGMA4.getSlotIdMask()
						|| itemSlotToEquip == ItemSlot.STIGMA5.getSlotIdMask()
						|| itemSlotToEquip == ItemSlot.STIGMA6.getSlotIdMask()
						|| itemSlotToEquip == ItemSlot.STIGMA_SPECIAL.getSlotIdMask();
				}
			}
		}
		return false;
	}
}
