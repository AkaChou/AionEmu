package com.aionemu.gameserver.services.instance;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import lombok.extern.slf4j.Slf4j;

import com.aionemu.boot.i18n.I18n;
import com.aionemu.commons.database.DatabaseFactory;
import com.aionemu.commons.database.dao.DAOManager;
import com.aionemu.gameserver.dao.AbyssRankDAO;
import com.aionemu.gameserver.dao.InstanceRewardLedgerDAO;
import com.aionemu.gameserver.dao.InstanceRewardLedgerDAO.PendingReward;
import com.aionemu.gameserver.dao.InventoryDAO;
import com.aionemu.gameserver.dao.PlayerDAO;
import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.dataholders.RetailInstanceData.Row;
import com.aionemu.gameserver.model.gameobjects.Item;
import com.aionemu.gameserver.model.gameobjects.PersistentState;
import com.aionemu.gameserver.model.gameobjects.player.AbyssRank;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.items.ItemId;
import com.aionemu.gameserver.model.items.storage.Storage;
import com.aionemu.gameserver.model.items.storage.StorageType;
import com.aionemu.gameserver.model.instance.playerreward.BattlegroundPlayerReward;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.services.abyss.AbyssPointsService;
import com.aionemu.gameserver.services.item.ItemFactory;
import com.aionemu.gameserver.services.item.ItemPacketService;
import com.aionemu.gameserver.services.item.ItemPacketService.ItemUpdateType;
import com.aionemu.gameserver.services.item.ItemService;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.world.WorldMapInstance;

@Slf4j
public final class InstanceSettlementService {
	private static final int PAYLOAD_VERSION = 1;
	private static final String[] TIME_ATTACK_GRADES = { "s", "a", "b", "c", "d", "f" };

	private InstanceSettlementService() {
	}

	public static int timeAttackRank(int worldId, int score, long elapsedSeconds) {
		Row row = timeAttackRow(worldId);
		for (int rank = 1; rank < TIME_ATTACK_GRADES.length; rank++) {
			String grade = TIME_ATTACK_GRADES[rank - 1];
			int minimumScore = row.intValue(grade + "_score_minimum", 0);
			int maximumTime = row.intValue(grade + "_time_maximum", 0);
			if (score >= minimumScore && (maximumTime <= 0 || elapsedSeconds <= maximumTime)) {
				return rank;
			}
		}
		return TIME_ATTACK_GRADES.length;
	}

	public static int timeAttackWaitSeconds(int worldId) {
		return timeAttackRow(worldId).requiredInt("wait_time");
	}

	public static int timeAttackLimitSeconds(int worldId) {
		return timeAttackRow(worldId).requiredInt("time_limit");
	}

	public static int darkPoetaPrepareSeconds() {
		return darkPoetaValue("BUFF_TIME");
	}

	public static int darkPoetaLimitSeconds() {
		return darkPoetaValue("LIMIT_TIME");
	}

	public static int darkPoetaLeaveSeconds() {
		return darkPoetaValue("Leave_Time");
	}

	public static int darkPoetaRank(int score, long elapsedSeconds) {
		if (score < 0 || elapsedSeconds < 0) {
			throw new IllegalArgumentException("Dark Poeta score and elapsed time cannot be negative");
		}
		for (int rank = 1; rank <= 5; rank++) {
			String grade = TIME_ATTACK_GRADES[rank - 1].toUpperCase();
			if (score >= darkPoetaValue(grade + "_SCORE_MINIMUM")
					&& elapsedSeconds < darkPoetaValue(grade + "_TIME_MAXIMUM")) {
				return rank;
			}
		}
		return 7;
	}

	public static int darkPoetaBossGrade(int rank, int highestPlayerLevel) {
		return rank == 1 && highestPlayerLevel >= 55 ? 6 : rank;
	}

	public static int darkPoetaGatherScore(int gatherId) {
		return DataManager.RETAIL_INSTANCE_DATA.rewards("npc_scores").stream()
			.filter(row -> row.value("name").startsWith("score_gather_IDLF1_")
				&& row.intValue("gather_id", 0) == gatherId)
			.findFirst().map(row -> row.requiredInt("score")).orElse(0);
	}

	public static RewardPlan timeAttackPlan(int worldId, int rank) {
		Row row = timeAttackRow(worldId);
		String grade = TIME_ATTACK_GRADES[Math.max(0, Math.min(rank - 1, TIME_ATTACK_GRADES.length - 1))];
		List<RewardItem> items = new ArrayList<>();
		add(items, row.intValue("item1_id", 0), row.intValue(grade + "_item1_reward", 0));
		add(items, row.intValue("item2_id", 0), row.intValue(grade + "_item2_reward", 0));
		add(items, row.intValue(grade + "_bonus_item1", 0), row.intValue(grade + "_bonus_item1_reward", 0));
		add(items, row.intValue(grade + "_bonus_item2", 0), row.intValue(grade + "_bonus_item2_reward", 0));
		return new RewardPlan(items, 0, 0, row.intValue(grade + "_ap_reward", 0),
				row.intValue(grade + "_gp_reward", 0));
	}

	public static RewardPlan lunaPlan(int worldId, int rank) {
		if (DataManager.RETAIL_INSTANCE_DATA.lunaDungeonForWorld(worldId) == null) {
			throw new IllegalStateException("Missing retail Luna dungeon for world " + worldId);
		}
		return timeAttackPlan(worldId, rank);
	}

	public static RewardPlan infinityPlan(int floor) {
		Row row = DataManager.RETAIL_INSTANCE_DATA.rewards("infinity_indun_reward").stream()
				.filter(candidate -> candidate.requiredInt("floor") == floor)
				.findFirst().orElseThrow(() -> new IllegalStateException("Missing retail infinity reward for floor " + floor));
		List<RewardItem> items = new ArrayList<>();
		for (int i = 1; i <= 6; i++) {
			add(items, row.intValue("reward_item" + i + "_id", 0), row.intValue("reward_item" + i + "_count", 0));
		}
		return new RewardPlan(items, row.longValue("exp", 0), row.longValue("gold", 0),
				row.intValue("reward_ap", 0), row.intValue("reward_gp", 0));
	}

	public static RewardPlan tournamentPlan(Row tournament, int round) {
		if (tournament == null || round < 1 || round > tournament.requiredInt("round_count")) {
			throw new IllegalArgumentException("Invalid tournament reward round");
		}
		String prefix = "round_" + round;
		List<RewardItem> items = new ArrayList<>();
		for (int slot = 1; slot <= 6; slot++) {
			add(items, tournament.intValue(prefix + "_item" + slot + "_id", 0),
					tournament.longValue(prefix + "_item" + slot + "_cnt", 0));
		}
		return new RewardPlan(items, tournament.longValue(prefix + "_exp", 0),
				tournament.longValue(prefix + "_gold", 0), tournament.intValue(prefix + "_ap", 0),
				tournament.intValue(prefix + "_gp", 0));
	}

	public static RewardPlan battlegroundPlan(int worldId, int spawnPage, BattleResult result, double bonusRate,
			int teamScore, int calculateMask, int minimumTeamSize) {
		if (!Double.isFinite(bonusRate) || bonusRate < 0 || bonusRate > 1) {
			throw new IllegalArgumentException("Battleground bonus rate must be between 0 and 1");
		}
		if (teamScore < 0 || minimumTeamSize < 0) {
			throw new IllegalArgumentException("Battleground score and team size cannot be negative");
		}
		Row row = battlegroundRow(worldId, spawnPage);
		String outcome = result.key;
		List<RewardItem> items = new ArrayList<>();
		for (int i = 1; i <= 2; i++) {
			String number = "0" + i;
			add(items, row.intValue("reward_item_" + number + "_id", 0),
					row.longValue("reward_item_" + number + "_count_" + outcome, 0)
							+ scaled(row.longValue("reward_item_" + number + "_count", 0), bonusRate));
			add(items, row.intValue("reward_add_item_" + number + "_id_" + outcome, 0),
					row.longValue("reward_add_item_" + number + "_count_" + outcome, 0));
			if ((calculateMask & 1 << (i - 1)) != 0
					&& (result == BattleResult.WIN || row.intValue("calculate_type_" + number, 0) == 0)) {
				add(items, row.intValue("reward_item_" + number + "_calculate_id", 0),
						row.longValue("reward_item_" + number + "_count", 0));
			}
		}
		if (teamScore >= row.intValue("score_reward_cond_" + outcome, Integer.MAX_VALUE)) {
			for (int i = 1; i <= 2; i++) {
				String number = "0" + i;
				add(items, row.intValue("score_reward_item_" + number + "_id_" + outcome, 0),
						row.longValue("score_reward_item_" + number + "_count_" + outcome, 0));
			}
		}
		int conditionTier = 0;
		for (int condition = 1; condition <= 5; condition++) {
			int threshold = row.intValue("condition_reward_cond_0" + condition, Integer.MAX_VALUE);
			if (minimumTeamSize < threshold) {
				break;
			}
			conditionTier = condition;
		}
		if (conditionTier > 0) {
			String conditionNumber = "0" + conditionTier;
			for (int item = 1; item <= 3; item++) {
				String itemNumber = "0" + item;
				add(items, row.intValue("condition_reward_item_" + conditionNumber + "_id_" + outcome + "_" + itemNumber, 0),
						row.longValue("condition_reward_item_" + conditionNumber + "_count_" + outcome + "_" + itemNumber, 0));
			}
		}
		long exp = row.longValue("reward_exp_" + outcome, 0)
				+ scaled(row.longValue("reward_exp_bonus", 0), bonusRate);
		int ap = Math.toIntExact(row.longValue("reward_ap_" + outcome, 0)
				+ scaled(row.longValue("reward_ap_bonus", 0), bonusRate));
		int gp = Math.toIntExact(row.longValue("reward_gp_" + outcome, 0)
				+ scaled(row.longValue("reward_gp_bonus", 0), bonusRate));
		return new RewardPlan(items, exp, 0, ap, gp);
	}

	public static RewardPlan battlegroundPlan(WorldMapInstance instance, BattleResult result, double bonusRate,
			int teamScore, int calculateMask, int minimumTeamSize) {
		int spawnPage = instance.getDynamicInstance() == null ? 0 : instance.getDynamicInstance().getSpawnPage();
		return battlegroundPlan(instance.getMapId(), spawnPage, result, bonusRate, teamScore, calculateMask,
				minimumTeamSize);
	}

	public static ArenaReward arenaReward(Row row, int rank, int playerCount, int score, int totalScore,
			double rewardRate) {
		if (row == null || rank < 0 || rank >= playerCount || playerCount < 1 || score < 0 || totalScore < score
				|| !Double.isFinite(rewardRate) || rewardRate < 0) {
			throw new IllegalArgumentException("Invalid arena reward input");
		}
		int rankRate = row.intValue("rank" + rankNumber(rank) + "_rewardrate", 0);
		int totalRankRate = 0;
		for (int currentRank = 0; currentRank < playerCount; currentRank++) {
			totalRankRate += row.intValue("rank" + rankNumber(currentRank) + "_rewardrate", 0);
		}

		int basicAp = arenaScaled(row.intValue("reward_ap_base", 0), rewardRate);
		int playAp = arenaScaled(arenaShare(row.intValue("reward_ap_perplayer", 0), playerCount, score,
				totalScore, row.intValue("play_rewardrate", 0)), rewardRate);
		int rankAp = arenaScaled(arenaShare(row.intValue("reward_ap_perplayer", 0), playerCount, rankRate,
				totalRankRate, row.intValue("rank_rewardrate", 0)), rewardRate);
		int basicGp = arenaScaled(row.intValue("reward_gp_base", 0), rewardRate);
		int playGp = arenaScaled(arenaShare(row.intValue("reward_gp_perplayer", 0), playerCount, score,
				totalScore, row.intValue("play_rewardrate", 0)), rewardRate);
		int rankGp = arenaScaled(arenaShare(row.intValue("reward_gp_perplayer", 0), playerCount, rankRate,
				totalRankRate, row.intValue("rank_rewardrate", 0)), rewardRate);

		int item1Id = row.intValue("reward_itemid1", 0);
		int basicItem1 = arenaScaled(row.intValue("reward_base1", 0), rewardRate);
		int playItem1 = arenaScaled(arenaShare(row.intValue("reward_perplayer1", 0), playerCount, score,
				totalScore, row.intValue("play_rewardrate", 0)), rewardRate);
		int rankItem1 = arenaScaled(arenaShare(row.intValue("reward_perplayer1", 0), playerCount, rankRate,
				totalRankRate, row.intValue("rank_rewardrate", 0)), rewardRate);
		int item2Id = row.intValue("reward_itemid2", 0);
		int basicItem2 = arenaScaled(row.intValue("reward_base2", 0), rewardRate);
		int playItem2 = arenaScaled(arenaShare(row.intValue("reward_perplayer2", 0), playerCount, score,
				totalScore, row.intValue("play_rewardrate", 0)), rewardRate);
		int rankItem2 = arenaScaled(arenaShare(row.intValue("reward_perplayer2", 0), playerCount, rankRate,
				totalRankRate, row.intValue("rank_rewardrate", 0)), rewardRate);

		String rankPrefix = "rank" + rankNumber(rank);
		int bonusItem1Id = playerCount >= row.intValue("min_pc_give_reward_item1", Integer.MAX_VALUE)
				? row.intValue(rankPrefix + "_itemid1", 0) : 0;
		int bonusItem1Count = bonusItem1Id == 0 ? 0 : row.intValue(rankPrefix + "_itemcount1", 0);
		int bonusItem2Id = playerCount >= row.intValue("min_pc_give_reward_item2", Integer.MAX_VALUE)
				? row.intValue(rankPrefix + "_itemid2", 0) : 0;
		int bonusItem2Count = bonusItem2Id == 0 ? 0 : row.intValue(rankPrefix + "_itemcount2", 0);

		List<RewardItem> items = new ArrayList<>();
		add(items, item1Id, (long) basicItem1 + playItem1 + rankItem1);
		add(items, item2Id, (long) basicItem2 + playItem2 + rankItem2);
		add(items, bonusItem1Id, bonusItem1Count);
		add(items, bonusItem2Id, bonusItem2Count);
		RewardPlan plan = new RewardPlan(items, 0, 0, basicAp + playAp + rankAp, basicGp + playGp + rankGp);
		return new ArenaReward(plan, basicAp, playAp, rankAp, basicGp, playGp, rankGp,
				item1Id, basicItem1, playItem1, rankItem1, item2Id, basicItem2, playItem2, rankItem2,
				bonusItem1Id, bonusItem1Count, bonusItem2Id, bonusItem2Count);
	}

	public static boolean arenaScoreLimitReached(Row row, int maximumScore, int minimumScore) {
		if (row == null || minimumScore < 0 || maximumScore < minimumScore) {
			throw new IllegalArgumentException("Invalid arena score range");
		}
		return maximumScore >= row.requiredInt("score_limit_top")
				|| maximumScore - minimumScore >= row.requiredInt("score_limit_gap");
	}

	public static boolean settleTimeAttack(WorldMapInstance instance, Player player, int rank) {
		return settle(instanceUid(instance), player, "timeattack", timeAttackPlan(instance.getMapId(), rank));
	}

	public static boolean settleLuna(WorldMapInstance instance, Player player, int rank) {
		return settle(instanceUid(instance), player, "luna", lunaPlan(instance.getMapId(), rank));
	}

	public static boolean settleInfinity(WorldMapInstance instance, Player player, int floor) {
		return settle(instanceUid(instance), player, "infinity:" + floor, infinityPlan(floor));
	}

	public static boolean settleBattleground(WorldMapInstance instance, Player player, BattleResult result,
			double bonusRate, int teamScore, int calculateMask, int minimumTeamSize) {
		return settleBattleground(instance, player, result,
				battlegroundPlan(instance, result, bonusRate, teamScore, calculateMask, minimumTeamSize));
	}

	public static boolean settleBattleground(WorldMapInstance instance, Player player, BattleResult result,
			RewardPlan plan) {
		queueBattleground(instance, player.getObjectId(), result, plan);
		return settle(instanceUid(instance), player, "battleground:" + result.key, plan);
	}

	public static boolean queueBattleground(WorldMapInstance instance, int playerId, BattleResult result,
			RewardPlan plan) {
		return queue(instance, playerId, "battleground:" + result.key, plan);
	}

	public static BattleResult battlegroundResult(int teamScore, int opposingScore) {
		return teamScore > opposingScore ? BattleResult.WIN
				: teamScore < opposingScore ? BattleResult.LOSE : BattleResult.DRAW;
	}

	public static double battlegroundBonusRate(double participation, int teamScore, int opposingScore) {
		if (!Double.isFinite(participation) || participation < 0 || participation > 1
				|| teamScore < 0 || opposingScore < 0) {
			throw new IllegalArgumentException("Invalid battleground participation or score");
		}
		long totalScore = (long) teamScore + opposingScore;
		double teamShare = totalScore == 0 ? 0.5 : (double) teamScore / totalScore;
		return participation * teamShare;
	}

	public static void applyBattlegroundDisplay(BattlegroundPlayerReward playerReward, RewardPlan base,
			RewardPlan total) {
		playerReward.setSettlementValues(Math.toIntExact(base.exp()), Math.toIntExact(total.exp() - base.exp()),
				base.ap(), total.ap() - base.ap(), base.gp(), total.gp() - base.gp());
		playerReward.clearRewardItems();
		for (int slot = 0; slot < Math.min(5, total.items().size()); slot++) {
			RewardItem item = total.items().get(slot);
			playerReward.setRewardItem(slot, item.itemId(), item.count());
		}
	}

	public static boolean queue(WorldMapInstance instance, int playerId, String rewardKey, RewardPlan plan) {
		String payload = plan.encode();
		return ledger().queue(instanceUid(instance), playerId, rewardKey, hash(payload), payload,
				System.currentTimeMillis());
	}

	public static int retryPending(Player player) {
		int completed = 0;
		for (PendingReward pending : ledger().loadPending(player.getObjectId())) {
			try {
				RewardPlan plan = RewardPlan.decode(pending.payloadJson());
				if (!hash(pending.payloadJson()).equals(pending.payloadHash())) {
					throw new IllegalStateException("Pending instance reward payload hash mismatch");
				}
				if (settle(pending.instanceUid(), player, pending.rewardKey(), plan)) {
					completed++;
				}
			} catch (RuntimeException e) {
				log.warn(I18n.get("log.f4d91bd76720", player.getObjectId(), pending.rewardKey(), e));
			}
		}
		return completed;
	}

	public static boolean settle(long instanceUid, Player player, String rewardKey, RewardPlan plan) {
		if (instanceUid <= 0 || player == null || rewardKey == null || rewardKey.isBlank()) {
			throw new IllegalArgumentException("Incomplete instance settlement identity");
		}
		synchronized (player) {
			return settleLocked(instanceUid, player, rewardKey, plan);
		}
	}

	private static boolean settleLocked(long instanceUid, Player player, String rewardKey, RewardPlan plan) {
		String payload = plan.encode();
		String payloadHash = hash(payload);
		Storage inventory = player.getInventory();
		List<Item> rewardItems = createItems(plan.items());
		try {
			ensureCapacity(inventory, rewardItems);
		} catch (RuntimeException e) {
			ItemService.releaseItemIds(rewardItems);
			throw e;
		}

		Item kinahItem = inventory.getKinahItem();
		boolean newKinah = plan.kinah() > 0 && kinahItem == null;
		if (newKinah) {
			kinahItem = ItemFactory.newItem(ItemId.KINAH.value(), plan.kinah());
			kinahItem.setItemLocation(StorageType.CUBE.getId());
		}
		long oldKinah = newKinah || kinahItem == null ? 0 : kinahItem.getItemCount();
		PersistentState oldKinahState = newKinah || kinahItem == null ? null : kinahItem.getPersistentState();
		if (plan.kinah() > 0 && !newKinah) {
			kinahItem.setItemCount(Math.addExact(oldKinah, plan.kinah()));
		}

		long oldExp = player.getCommonData().getExp();
		long newExp = player.getCommonData().capExp(Math.addExact(oldExp, plan.exp()));
		AbyssRank storedRank = copyRank(player.getAbyssRank());
		if (plan.ap() > 0) {
			storedRank.addAp(plan.ap(), player);
		}
		if (plan.gp() > 0) {
			storedRank.addGp(plan.gp());
		}

		List<Item> persistedItems = new ArrayList<>(rewardItems);
		if (plan.kinah() > 0) {
			persistedItems.add(kinahItem);
		}
		InventoryDAO inventoryDAO = DAOManager.getDAO(InventoryDAO.class);
		boolean alreadyCompleted;
		try (Connection connection = DatabaseFactory.getConnection()) {
			connection.setAutoCommit(false);
			try {
				alreadyCompleted = ledger().lockOrCreate(connection, instanceUid, player.getObjectId(), rewardKey,
						payloadHash, payload, System.currentTimeMillis());
				if (!alreadyCompleted) {
					inventoryDAO.storeInTransaction(connection, persistedItems, player.getObjectId(), null, null);
					if (newExp != oldExp) {
						DAOManager.getDAO(PlayerDAO.class).storeExpInTransaction(connection, player.getObjectId(), newExp);
					}
					if (plan.ap() > 0 || plan.gp() > 0) {
						DAOManager.getDAO(AbyssRankDAO.class).storeInTransaction(connection, player.getObjectId(), storedRank);
					}
					ledger().complete(connection, instanceUid, player.getObjectId(), rewardKey, System.currentTimeMillis());
				}
				connection.commit();
			} catch (SQLException | RuntimeException e) {
				connection.rollback();
				throw e;
			}
		} catch (SQLException | RuntimeException e) {
			restoreKinah(kinahItem, newKinah, oldKinah, oldKinahState);
			ItemService.releaseItemIds(rewardItems);
			if (newKinah) {
				ItemService.releaseItemId(kinahItem);
			}
			throw new IllegalStateException("Failed to settle instance reward " + rewardKey + " for "
					+ player.getObjectId(), e);
		}
		if (alreadyCompleted) {
			restoreKinah(kinahItem, newKinah, oldKinah, oldKinahState);
			ItemService.releaseItemIds(rewardItems);
			if (newKinah) {
				ItemService.releaseItemId(kinahItem);
			}
			return false;
		}

		inventoryDAO.markStored(persistedItems);
		for (Item item : rewardItems) {
			inventory.onLoadHandler(item);
			ItemPacketService.sendStorageUpdatePacket(player, StorageType.CUBE, item);
		}
		if (plan.kinah() > 0) {
			if (newKinah) {
				inventory.onLoadHandler(kinahItem);
				ItemPacketService.sendStorageUpdatePacket(player, StorageType.CUBE, kinahItem);
			} else {
				ItemPacketService.sendItemPacket(player, StorageType.CUBE, kinahItem, ItemUpdateType.INC_KINAH_COLLECT);
			}
		}
		if (newExp != oldExp) {
			player.getCommonData().setExp(newExp, false);
			PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_GET_EXP2(newExp - oldExp));
		}
		if (plan.ap() > 0) {
			AbyssPointsService.addAp(player, plan.ap());
		}
		if (plan.gp() > 0) {
			AbyssPointsService.addGp(player, plan.gp());
		}
		player.getAbyssRank().setPersistentState(PersistentState.UPDATED);
		return true;
	}

	private static void ensureCapacity(Storage inventory, List<Item> items) {
		long normal = items.stream().filter(item -> item.getItemTemplate().getExtraInventoryId() < 1).count();
		long special = items.size() - normal;
		if (normal > inventory.getFreeSlots() || special > inventory.getSpecialCubeFreeSlots()) {
			throw new IllegalStateException("Not enough inventory slots for instance reward");
		}
	}

	private static List<Item> createItems(Collection<RewardItem> rewards) {
		List<Item> items = new ArrayList<>();
		for (RewardItem reward : rewards) {
			long remaining = reward.count();
			while (remaining > 0) {
				Item item = ItemFactory.newItem(reward.itemId());
				if (item == null) {
					ItemService.releaseItemIds(items);
					throw new IllegalStateException("Unknown instance reward item " + reward.itemId());
				}
				long count = Math.min(remaining, item.getItemTemplate().getMaxStackCount());
				item.setItemCount(count);
				item.setItemLocation(StorageType.CUBE.getId());
				items.add(item);
				remaining -= count;
			}
		}
		return items;
	}

	private static AbyssRank copyRank(AbyssRank rank) {
		AbyssRank copy = new AbyssRank(rank.getDailyAP(), rank.getDailyGP(), rank.getWeeklyAP(), rank.getWeeklyGP(),
				rank.getAp(), rank.getGp(), rank.getRank().getId(), rank.getTopRanking(), rank.getDailyKill(),
				rank.getWeeklyKill(), rank.getAllKill(), rank.getMaxRank(), rank.getLastKill(), rank.getLastAP(),
				rank.getLastGP(), rank.getLastUpdate());
		copy.setPersistentState(rank.getPersistentState() == PersistentState.NEW ? PersistentState.NEW
				: PersistentState.UPDATE_REQUIRED);
		return copy;
	}

	private static void restoreKinah(Item item, boolean newKinah, long count, PersistentState state) {
		if (item != null && !newKinah) {
			item.setItemCount(count);
			item.setPersistentState(state);
		}
	}

	private static long instanceUid(WorldMapInstance instance) {
		if (instance == null || instance.getDynamicInstance() == null || instance.getDynamicInstance().getInstanceUid() <= 0) {
			throw new IllegalStateException("Instance reward requires a persisted dynamic instance");
		}
		return instance.getDynamicInstance().getInstanceUid();
	}

	private static InstanceRewardLedgerDAO ledger() {
		return DAOManager.getDAO(InstanceRewardLedgerDAO.class);
	}

	private static void add(List<RewardItem> items, int itemId, long count) {
		if (itemId > 0 && count > 0) {
			items.add(new RewardItem(itemId, count));
		}
	}

	private static Row timeAttackRow(int worldId) {
		return DataManager.RETAIL_INSTANCE_DATA.rewards("world_timeattack").stream()
				.filter(candidate -> candidate.requiredInt("world_id") == worldId)
				.findFirst().orElseThrow(() -> new IllegalStateException("Missing retail time attack reward for " + worldId));
	}

	private static int darkPoetaValue(String name) {
		String key = "IDLF1_" + name;
		return DataManager.RETAIL_INSTANCE_DATA.rewards("instant_dungeon_define").stream()
			.filter(row -> key.equals(row.value("name")))
			.findFirst().orElseThrow(() -> new IllegalStateException(
				"Missing retail Dark Poeta definition " + key)).requiredInt("value");
	}

	private static Row battlegroundRow(int worldId, int spawnPage) {
		List<Row> rows = DataManager.RETAIL_INSTANCE_DATA.rewards("instant_dungeon_battleground").stream()
				.filter(candidate -> candidate.requiredInt("world_id") == worldId).toList();
		if (rows.isEmpty()) {
			throw new IllegalStateException("Missing retail battleground reward for " + worldId);
		}
		return rows.stream().filter(row -> row.intValue("spawn_page", 0) == spawnPage).findFirst()
				.orElseGet(() -> {
					if (rows.size() == 1) {
						return rows.getFirst();
					}
					throw new IllegalStateException("Missing retail battleground reward for " + worldId
							+ " spawn page " + spawnPage);
				});
	}

	public static Row arenaRow(int worldId, int spawnPage) {
		return DataManager.RETAIL_INSTANCE_DATA.rewards("instant_dungeon_idarenapvp").stream()
				.filter(row -> row.requiredInt("world_id") == worldId
						&& row.requiredInt("spawn_page") == spawnPage)
				.findFirst().orElseThrow(() -> new IllegalStateException("Missing retail arena reward for " + worldId
						+ " spawn page " + spawnPage));
	}

	private static int arenaShare(int perPlayer, int playerCount, int weight, int totalWeight, int rate) {
		if (perPlayer <= 0 || weight <= 0 || totalWeight <= 0 || rate <= 0) {
			return 0;
		}
		return (int) (((double) weight * ((long) perPlayer * playerCount) / totalWeight) * rate / 100);
	}

	private static int arenaScaled(int value, double rewardRate) {
		return (int) (value * rewardRate);
	}

	private static String rankNumber(int rank) {
		return String.format("%02d", rank + 1);
	}

	private static long scaled(long value, double rate) {
		return (long) (value * rate);
	}

	static String hash(String payload) {
		try {
			return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
					.digest(payload.getBytes(StandardCharsets.UTF_8)));
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException("SHA-256 is unavailable", e);
		}
	}

	public record RewardItem(int itemId, long count) {
		public RewardItem {
			if (itemId <= 0 || count <= 0) {
				throw new IllegalArgumentException("Instance reward item must be positive");
			}
		}
	}

	public record ArenaReward(RewardPlan plan, int basicAp, int playAp, int rankAp, int basicGp, int playGp,
			int rankGp, int item1Id, int basicItem1, int playItem1, int rankItem1, int item2Id, int basicItem2,
			int playItem2, int rankItem2, int bonusItem1Id, int bonusItem1Count, int bonusItem2Id,
			int bonusItem2Count) {
	}

	public enum BattleResult {
		WIN("win"), DRAW("draw"), LOSE("lose");

		private final String key;

		BattleResult(String key) {
			this.key = key;
		}
	}

	public record RewardPlan(List<RewardItem> items, long exp, long kinah, int ap, int gp) {
		public RewardPlan {
			if (exp < 0 || kinah < 0 || ap < 0 || gp < 0) {
				throw new IllegalArgumentException("Instance rewards cannot be negative");
			}
			Map<Integer, Long> normalized = new TreeMap<>();
			for (RewardItem item : items == null ? List.<RewardItem>of() : items) {
				normalized.merge(item.itemId(), item.count(), Math::addExact);
			}
			items = normalized.entrySet().stream().map(entry -> new RewardItem(entry.getKey(), entry.getValue())).toList();
		}

		public long itemCount(int itemId) {
			return items.stream().filter(item -> item.itemId() == itemId).mapToLong(RewardItem::count).sum();
		}

		public String encode() {
			try {
				ByteArrayOutputStream bytes = new ByteArrayOutputStream();
				try (DataOutputStream output = new DataOutputStream(bytes)) {
					output.writeInt(PAYLOAD_VERSION);
					output.writeLong(exp);
					output.writeLong(kinah);
					output.writeInt(ap);
					output.writeInt(gp);
					output.writeInt(items.size());
					for (RewardItem item : items) {
						output.writeInt(item.itemId());
						output.writeLong(item.count());
					}
				}
				return "{\"version\":1,\"data\":\"" + Base64.getEncoder().encodeToString(bytes.toByteArray()) + "\"}";
			} catch (IOException e) {
				throw new IllegalStateException("Failed to encode instance reward", e);
			}
		}

		public static RewardPlan decode(String payload) {
			int marker = payload == null ? -1 : payload.indexOf("\"data\":\"");
			int start = marker + 8;
			int end = marker < 0 ? -1 : payload.indexOf('"', start);
			if (marker < 0 || end < 0) {
				throw new IllegalStateException("Invalid instance reward payload");
			}
			try (DataInputStream input = new DataInputStream(
					new ByteArrayInputStream(Base64.getDecoder().decode(payload.substring(start, end))))) {
				if (input.readInt() != PAYLOAD_VERSION) {
					throw new IllegalStateException("Unsupported instance reward payload version");
				}
				long exp = input.readLong();
				long kinah = input.readLong();
				int ap = input.readInt();
				int gp = input.readInt();
				int size = input.readInt();
				if (size < 0 || size > 100) {
					throw new IllegalStateException("Invalid instance reward item count");
				}
				List<RewardItem> items = new ArrayList<>(size);
				for (int i = 0; i < size; i++) {
					items.add(new RewardItem(input.readInt(), input.readLong()));
				}
				if (input.available() != 0) {
					throw new IllegalStateException("Trailing instance reward payload bytes");
				}
				return new RewardPlan(items, exp, kinah, ap, gp);
			} catch (IOException | IllegalArgumentException e) {
				throw new IllegalStateException("Failed to decode instance reward", e);
			}
		}
	}
}
