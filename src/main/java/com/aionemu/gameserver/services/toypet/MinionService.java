package com.aionemu.gameserver.services.toypet;


import com.aionemu.boot.i18n.I18n;
import lombok.extern.slf4j.Slf4j;
import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import java.sql.Timestamp;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import org.springframework.beans.factory.ObjectProvider;

import com.aionemu.commons.database.dao.DAOManager;
import com.aionemu.commons.utils.Rnd;
import com.aionemu.gameserver.controllers.MinionController;
import com.aionemu.gameserver.controllers.observer.ItemUseObserver;
import com.aionemu.gameserver.dao.PlayerMinionsDAO;
import com.aionemu.gameserver.dao.PlayerSkillListDAO;
import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.model.DescriptionId;
import com.aionemu.gameserver.model.TaskId;
import com.aionemu.gameserver.model.gameobjects.Item;
import com.aionemu.gameserver.model.gameobjects.Minion;
import com.aionemu.gameserver.model.gameobjects.player.MinionCommonData;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.minion.MinionBuff;
import com.aionemu.gameserver.model.team2.common.legacy.LootRuleType;
import com.aionemu.gameserver.model.templates.item.ItemUseLimits;
import com.aionemu.gameserver.model.templates.item.actions.AbstractItemAction;
import com.aionemu.gameserver.model.templates.item.actions.ItemActions;
import com.aionemu.gameserver.model.templates.minion.MinionEvolved;
import com.aionemu.gameserver.model.templates.minion.MinionSkill;
import com.aionemu.gameserver.model.templates.minion.MinionTemplate;
import com.aionemu.gameserver.network.aion.serverpackets.SM_ITEM_USAGE_ANIMATION;
import com.aionemu.gameserver.network.aion.serverpackets.SM_MINIONS;
import com.aionemu.gameserver.network.aion.serverpackets.SM_QUEST_ACTION;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.restrictions.RestrictionsManager;
import com.aionemu.gameserver.services.SkillLearnService;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.world.knownlist.PlayerAwareKnownList;

/**
 * 守护灵（Minion）服务，管理召唤、成长、进化、组合与功能增益。
 * Minion service managing summon, growth, evolution, combination and functional buffs.
 *
 * <p>Reworked by G-Robson26; Rework &amp; Test: MATTY</p>
 */
@Slf4j
public class MinionService {

	private static final int MAX_SKILL_POINTS = 50000;
	private static final int KINAH_PER_SKILL_POINT = 20;
	private static final int MAX_MINIONS = 200;
	private static volatile ObjectProvider<MinionService> instanceProvider;
	private static List<Integer> minions;
	private MinionBuff minionbuff;

	/**
	 * 初始化守护灵模板缓存与增益对象。
	 * Initialize minion template cache and buff holder.
	 */
	public void init() {
		minions = DataManager.MINION_DATA.getAll();
		minionbuff = new MinionBuff();
		log.info(I18n.get("log.1d620f7162e6"));
	}

	/**
	 * 玩家登录时同步守护灵列表，并尝试恢复上次使用的守护灵。
	 * Sync minion list on login and restore the last-used minion if any.
	 *
	 * @param player 玩家 / Player
	 */
	public void onPlayerLogin(Player player) {
		PacketSendUtility.sendPacket(player, new SM_MINIONS(0, player.getMinionList().getMinions()));
		PacketSendUtility.sendPacket(player, new SM_MINIONS(9, 0));
		PacketSendUtility.sendPacket(player, new SM_MINIONS(11, player.getMinionSkillPoints(),
				player.getCommonData().isMinionSkillPointsAutoCharge()));
		PacketSendUtility.sendPacket(player, new SM_MINIONS(12));
		
		final int lastUsedMinionId = player.getMinionList().getLastUsed();
		if (lastUsedMinionId != 0 && player.getMinion() == null) {
			GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
				@Override
				public void run() {
					if (player.isOnline() && player.getMinion() == null) {
						MinionCommonData minionData = player.getMinionList().getMinion(lastUsedMinionId);
						if (minionData != null) {
							spawnMinion(player, lastUsedMinionId);
						}
					}
				}
			}, 3000);
		}
	}

	/**
	 * 使用契约/票券物品抽取并添加守护灵。
	 * Use a contract/ticket item to roll and add a minion.
	 *
	 * 玩家 / Player
	 * Item object id
	 */
	public void addMinion(final Player player, final int itemObjId) {
		if (rejectIfMinionLimitReached(player)) {
			return;
		}

		final Item item = player.getInventory().getItemByObjId(itemObjId);
		PacketSendUtility.broadcastPacket(player, new SM_ITEM_USAGE_ANIMATION(player.getObjectId(), itemObjId, item.getItemId(), 1500, 0), true);

		final ItemUseObserver itemUseObserver = new ItemUseObserver() {

			@Override
			public void abort() {
				player.getController().cancelTask(TaskId.ITEM_USE);
				player.removeItemCoolDown(item.getItemTemplate().getUseLimits().getDelayId());
				PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_ITEM_CANCELED(new DescriptionId(item.getItemTemplate().getNameId())));
				PacketSendUtility.broadcastPacket(player, new SM_ITEM_USAGE_ANIMATION(player.getObjectId(), itemObjId, item.getItemId(), 0, 2), true);
				player.getObserveController().removeObserver(this);
			}
		};

		player.getObserveController().attach(itemUseObserver);
		player.getController().addTask(TaskId.ITEM_USE, GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {

			@Override
			public void run() {
				player.getObserveController().removeObserver(itemUseObserver);
				player.getController().cancelTask(TaskId.ITEM_USE);
				if (rejectIfMinionLimitReached(player)) {
					PacketSendUtility.broadcastPacket(player,
							new SM_ITEM_USAGE_ANIMATION(player.getObjectId(), itemObjId, item.getItemId(), 0, 2), true);
					return;
				}
				PacketSendUtility.broadcastPacket(player, new SM_ITEM_USAGE_ANIMATION(player.getObjectId(), itemObjId, item.getItemId(), 0, 1), true);

				if (!player.getInventory().decreaseByObjectId(itemObjId, 1)) {
					return;
				}
				int rnd = 0;
				int minionId = 0;
				String grade = "";
				int level = 0;
				String name = "";
				int growthPoint = 0;
				if (!item.getItemTemplate().getMinionTicket()) {
					return;
				}

				MinionTemplate minionTemplate = null;
				if (item.getItemTemplate().isMinionCashContract()) {
					switch (item.getItemTemplate().getTemplateId()) {
						case 190080007:
						case 190080008:
						case 190080013:
							rnd = Rnd.get(0, 1610);
							minionId = minionId(rnd);
							break;

						case 190080006:
						case 190080012:
							rnd = Rnd.get(0, 910);
							minionId = minionId(rnd);
							MinionTemplate mediumTemplate = DataManager.MINION_DATA.getMinionTemplate(minionId);
							if (mediumTemplate != null) {
								String rank = mediumTemplate.getGrade();
								int attempts = 0;
								while (rank.equals("A") && attempts < 50) {
									rnd = Rnd.get(0, 910);
									minionId = minionId(rnd);
									mediumTemplate = DataManager.MINION_DATA.getMinionTemplate(minionId);
									if (mediumTemplate != null) {
										rank = mediumTemplate.getGrade();
									}
									attempts++;
								}
							}
							break;

						case 190080005:
						case 190080009:
						case 190080010:
						case 190080011:
						case 190089999:
							rnd = Rnd.get(0, 210);
							minionId = minionId(rnd);
							MinionTemplate lesserTemplate = DataManager.MINION_DATA.getMinionTemplate(minionId);
							if (lesserTemplate != null) {
								String rank = lesserTemplate.getGrade();
								int attempts = 0;
								while ((rank.equals("A") || rank.equals("B")) && attempts < 50) {
									rnd = Rnd.get(0, 210);
									minionId = minionId(rnd);
									lesserTemplate = DataManager.MINION_DATA.getMinionTemplate(minionId);
									if (lesserTemplate != null) {
										rank = lesserTemplate.getGrade();
									}
									attempts++;
								}
							}
							break;

						default:
							minionId = minions.get(new Random().nextInt(minions.size()));
							break;
					}
				}

				minionTemplate = DataManager.MINION_DATA.getMinionTemplate(minionId);
				grade = minionTemplate.getGrade();
				level = minionTemplate.getLevel();
				name = minionTemplate.getName();
				growthPoint = minionTemplate.getGrowthPt();

				MinionCommonData addNewMinion = player.getMinionList().addNewMinion(player, minionId, name, grade, level, growthPoint);

				if (addNewMinion != null) {
					PacketSendUtility.sendPacket(player, new SM_MINIONS(1, addNewMinion, 0));
					player.getMinionList().updateMinionsList();
					checkQuest(player, item);
				}
			}
		}, 1500));
	}

	/**
	 * 若已达守护灵数量上限则提示并拒绝操作。
	 * Reject the action when minion limit is reached.
	 *
	 * @param player 玩家 / Player
	 * @return 是否已拒绝 / Whether rejected
	 */
	private static boolean rejectIfMinionLimitReached(Player player) {
		if (!isMinionLimitReached(player.getMinionList().getMinions().size())) {
			return false;
		}
		PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_FAMILIAR_MSG_CANNOT_CONTRACT_BY_MAXUNIT);
		return true;
	}

	/**
	 * 判断守护灵数量是否达到上限。
	 * Whether the minion count has reached the maximum.
	 *
	 * Current count
	 *
	 * @param minionCount @return 是否达上限 / Whether limit reached
	 */
	static boolean isMinionLimitReached(int minionCount) {
		return minionCount >= MAX_MINIONS;
	}

	/**
	 * 契约相关任务进度推进。
	 * Advance quest progress related to minion contracts.
	 *
	 * @param player 玩家 / Player
	 * @param item 使用的契约物品 / Contract item used
	 */
	private static void checkQuest(Player player, Item item) {
		switch (player.getRace()) {
		case ELYOS:
			if (player.getQuestStateList().hasQuest(15545) && item.getItemId() == 190080010) {
				QuestState qs = player.getQuestStateList().getQuestState(15545);
                if (qs != null && qs.getStatus() == QuestStatus.START) {
                    qs.setQuestVar(1);
                    qs.setStatus(QuestStatus.REWARD);
					PacketSendUtility.sendPacket(player, new SM_QUEST_ACTION(15545, qs.getStatus(), qs.getQuestVars().getQuestVars()));
					player.getController().updateNearbyQuests();
				}
			}
            if (player.getQuestStateList().hasQuest(19900) && item.getItemId() == 190080010) {
                QuestState qs = player.getQuestStateList().getQuestState(19900);
                if (qs != null && qs.getStatus() == QuestStatus.START) {
                    qs.setQuestVar(1);
                    qs.setStatus(QuestStatus.REWARD);
                    PacketSendUtility.sendPacket(player, new SM_QUEST_ACTION(19900, qs.getStatus(), qs.getQuestVars().getQuestVars()));
                    player.getController().updateNearbyQuests();
                }
            }
            break;
		case ASMODIANS:
			if (player.getQuestStateList().hasQuest(25545) && item.getItemId() == 190080011) {
				QuestState qs = player.getQuestStateList().getQuestState(25545);
                if (qs != null && qs.getStatus() == QuestStatus.START) {
                    qs.setQuestVar(1);
                    qs.setStatus(QuestStatus.REWARD);
					PacketSendUtility.sendPacket(player, new SM_QUEST_ACTION(25545, qs.getStatus(), qs.getQuestVars().getQuestVars()));
					player.getController().updateNearbyQuests();
				}
			}
            if (player.getQuestStateList().hasQuest(29900) && item.getItemId() == 190080011) {
                QuestState qs = player.getQuestStateList().getQuestState(29900);
                if (qs != null && qs.getStatus() == QuestStatus.START) {
                    qs.setQuestVar(1);
                    qs.setStatus(QuestStatus.REWARD);
                    PacketSendUtility.sendPacket(player, new SM_QUEST_ACTION(29900, qs.getStatus(), qs.getQuestVars().getQuestVars()));
                    player.getController().updateNearbyQuests();
                }
            }
            break;
        default:
            break;
		}
	}

	/**
	 * 召唤指定守护灵；若已有则先解散。
	 * Spawn the given minion; despawn the current one first if present.
	 *
	 * @param player 玩家 / Player
	 * @param minionObjId 守护灵对象 ID / Minion object id
	 */
	public void spawnMinion(Player player, int minionObjId) {
		MinionCommonData minionCommonData = player.getMinionList().getMinion(minionObjId);
		if (minionCommonData == null) {
			log.warn(I18n.get("log.9dcf7dbcf415", minionObjId));
			return;
		}
		
		MinionTemplate minionTemplate = DataManager.MINION_DATA.getMinionTemplate(minionCommonData.getMinionId());
		if (minionTemplate == null) {
			log.warn(I18n.get("log.9c4fbbb87d87", minionCommonData.getMinionId()));
			return;
		}
		
		MinionController controller = new MinionController();
		Minion minion = new Minion(minionTemplate, controller, minionCommonData, player);
		
		if (player.getMinion() != null) {
			despawnMinion(player, player.getMinionList().getLastUsed());
		}
		
		Iterator<MinionSkill> iterator = minionTemplate.getAction().getSkillsCollections().iterator();
		while (iterator.hasNext()) {
			int skillId = iterator.next().getSkillId();
			if (!player.getSkillList().isSkillPresent(skillId)) {
				player.getSkillList().addSkill(player, skillId, 1);
				log.debug("Added skill " + skillId + " to player " + player.getName());
			} else {
				log.debug("Skill " + skillId + " already present for player " + player.getName());
			}
		}
		
		minion.setKnownlist(new PlayerAwareKnownList(minion));
		player.setMinion(minion);
		player.getMinionList().setLastUsed(minionObjId);
		minionbuff.apply(player, minionCommonData.getMinionId());
		
		((MinionController) minion.getController()).startFollowing(player);
		
		PacketSendUtility.broadcastPacketAndReceive(player, new SM_MINIONS(5, minionCommonData));
	}

	/**
	 * 解散守护灵并移除其授予的技能与增益。
	 * Despawn the minion and remove granted skills and buffs.
	 *
	 * @param player 玩家 / Player
	 * @param minionObjId 守护灵对象 ID（0 表示当前） / Minion object id (0 = current)
	 */
	public void despawnMinion(Player player, int minionObjId) {
		Minion minion = player.getMinion();
		if (minion == null && minionObjId == 0) {
			log.debug("No active minion to despawn");
			return;
		}
		
		int despawnMinionObjId = minionObjId == 0 ? minion.getObjectId() : minionObjId;
		MinionCommonData minionCommonData = player.getMinionList().getMinion(despawnMinionObjId);
		if (minionCommonData == null) {
			log.warn(I18n.get("log.9dcf7dbcf415", despawnMinionObjId));
			return;
		}
		
		MinionTemplate minionTemplate = DataManager.MINION_DATA.getMinionTemplate(minionCommonData.getMinionId());
		if (minionTemplate == null) {
			log.warn(I18n.get("log.9c4fbbb87d87", minionCommonData.getMinionId()));
			return;
		}
		
		Iterator<MinionSkill> iterator = minionTemplate.getAction().getSkillsCollections().iterator();
		while (iterator.hasNext()) {
			int skillId = iterator.next().getSkillId();
			if (player.getSkillList().isSkillPresent(skillId)) {
				SkillLearnService.removeSkill(player, skillId);
				log.debug("Removed skill " + skillId + " from player " + player.getName());
			}
		}
		
		minionCommonData.setIsLooting(false);
		minionCommonData.setIsBuffing(false);
		
		if (minion != null) {
			((MinionController) minion.getController()).stopFollowing(player);
			minion.getController().delete();
			player.setMinion(null);
		}
		
		minionbuff.end(player);
		PacketSendUtility.broadcastPacketAndReceive(player, new SM_MINIONS(6, minionCommonData));
	}

	/**
	 * 消耗材料守护灵提升目标守护灵成长点。
	 * Consume material minions to raise target minion growth points.
	 *
	 * @param player 玩家 / Player
	 * @param minionObjectId 目标守护灵对象 ID / Target minion object id
	 * @param material 材料守护灵对象 ID 列表 / Material minion object ids
	 */
	public void growthUpMinion(Player player, int minionObjectId, List<Integer> material) {
		int growthPoint = 0;
		long growthCost = 0;
		String tierGrade = "";
		MinionCommonData playerMinion = player.getMinionList().getMinion(minionObjectId);
		tierGrade = DataManager.MINION_DATA.getMinionTemplate(playerMinion.getMinionId()).getGrade();
		int maxgrowthMax = DataManager.MINION_DATA.getMinionTemplate(playerMinion.getMinionId()).getMaxGrowthValue();
		for (MinionCommonData list : player.getMinionList().getMinions()) {
			for (int matObjt : material) {
				if (list.getObjectId() == matObjt) {
					int minionGrowth = 0;
					if (DataManager.MINION_DATA.getMinionTemplate(list.getMinionId()).getGrade().equalsIgnoreCase(tierGrade)) {
						minionGrowth = DataManager.MINION_DATA.getMinionTemplate(list.getMinionId()).getGrowthPt() * 2;
					} else {
						minionGrowth = DataManager.MINION_DATA.getMinionTemplate(list.getMinionId()).getGrowthPt();
					}
					growthPoint += minionGrowth;
					growthCost += DataManager.MINION_DATA.getMinionTemplate(list.getMinionId()).getGrowthCost();
				}
			}
		}
		if (growthPoint <= 0) {
			PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_FAMILIAR_GROWTH_MSG_NOTSELECT);
			return;
		}
		if (player.getInventory().getKinah() < growthCost) {
			PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_FAMILIAR_GROWTH_MSG_NOGOLD);
			return;
		}
		player.getInventory().decreaseKinah(growthCost);
		if (playerMinion.getMinionGrowthPoint() + growthPoint > maxgrowthMax) {
			playerMinion.setMinionGrowthPoint(maxgrowthMax);
		} else {
			playerMinion.setMinionGrowthPoint(playerMinion.getMinionGrowthPoint() + growthPoint);
		}
		DAOManager.getDAO(PlayerMinionsDAO.class).updatePlayerMinionGrowthPoint(player, playerMinion);
		PacketSendUtility.broadcastPacket(player, new SM_MINIONS(7, playerMinion), true);
		for (int matObjt2 : material) {
			deleteMinion(player, matObjt2, true);
		}
		player.getMinionList().updateMinionsList();
	}

	/**
	 * 消耗基纳与进化材料提升守护灵等级。
	 * Evolve a minion by spending kinah and evolution materials.
	 *
	 * @param player 玩家 / Player
	 * @param minionObjId 守护灵对象 ID / Minion object id
	 */
	public void evolutionUpMinion(Player player, int minionObjId) {
		MinionCommonData minion = player.getMinionList().getMinion(minionObjId);
		MinionEvolved items = DataManager.MINION_DATA.getMinionTemplate(player.getMinionList().getMinion(minionObjId).getMinionId()).getEvolved();
		if (minion.getMinionLevel() >= 4) {
			PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_FAMILIAR_EVOLVE_MSG_NOEVOLVE);
			return;
		}
		if (player.getInventory().getKinah() < items.getEvolvedCost()) {
			PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_FAMILIAR_EVOLVE_MSG_NOGOLD);
			return;
		}
		if (player.getInventory().getItemCountByItemId(190200000) < items.getEvolvedNum()) {
			PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_FAMILIAR_EVOLVE_MSG_LACK_ITEM);
			return;
		}
		player.getInventory().decreaseKinah(items.getEvolvedCost());
		player.getInventory().decreaseByItemId(190200000, items.getEvolvedNum());
		minion.setMinionId(minion.getMinionId() + 1);
		minion.setMinionLevel(minion.getMinionLevel() + 1);
		minion.setMinionGrowthPoint(0);
		DAOManager.getDAO(PlayerMinionsDAO.class).evolutionMinion(player, minion);
		PacketSendUtility.sendPacket(player, new SM_MINIONS(1, minion, 1));
		player.getMinionList().updateMinionsList();
	}

	/**
	 * 删除守护灵（普通删除或作为材料消耗）。
	 * Delete a minion (normal delete or material consumption).
	 *
	 * @param player 玩家 / Player
	 * @param minionObjId 守护灵对象 ID / Minion object id
	 * @param isMaterial 是否作为材料 / Whether used as material
	 */
	public void deleteMinion(Player player, int minionObjId, boolean isMaterial) {
		MinionCommonData minion = player.getMinionList().getMinion(minionObjId);
		if (minion != null) {
			player.getMinionList().deleteMinion(minion.getObjectId());
			PacketSendUtility.broadcastPacket(player, new SM_MINIONS(2, isMaterial, minion), true);
		} else {
			return;
		}
	}

	/**
	 * 锁定或解锁守护灵，防止误操作。
	 * Lock or unlock a minion to prevent accidental actions.
	 *
	 * @param player 玩家 / Player
	 * @param minionObjId 守护灵对象 ID / Minion object id
	 * @param lock 0 解锁 / 1 lock / 0 unlock。 / 0 解锁 / 1 lock / 0 unlock
	 */
	public void lockMinion(Player player, int minionObjId, int lock) {
		MinionCommonData minion = player.getMinionList().getMinion(minionObjId);
		if (lock == 1) {
			minion.setLock(true);
			DAOManager.getDAO(PlayerMinionsDAO.class).lockMinions(player, minionObjId, 1);
			PacketSendUtility.broadcastPacket(player, new SM_MINIONS(4, minion), true);
		} else {
			minion.setLock(false);
			DAOManager.getDAO(PlayerMinionsDAO.class).lockMinions(player, minionObjId, 0);
			PacketSendUtility.broadcastPacket(player, new SM_MINIONS(4, minion), true);
		}
	}

	/**
	 * 重命名守护灵。
	 * Rename a minion.
	 *
	 * 玩家 / Player
	 * @param minionObjId 守护灵对象 ID / Minion object id
	 * New name
	 */
	public void renameMinion(Player player, int minionObjId, String name) {
		MinionCommonData minion = player.getMinionList().getMinion(minionObjId);
		minion.setName(name);
		DAOManager.getDAO(PlayerMinionsDAO.class).updateMinionName(minion);
		PacketSendUtility.broadcastPacketAndReceive(player, new SM_MINIONS(3, minion));
	}

	/**
	 * 充能守护灵技能点数，并可设置自动充能。
	 * Charge minion skill points and optionally enable auto-charge.
	 *
	 * @param player 玩家 / Player
	 * @param charge 是否立即充能 / Whether to charge now
	 * @param autoCharge 是否自动充能 / Whether auto-charge
	 */
	public void addMinionSkillPoints(Player player, boolean charge, boolean autoCharge) {
		int currentSkillPoints = player.getMinionSkillPoints();
		player.getCommonData().setMinionSkillPointsAutoCharge(autoCharge);
		if (charge) {
			if (!player.getInventory().tryDecreaseKinah(chargePrice(currentSkillPoints))) {
				return;
			}
			currentSkillPoints = MAX_SKILL_POINTS;
			player.setMinionSkillPoints(currentSkillPoints);
			PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_FAMILIAR_MSG_FENERGY_CHARGE);
		}
		PacketSendUtility.sendPacket(player, new SM_MINIONS(11, currentSkillPoints, autoCharge));
	}

	/**
	 * 消耗守护灵技能能量；自动充能开启时不足则尝试购买。
	 * Consume minion skill energy; auto-charge buys more when short.
	 *
	 * 玩家 / Player
	 * Skill id
	 *
	 * @return 是否允许施放 / Whether cast is allowed
	 */
	public boolean consumeMinionSkillPoints(Player player, int skillId) {
		if (player.getMinion() == null || player.getMinion().getMinionTemplate().getAction() == null
				|| player.getMinion().getMinionTemplate().getAction().getSkillsCollections() == null) {
			return true;
		}

		MinionSkill minionSkill = player.getMinion().getMinionTemplate().getAction().getSkillsCollections().stream()
				.filter(skill -> skill.getSkillId() == skillId).findFirst().orElse(null);
		if (minionSkill == null) {
			return true;
		}

		int energyCost = minionSkill.getEnergyCost();
		int currentSkillPoints = player.getMinionSkillPoints();
		boolean autoCharge = player.getCommonData().isMinionSkillPointsAutoCharge();
		if (currentSkillPoints < energyCost) {
			if (!autoCharge) {
				PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_FAMILIAR_MSG_CANNOT_USE_FSKILL_BY_LACK_FENERGY);
				return false;
			}
			int skillPointsToAdd = MAX_SKILL_POINTS - currentSkillPoints;
			int price = chargePrice(currentSkillPoints);
			if (!player.getInventory().tryDecreaseKinah(price)) {
				player.getCommonData().setMinionSkillPointsAutoCharge(false);
				PacketSendUtility.sendPacket(player, new SM_MINIONS(11, currentSkillPoints, false));
				PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_FAMILIAR_MSG_FENERGY_AUTOCHARGING_FAIL_BY_GOLD);
				return false;
			}
			currentSkillPoints = MAX_SKILL_POINTS;
			PacketSendUtility.sendPacket(player, new SM_SYSTEM_MESSAGE(1404328, skillPointsToAdd, price));
		}

		player.setMinionSkillPoints(currentSkillPoints - energyCost);
		PacketSendUtility.sendPacket(player, new SM_MINIONS(11, player.getMinionSkillPoints(), autoCharge));
		return true;
	}

	/**
	 * 计算将技能点充至上限所需基纳。
	 * Compute kinah cost to charge skill points to the maximum.
	 *
	 * @param currentSkillPoints 当前技能点 / Current skill points
	 * Kinah cost
	 */
	static int chargePrice(int currentSkillPoints) {
		return Math.max(0, MAX_SKILL_POINTS - currentSkillPoints) * KINAH_PER_SKILL_POINT;
	}

	/**
	 * 激活守护灵功能（30 天，消耗基纳）。
	 * Activate minion functions for 30 days (costs kinah).
	 *
	 * @param player 玩家 / Player
	 */
	public void activateMinionFunction(Player player) {
		long leftTime = System.currentTimeMillis() + (30 * 24 * 60 * 60 * 1000);
		log.debug("Activate minion function. playerId={} expiresAt={}", player.getObjectId(), new Timestamp(leftTime));
		if (player.getInventory().tryDecreaseKinah(25000000)) {
			player.getCommonData().setMinionFunctionTime(new Timestamp(leftTime));
			PacketSendUtility.sendPacket(player, new SM_MINIONS(9, leftTime));
			PacketSendUtility.sendPacket(player, new SM_MINIONS(12));
		} else {
			return;
		}
	}

	/**
	 * 向守护灵功能背包添加增益物品。
	 * Add functional/doping items into the minion bag.
	 *
	 * 玩家 / Player
	 * @param minionObjectId 守护灵对象 ID / Minion object id
	 * Item template id
	 * Target slot
	 */
	public void addMinionFunctionItem(Player player, int minionObjectId, int itemId, int targetSlot) {
		Minion minions = player.getMinion();
		if (minions == null || minions.getObjectId() != minionObjectId || minions.getCommonData().getDopingBag() == null
				|| !isDopingSlot(targetSlot)) {
			return;
		}
		minions.getCommonData().getDopingBag().setItem(itemId, targetSlot);

		if (minions.getCommonData().getDopingBag().getFoodItem() != 0) {
			log.debug("Minion bag food. playerId={} minionId={} itemId={}", player.getObjectId(), minionObjectId,
					minions.getCommonData().getDopingBag().getFoodItem());
		}
		if (minions.getCommonData().getDopingBag().getDrinkItem() != 0) {
			log.debug("Minion bag drink. playerId={} minionId={} itemId={}", player.getObjectId(), minionObjectId,
					minions.getCommonData().getDopingBag().getDrinkItem());
		}
		for (int a : minions.getCommonData().getDopingBag().getScrollsUsed()) {
			log.debug("Minion bag scroll. playerId={} minionId={} itemId={}", player.getObjectId(), minionObjectId, a);
		}
		DAOManager.getDAO(PlayerMinionsDAO.class).saveDopingBag(player, minions.getCommonData(), minions.getCommonData().getDopingBag());
		PacketSendUtility.broadcastPacket(player, new SM_MINIONS(8, 0, minionObjectId, itemId, targetSlot, 0), true);
	}

	/**
	 * 从守护灵功能槽移除物品。
	 * Removes an item from a minion function slot.
	 */
	public void removeMinionFunctionItem(Player player, int minionObjectId, int slot) {
		Minion minion = player.getMinion();
		if (minion == null || minion.getObjectId() != minionObjectId || minion.getCommonData().getDopingBag() == null
				|| !isDopingSlot(slot)) {
			return;
		}
		minion.getCommonData().getDopingBag().setItem(0, slot);
		DAOManager.getDAO(PlayerMinionsDAO.class).saveDopingBag(player, minion.getCommonData(), minion.getCommonData().getDopingBag());
		PacketSendUtility.broadcastPacket(player, new SM_MINIONS(8, 1, minionObjectId, 0, slot, 0), true);
	}

	static boolean isDopingSlot(int slot) {
		return slot >= 0 && slot < 6;
	}

	/**
	 * 使用守护灵背包中的物品为玩家施加增益。
	 * Use an item from the minion bag to buff the player.
	 *
	 * 玩家 / Player
	 * @param minionObjectId 守护灵对象 ID / Minion object id
	 * Item template id
	 * Slot
	 */
	public void buffPlayer(final Player player, final int minionObjectId, int itemId, final int slot) {
		Minion minion = player.getMinion();
		if (minion == null || minion.getObjectId() != minionObjectId || minion.getCommonData().getDopingBag() == null
				|| !isDopingSlot(slot)) {
			return;
		}
		List<Item> items = player.getInventory().getItemsByItemId(itemId);
		if (items.isEmpty()) {
			return;
		}
		Item useItem = items.get(0);
		ItemActions itemActions = useItem.getItemTemplate().getActions();
		ItemUseLimits limit = new ItemUseLimits();
		int useDelay = player.getItemCooldown(useItem.getItemTemplate()) / 3;
		if (useDelay < 3000) {
			useDelay = 3000;
		}
		limit.setDelayId(useItem.getItemTemplate().getUseLimits().getDelayId());
		limit.setDelayTime(useDelay);
		if (player.isItemUseDisabled(limit)) {
			final int useItemId = itemId;
			GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
				@Override
				public void run() {
					PacketSendUtility.broadcastPacket(player, new SM_MINIONS(8, 0, minionObjectId, useItemId, slot, 0), true);
				}
			}, useDelay);
			return;
		}
		if (!RestrictionsManager.canUseItem(player, useItem) || player.isProtectionActive()) {
			player.addItemCoolDown(limit.getDelayId(), System.currentTimeMillis() + useDelay, useDelay / 1000);
		} else {
			player.getController().cancelCurrentSkill();
			for (AbstractItemAction itemAction : itemActions.getItemActions()) {
				if (itemAction.canAct(player, useItem, null)) {
					itemAction.act(player, useItem, null);
				}
			}
		}
		PacketSendUtility.broadcastPacket(player, new SM_MINIONS(8, 0, minionObjectId, itemId, slot, 0), true);
		itemId = minion.getCommonData().getDopingBag().getFoodItem();
		long totalDopes = player.getInventory().getItemCountByItemId(itemId);
		itemId = minion.getCommonData().getDopingBag().getDrinkItem();
		totalDopes += player.getInventory().getItemCountByItemId(itemId);
		final int[] scrollBag = minion.getCommonData().getDopingBag().getScrollsUsed();
		for (int i = 0; i < scrollBag.length; ++i) {
			if (scrollBag[i] != 0) {
				totalDopes += player.getInventory().getItemCountByItemId(scrollBag[i]);
			}
		}
		if (totalDopes == 0L) {
			minion.getCommonData().setIsBuffing(false);
			PacketSendUtility.broadcastPacket(player, new SM_MINIONS(8, 0, minionObjectId, itemId, slot, 0), true);
		}
	}

	/**
	 * 调整守护灵增益背包中卷轴槽位。
	 * Relocate doping bag scroll slots for a minion.
	 *
	 * 玩家 / Player
	 * @param minionObjectId 守护灵对象 ID / Minion object id
	 * Source slot
	 * Destination slot
	 */
	public void relocateDoping(Player player, int minionObjectId, int targetSlot, int destinationSlot) {
		MinionCommonData minions = player.getMinionList().getMinion(minionObjectId);
		if (minions == null || minions.getDopingBag() == null || targetSlot < 2 || !isDopingSlot(targetSlot)
				|| destinationSlot < 2 || !isDopingSlot(destinationSlot)) {
			return;
		}
		int[] scrollBag = minions.getDopingBag().getScrollsUsed();
		if (targetSlot - 2 >= scrollBag.length || destinationSlot - 2 >= scrollBag.length
				|| scrollBag[targetSlot - 2] == 0 || scrollBag[destinationSlot - 2] == 0) {
			return;
		}
		int targetItem = scrollBag[targetSlot - 2];
		minions.getDopingBag().setItem(scrollBag[destinationSlot - 2], targetSlot);
		PacketSendUtility.broadcastPacket(player, new SM_MINIONS(8, 0, minionObjectId, scrollBag[destinationSlot - 2], targetSlot, 0), true);
		minions.getDopingBag().setItem(targetItem, destinationSlot);
		PacketSendUtility.broadcastPacket(player, new SM_MINIONS(8, 0, minionObjectId, targetItem, 0, destinationSlot), true);
	}

	/**
	 * 切换守护灵自动拾取状态。
	 * Toggle minion auto-loot state.
	 *
	 * 玩家 / Player
	 * @param minionObjectId 守护灵对象 ID / Minion object id
	 * Whether to activate
	 */
	public void activateLoot(Player player, int minionObjectId, boolean activate) {
		Minion minion = player.getMinion();
		if (minion == null || minion.getObjectId() != minionObjectId || minion.getCommonData().isLooting() == activate) {
			return;
		}
		if (activate) {
			if (player.isInTeam()) {
				LootRuleType lootType = player.getLootGroupRules().getLootRule();
				if (lootType == LootRuleType.FREEFORALL) {
					PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_LOOTING_PET_MESSAGE03);
					return;
				}
			}
			PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_LOOTING_PET_MESSAGE01);
		}
		minion.getCommonData().setIsLooting(activate);
		PacketSendUtility.sendPacket(player, new SM_MINIONS(8, 1, 0, activate));
	}

	/**
	 * 组合多只守护灵生成新守护灵。
	 * Combine multiple minions into a new minion.
	 *
	 * @param player 玩家 / Player
	 * @param minionObjIds 参与组合的对象 ID 列表 / Object ids to combine
	 */
	public void CombinationMinion(Player player, List<Integer> minionObjIds) {
		log.debug("Minion Combination");

		if (player == null) {
			log.error(I18n.get("log.48fe7386f00a"));
			return;
		}
		if (player.getInventory() == null) {
			log.error(I18n.get("log.97c2776a982e"));
			return;
		}

		long kinah = player.getInventory().getKinah();

		if (kinah < 50000) {
			PacketSendUtility.sendPacket(player, new SM_SYSTEM_MESSAGE(1404348, new Object[0]));
			return;
		}

		player.getInventory().decreaseKinah(50000);

		log.debug("MinionObjIds size: " + minionObjIds.size());
		if (minionObjIds.isEmpty()) {
			log.debug("CRITICAL ERROR: MinionObjIds is empty!  Cannot combine minions.");
			return;
		}

		MinionCommonData minion = null;
		int point = 0;
		int level = 0;
		int totalGrowthPoint = 0;
		int validMinionCount = 0;

		for (int minions : minionObjIds) {
			log.debug("Processing minion with ObjId: " + minions);
			minion = player.getMinionList().getMinion(minions);
			if (minion == null) {
				log.debug("CRITICAL ERROR: player.getMinionList().getMinion(" + minions + ") returned null! Skipping this minion.");
				continue;
			}
			point += minion.getMinionGrowthPoint();
			level += minion.getMinionLevel();
			totalGrowthPoint += minion.getMinionGrowthPoint();
			validMinionCount++;
		}

		if (point == 0 && level == 0) {
			log.debug("CRITICAL ERROR: No valid minions found in MinionObjIds! Combination failed.");
			return;
		}

		int averageGrowthPoint = 0;
		if (validMinionCount > 0) {
			averageGrowthPoint = totalGrowthPoint / validMinionCount;
		} else {
			log.debug("CRITICAL ERROR: No valid minions found. Setting averageGrowthPoint to 0.");
		}

		int minionId = 0;
		String name = "";
		String grade = "";
		int levelNewMinion = 0;

		if (minionObjIds.isEmpty()) {
			log.debug("CRITICAL ERROR: MinionObjIds is empty! Cannot get grade.");
			return;
		}

		if (player.getMinionList().getMinion(minionObjIds.get(0)) == null){
			log.debug("CRITICAL ERROR: player.getMinionList().getMinion(" + minionObjIds.get(0) + ") is null!");
			return;
		}
		grade = player.getMinionList().getMinion(minionObjIds.get(0)).getMinionGrade();
		log.debug("Grade of first minion: " + grade);

		int rnd = 0;
		if (level > 0) {
			rnd = Rnd.get(0, 200) + ((point / level) / 1000) + (level / 4);
		} else { 
			log.debug("CRITICAL ERROR: Level is zero!  Setting rnd to 0.");
		}
		
		log.debug("Rnd: " + rnd);

		boolean result;
		if (rnd < 125) {
			result = false;
			rnd = Rnd.get(0, 3);
			log.debug("Combination failed. Using grade: " + grade + " and rnd: " + rnd + " to determine minionId.");
			switch (grade) {
				case "D":
					minionId = 980010;
					break;
				case "C":
					minionId = player.getMinionList().getMinion(minionObjIds.get(rnd)).getMinionId();
					break;
				case "B":
					minionId = player.getMinionList().getMinion(minionObjIds.get(rnd)).getMinionId();
					break;
				default:
					log.debug("WARNING: Unknown grade: " + grade + ".  Using default minionId 0.");
					minionId = 0;
					break;
			}
		} else {
			result = true;
			log.debug("Combination succeeded. Using grade: " + grade + " to determine minionId.");
			switch (grade) {
				case "D":
					minionId = minionId(Rnd.get(36, 420));
					break;
				case "C":
					if (player.getMinionList().getMinion(minionObjIds.get(0)).getMinionId() == 980011) {
						minionId = minionId(Rnd.get(141, 700));
					} else {
						minionId = minionId(Rnd.get(421, 700));
					}
					break;
				case "B":
					if (player.getMinionList().getMinion(minionObjIds.get(0)).getMinionId() == 980012) {
						minionId = minionId(Rnd.get(421, 980));
					} else {
						minionId = minionId(Rnd.get(701, 980));
					}
					break;
				default:
					log.debug("WARNING: Unknown grade: " + grade + ".  Using default minionId 0.");
					minionId = 0;
					break;
			}
		}

		if (player.getAccessLevel() > 5) {
			PacketSendUtility.sendMessage(player, (result ? "Success" : "Fail") + " Rnd:" + rnd + " Luck:" + 125);
		}

		MinionTemplate minionTemplate = DataManager.MINION_DATA.getMinionTemplate(minionId);
		if (minionTemplate == null) {
			return;
		}

		grade = minionTemplate.getGrade();
		levelNewMinion = minionTemplate.getLevel();
		name = minionTemplate.getName();
		
		log.info(I18n.get("log.7c43b9b99db4", minionId, name, grade, levelNewMinion));
		MinionCommonData addNewMinion = player.getMinionList().addNewMinion(player, minionId, name, grade, levelNewMinion, averageGrowthPoint);
		if (addNewMinion == null) {
			return;
		}

		PacketSendUtility.sendPacket(player, new SM_MINIONS(1, addNewMinion, (result ? 2 : 3)));

		for (int minionObjId : minionObjIds) {
			deleteMinion(player, minionObjId, true);
		}
		minionObjIds.clear();
		player.getMinionList().updateMinionsList();
	}

	/**
	 * 按随机值映射守护灵模板 ID。
	 * Map a random roll value to a minion template id.
	 *
	 * Random roll
	 *
	 * @param rnd @return 守护灵模板 ID / Minion template id
	 */
	private static int minionId(int rnd) {
		if (rnd <= 35) {
			return 980010; // Kerubar D
		} else if (rnd <= 70) {
			return 980011; // Kerubian C
		} else if (rnd <= 105) {
			return 980020; // Seiren C lv1
		} else if (rnd <= 140) {
			return 980021; // Seiren C lv2
		} else if (rnd <= 175) {
			return 980022; // Seiren C lv3
		} else if (rnd <= 210) {
			return 980023; // Seiren C lv4
		} else if (rnd <= 245) {
			return 980030; // Steel Rose C lv1
		} else if (rnd <= 280) {
			return 980031; // Steel Rose C lv2
		} else if (rnd <= 315) {
			return 980032; // Steel Rose C lv3
		} else if (rnd <= 350) {
			return 980033; // Steel Rose C lv4
		} else if (rnd <= 385) {
			return 980040; // Abija B lv1
		} else if (rnd <= 420) {
			return 980041; // Abija B lv2
		} else if (rnd <= 455) {
			return 980042; // Abija B lv3
		} else if (rnd <= 490) {
			return 980043; // Abija B lv4
		} else if (rnd <= 525) {
			return 980050; // Hamerun B lv1
		} else if (rnd <= 560) {
			return 980051; // Hamerun B lv2
		} else if (rnd <= 595) {
			return 980052; // Hamerun B lv3
		} else if (rnd <= 630) {
			return 980053; // Hamerun B lv4
		} else if (rnd <= 665) {
			return 980074; // Karemiwen B lv1
		} else if (rnd <= 700) {
			return 980075; // Karemiwen B lv2
		} else if (rnd <= 735) {
			return 980076; // Karemiwen B lv3
		} else if (rnd <= 770) {
			return 980077; // Karemiwen B lv4
		} else if (rnd <= 805) {
			return 980078; // Saendukal B lv1
		} else if (rnd <= 840) {
			return 980079; // Saendukal B lv2
		} else if (rnd <= 875) {
			return 980080; // Saendukal B lv3
		} else if (rnd <= 910) {
			return 980081; // Saendukal B lv4
		} else if (rnd <= 945) {
			return 980060; // Grendal A lv1
		} else if (rnd <= 980) {
			return 980061; // Grendal A lv2
		} else if (rnd <= 1015) {
			return 980062; // Grendal A lv3
		} else if (rnd <= 1050) {
			return 980063; // Grendal A lv4
		} else if (rnd <= 1085) {
			return 980070; // Sita A lv1
		} else if (rnd <= 1120) {
			return 980071; // Sita A lv2
		} else if (rnd <= 1155) {
			return 980072; // Sita A lv3
		} else if (rnd <= 1190) {
			return 980073; // Sita A lv4
		} else if (rnd <= 1225) {
			return 980082; // Weda A lv1
		} else if (rnd <= 1260) {
			return 980083; // Weda A lv2
		} else if (rnd <= 1295) {
			return 980084; // Weda A lv3
		} else if (rnd <= 1330) {
			return 980085; // Weda A lv4
		} else if (rnd <= 1365) {
			return 980086; // Kromede A lv1
		} else if (rnd <= 1400) {
			return 980087; // Kromede A lv2
		} else if (rnd <= 1435) {
			return 980088; // Kromede A lv3
		} else if (rnd <= 1470) {
			return 980089; // Kromede A lv4
		} else if (rnd <= 1505) {
			return 980090; // Hyperion A lv1
		} else if (rnd <= 1540) {
			return 980091; // Hyperion A lv2
		} else if (rnd <= 1575) {
			return 980092; // Hyperion A lv3
		} else if (rnd <= 1610) {
			return 980093; // Hyperion A lv4
		} else {
			return 980010; // Kerubar D (default)
		}
	}

	/**
	 * 返回服务单例；优先通过 Spring 提供者获取。
	 * Returns service singleton; prefers Spring provider when available.
	 *
	 * Service instance
	 */
	public static MinionService getInstance() {
		ObjectProvider<MinionService> provider = instanceProvider;
		if (provider != null) {
			return provider.getIfAvailable(() -> SingletonHolder.instance);
		}
		return SingletonHolder.instance;
	}

	/**
	 * 注入 Spring 实例提供者。
	 * Inject Spring instance provider.
	 *
	 * Provider
	 */
	public static void setInstanceProvider(ObjectProvider<MinionService> provider) {
		instanceProvider = provider;
	}

	private static class SingletonHolder {
		protected static final MinionService instance = new MinionService();
	}
}
