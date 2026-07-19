package com.aionemu.gameserver.model.autogroup;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BooleanSupplier;

import com.aionemu.commons.taskmanager.AbstractLockManager;
import com.aionemu.gameserver.model.PlayerClass;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.gameobjects.Item;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.instance.instancereward.InstanceReward;
import com.aionemu.gameserver.services.instance.InstanceAdmissionService;
import com.aionemu.gameserver.services.instance.InstanceAdmissionService.Admission;
import com.aionemu.gameserver.world.WorldMapInstance;

/**
 * 自动副本，用于 autogroup 相关逻辑。
 * Auto Instance for autogroup logic.
 */

public abstract class AutoInstance extends AbstractLockManager implements AutoInstanceHandler {
	protected int instanceMaskId;
	public long startInstanceTime;
	public WorldMapInstance instance;
	public MatchDefinition agt;
	public Map<Integer, AGPlayer> players = new ConcurrentHashMap<Integer, AGPlayer>();
	private final Map<Integer, Admission> pendingAdmissions = new ConcurrentHashMap<>();

	protected boolean decrease(Player player, int itemId, long count) {
		long i = 0;
		List<Item> items = player.getInventory().getItemsByItemId(itemId);
		for (Item findedItem : items) {
			i += findedItem.getItemCount();
		}
		if (i < count) {
			return false;
		}
		Collections.sort(items, new Comparator<Item>() {
			/** 比较 / compare. */
			@Override
			public int compare(Item o1, Item o2) {
				return Long.compare(o1.getExpireTime(), o2.getExpireTime());
			}
		});
		for (Item item : items) {
			long l = player.getInventory().decreaseItemCount(item, count);
			if (l == 0) {
				break;
			} else {
				count = l;
			}
		}
		return true;
	}

	protected List<AGPlayer> getAGPlayersByRace(Race race) {
		List<AGPlayer> result = new ArrayList<AGPlayer>();
		for (AGPlayer agPlayer : players.values()) {
			if (agPlayer.getRace() == race) {
				result.add(agPlayer);
			}
		}
		return result;
	}

	protected List<Player> getPlayersByRace(Race race) {
		List<Player> result = new ArrayList<Player>();
		for (Player player : instance.getPlayersInside()) {
			if (player.getRace() == race) {
				result.add(player);
			}
		}
		return result;
	}

	protected List<AGPlayer> getPlayersByClass(PlayerClass playerClass) {
		List<AGPlayer> result = new ArrayList<AGPlayer>();
		for (AGPlayer agPlayer : players.values()) {
			if (agPlayer.getPlayerClass() == playerClass) {
				result.add(agPlayer);
			}
		}
		return result;
	}

	/** 初始化 / initsialize. */
	@Override
	public void initsialize(int instanceMaskId) {
		this.instanceMaskId = instanceMaskId;
		agt = MatchDefinition.getByMaskId(instanceMaskId);
	}

	/** 副本创建 / On Instance Create*/
	@Override
	public void onInstanceCreate(WorldMapInstance instance) {
		this.instance = instance;
		startInstanceTime = System.currentTimeMillis();
	}

	public void restorePlayer(AGPlayer player) {
		players.put(player.getObjectId(), player);
	}

	/** 添加玩家。 / Adds player. */
	@Override
	public AGQuestion addPlayer(Player player, SearchInstance searchInstance) {
		return AGQuestion.FAILED;
	}

	protected final AGQuestion addSidedPlayers(Player player, SearchInstance searchInstance) {
		writeLock();
		try {
			List<Player> candidates = new ArrayList<>();
			if (searchInstance.getEntryRequestType().isGroupEntry()) {
				for (Player member : player.getPlayerGroup2().getOnlineMembers()) {
					if (searchInstance.getMembers().contains(member.getObjectId())) {
						candidates.add(member);
					}
				}
			} else {
				candidates.add(player);
			}
			if (!satisfyTime(searchInstance)) {
				return AGQuestion.FAILED;
			}
			byte side = allocateSide(candidates.size(), player.getRace());
			if (side < 0) {
				return AGQuestion.FAILED;
			}
			List<AGPlayer> accepted = new ArrayList<>();
			for (AGPlayer matchPlayer : players.values()) {
				if (matchPlayer.getMatchSide() == side) {
					accepted.add(matchPlayer);
				}
			}
			for (Player candidate : candidates) {
				if (!agt.canAdd(candidate.getPlayerClass(), accepted, 1)) {
					return AGQuestion.FAILED;
				}
				AGPlayer matchPlayer = new AGPlayer(candidate);
				matchPlayer.setMatchSide(side);
				accepted.add(matchPlayer);
			}
			for (AGPlayer matchPlayer : accepted) {
				players.putIfAbsent(matchPlayer.getObjectId(), matchPlayer);
			}
			return instance != null ? AGQuestion.ADDED
					: agt.isCompositionReady(players.values()) ? AGQuestion.READY : AGQuestion.ADDED;
		} finally {
			writeUnlock();
		}
	}

	/** 进入副本 / On Enter Instance*/
	@Override
	public void onEnterInstance(Player player) {
		pendingAdmissions.remove(player.getObjectId());
		players.get(player.getObjectId()).setInInstance(true);
		players.get(player.getObjectId()).setOnline(true);
	}

	/** 离开副本 / On Leave Instance*/
	@Override
	public void onLeaveInstance(Player player) {
	}

	/** 按下回车时 / on Press Enter. */
	@Override
	public void onPressEnter(Player player) {
		enter(player, () -> true);
	}

	protected final boolean enter(Player player, BooleanSupplier transfer) {
		AGPlayer matchPlayer = players.get(player.getObjectId());
		if (matchPlayer == null) {
			return false;
		}
		if (matchPlayer.getMatchSide() < 0) {
			byte side = allocateSide(1, player.getRace());
			if (side < 0) {
				return false;
			}
			matchPlayer.setMatchSide(side);
		}
		Admission admission = InstanceAdmissionService.admitMatch(instance, player, matchPlayer.getMatchSide());
		if (admission == null) {
			return false;
		}
		try {
			pendingAdmissions.put(player.getObjectId(), admission);
			if (!transfer.getAsBoolean()) {
				pendingAdmissions.remove(player.getObjectId());
				admission.rollback();
				return false;
			}
			matchPlayer.setPressEnter(true);
			return true;
		} catch (RuntimeException | Error e) {
			pendingAdmissions.remove(player.getObjectId());
			admission.rollback();
			throw e;
		}
	}

	protected final byte allocateSide(int additions, Race race) {
		if (agt.getMatchSides() == 1) {
			return 0;
		}
		if (!agt.isRaceFree() && agt.getMatchSides() == 2) {
			return (byte) race.getRaceId();
		}
		byte selected = -1;
		int smallest = Integer.MAX_VALUE;
		for (byte side = 0; side < agt.getMatchSides(); side++) {
			int count = 0;
			for (AGPlayer matchPlayer : players.values()) {
				if (matchPlayer.getMatchSide() == side) {
					count++;
				}
			}
			if (count + additions <= agt.getPlayersPerSide() && count < smallest) {
				selected = side;
				smallest = count;
			}
		}
		return selected;
	}

	/** 注销。 / Unregister. */
	@Override
	public void unregister(Player player) {
		Integer obj = player.getObjectId();
		Admission admission = pendingAdmissions.remove(obj);
		if (admission != null) {
			admission.rollback();
		}
		if (players.containsKey(obj)) {
			players.remove(obj);
		}
	}

	/** 清空。 / Clear. */
	@Override
	public void clear() {
		for (Admission admission : pendingAdmissions.values()) {
			admission.rollback();
		}
		pendingAdmissions.clear();
		players.clear();
	}

	protected boolean satisfyTime(SearchInstance searchInstance) {
		if (instance != null) {
			InstanceReward<?> instanceReward = instance.getInstanceHandler().getInstanceReward();
			if ((instanceReward != null && instanceReward.getInstanceScoreType().isEndProgress())) {
				return false;
			}
		}
		if (!searchInstance.getEntryRequestType().isFastGroupEntry()) {
			return startInstanceTime == 0;
		}
		int time = agt.getTime();
		if (time == 0 || startInstanceTime == 0) {
			return true;
		}
		return System.currentTimeMillis() - startInstanceTime < time;
	}
}
