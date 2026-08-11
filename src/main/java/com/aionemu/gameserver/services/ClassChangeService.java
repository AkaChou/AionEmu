package com.aionemu.gameserver.services;

import java.sql.Timestamp;
import java.util.Calendar;

import com.aionemu.gameserver.configs.main.CustomConfig;
import com.aionemu.gameserver.model.PlayerClass;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_DIALOG_WINDOW;
import com.aionemu.gameserver.network.aion.serverpackets.SM_QUEST_ACTION;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * 职业转职服务，在简易二转模式下弹出对话框并完成职业切换。
 * Class-change service showing dialogs and applying class switches in simple 2nd-class mode.
 */
public class ClassChangeService {
	/**
	 * 满足等级与起始职业条件时弹出转职对话框。
	 * Shows the class-change dialog when level and starting-class conditions are met.
	 *
	 * @param player 玩家 / player
	 */
	public static void showClassChangeDialog(Player player) {
		if (CustomConfig.ENABLE_SIMPLE_2NDCLASS) {
			PlayerClass playerClass = player.getPlayerClass();
			Race playerRace = player.getRace();
			int questId = questIdForRace(playerRace);
			QuestState questState = questId == 0 ? null : player.getQuestStateList().getQuestState(questId);
			if (questId != 0 && (questState == null || questState.getStatus() != QuestStatus.COMPLETE)
					&& player.getLevel() >= 9 && playerClass.isStartingClass()) {
				if (playerRace == Race.ELYOS) {
					switch (playerClass) {
					case WARRIOR:
						PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(0, 2375, 1006));
						break;
					case SCOUT:
						PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(0, 2716, 1006));
						break;
					case MAGE:
						PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(0, 3057, 1006));
						break;
					case PRIEST:
						PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(0, 3398, 1006));
						break;
					case TECHNIST:
						PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(0, 3739, 1006));
						break;
					case MUSE:
						PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(0, 4080, 1006));
						break;
					default:
						break;
					}
				} else if (playerRace == Race.ASMODIANS) {
					switch (playerClass) {
					case WARRIOR:
						PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(0, 3057, 2008));
						break;
					case SCOUT:
						PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(0, 3398, 2008));
						break;
					case MAGE:
						PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(0, 3739, 2008));
						break;
					case PRIEST:
						PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(0, 4080, 2008));
						break;
					case TECHNIST:
						PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(0, 3569, 2008));
						break;
					case MUSE:
						PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(0, 3910, 2008));
						break;
					default:
						break;
					}
				}
			}
		}
	}

	/**
	 * 根据对话框选项完成职业切换，并奖励经验与完成转职任务。
	 * Applies the class switch from dialog selection and grants exp plus the class-change quest.
	 *
	 * @param player 玩家 / player
	 * @param dialogId 对话框选项 ID / dialog option id
	 */
	public static boolean changeClassToSelection(final Player player, final int questId, final int dialogId) {
		if (!CustomConfig.ENABLE_SIMPLE_2NDCLASS || player == null) {
			return false;
		}
		Race playerRace = player.getRace();
		int expectedQuestId = questIdForRace(playerRace);
		if (questId != expectedQuestId || expectedQuestId == 0) {
			return false;
		}
		QuestState questState = player.getQuestStateList().getQuestState(questId);
		if (questState != null && questState.getStatus() == QuestStatus.COMPLETE) {
			return false;
		}
		PlayerClass targetClass = classForSelection(playerRace, dialogId);
		if (targetClass == null || !trySetClass(player, targetClass)) {
			return false;
		}
		player.getCommonData().addExp(73200, null);
		completeQuest(player, questId);
		return true;
	}

	static int questIdForRace(Race race) {
		return race == Race.ELYOS ? 1006 : race == Race.ASMODIANS ? 2008 : 0;
	}

	static PlayerClass classForSelection(Race race, int dialogId) {
		if (race == Race.ELYOS) {
			return switch (dialogId) {
				case 2376 -> PlayerClass.GLADIATOR;
				case 2461 -> PlayerClass.TEMPLAR;
				case 2717 -> PlayerClass.ASSASSIN;
				case 2802 -> PlayerClass.RANGER;
				case 3058 -> PlayerClass.SORCERER;
				case 3143 -> PlayerClass.SPIRIT_MASTER;
				case 3399 -> PlayerClass.CLERIC;
				case 3484 -> PlayerClass.CHANTER;
				case 3740 -> PlayerClass.AETHERTECH;
				case 3825 -> PlayerClass.GUNSLINGER;
				case 4081 -> PlayerClass.SONGWEAVER;
				default -> null;
			};
		}
		if (race == Race.ASMODIANS) {
			return switch (dialogId) {
				case 3058 -> PlayerClass.GLADIATOR;
				case 3143 -> PlayerClass.TEMPLAR;
				case 3399 -> PlayerClass.ASSASSIN;
				case 3484 -> PlayerClass.RANGER;
				case 3570 -> PlayerClass.AETHERTECH;
				case 3591 -> PlayerClass.GUNSLINGER;
				case 3740 -> PlayerClass.SORCERER;
				case 3825 -> PlayerClass.SPIRIT_MASTER;
				case 3911 -> PlayerClass.SONGWEAVER;
				case 4081 -> PlayerClass.CLERIC;
				case 4166 -> PlayerClass.CHANTER;
				default -> null;
			};
		}
		return null;
	}

	/**
	 * 将指定任务标记为完成。
	 * Marks the given quest as complete for the player.
	 *
	 * 玩家 / player
	 * quest id
	 */
	private static void completeQuest(Player player, int questId) {
		QuestState qs = player.getQuestStateList().getQuestState(questId);
		Calendar calendar = Calendar.getInstance();
		Timestamp timeStamp = new Timestamp(calendar.getTime().getTime());
		if (qs == null) {
			player.getQuestStateList().addQuest(questId, new QuestState(questId, QuestStatus.COMPLETE, 0, 1, null, 0, timeStamp));
			PacketSendUtility.sendPacket(player, new SM_QUEST_ACTION(questId, QuestStatus.COMPLETE.value(), 0));
		} else {
			qs.setStatus(QuestStatus.COMPLETE);
			qs.setCompleteCount(qs.getCompleteCount() + 1);
			PacketSendUtility.sendPacket(player, new SM_QUEST_ACTION(questId, qs.getStatus(), qs.getQuestVars().getQuestVars()));
		}
	}

	/**
	 * 在校验通过后设置玩家职业并升级角色数据。
	 * Sets the player class after validation and upgrades player data.
	 *
	 * 玩家 / player
	 * target class
	 */
	public static void setClass(Player player, PlayerClass playerClass) {
		trySetClass(player, playerClass);
	}

	private static boolean trySetClass(Player player, PlayerClass playerClass) {
		if (validateSwitch(player, playerClass)) {
			player.getCommonData().setPlayerClass(playerClass);
			player.getController().upgradePlayer();
			PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(0, 0, 0));
			return true;
		}
		return false;
	}

	/**
	 * 校验转职条件：等级、是否起始职业、目标是否合法分支。
	 * Validates class switch: level, starting class, and legal branch.
	 *
	 * 玩家 / player
	 * target class
	 *
	 * @return 可转职时为 {@code true} / {@code true} if valid
	 */
	private static boolean validateSwitch(Player player, PlayerClass playerClass) {
		int level = player.getLevel();
		PlayerClass oldClass = player.getPlayerClass();
		if (level < 9) {
			PacketSendUtility.sendMessage(player, "You can only switch class at level 9");
			return false;
		}
		if (!oldClass.isStartingClass()) {
			PacketSendUtility.sendMessage(player, "You already switched class");
			return false;
		}
		boolean valid = isValidClassSwitch(oldClass, playerClass);
		if (!valid) {
			PacketSendUtility.sendMessage(player, "Invalid class switch chosen");
			return false;
		}
		return true;
	}

	static boolean isValidClassSwitch(PlayerClass oldClass, PlayerClass playerClass) {
		return switch (oldClass) {
			case WARRIOR -> playerClass == PlayerClass.GLADIATOR || playerClass == PlayerClass.TEMPLAR;
			case SCOUT -> playerClass == PlayerClass.ASSASSIN || playerClass == PlayerClass.RANGER;
			case MAGE -> playerClass == PlayerClass.SORCERER || playerClass == PlayerClass.SPIRIT_MASTER;
			case PRIEST -> playerClass == PlayerClass.CLERIC || playerClass == PlayerClass.CHANTER;
			case TECHNIST -> playerClass == PlayerClass.GUNSLINGER || playerClass == PlayerClass.AETHERTECH;
			case MUSE -> playerClass == PlayerClass.SONGWEAVER;
			default -> false;
		};
	}
}
