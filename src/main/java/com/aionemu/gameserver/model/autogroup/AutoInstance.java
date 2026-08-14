package com.aionemu.gameserver.model.autogroup;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.aionemu.commons.taskmanager.AbstractLockManager;
import com.aionemu.gameserver.model.PlayerClass;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.gameobjects.Item;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.instance.instancereward.InstanceReward;
import com.aionemu.gameserver.world.WorldMapInstance;

/**
 * 自动副本，用于 autogroup 相关逻辑。
 * Auto Instance for autogroup logic.
 */

public abstract class AutoInstance extends AbstractLockManager implements AutoInstanceHandler {
	protected int instanceMaskId;
	public long startInstanceTime;
	public WorldMapInstance instance;
	public AutoGroupType agt;
	public Map<Integer, AGPlayer> players = new ConcurrentHashMap<Integer, AGPlayer>();

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

	/** 初始化 / Initialize. */
	@Override
	public void initsialize(int instanceMaskId) {
		this.instanceMaskId = instanceMaskId;
		agt = AutoGroupType.getAGTByMaskId(instanceMaskId);
	}

	/** 副本创建 / On Instance Create*/
	@Override
	public void onInstanceCreate(WorldMapInstance instance) {
		this.instance = instance;
		startInstanceTime = System.currentTimeMillis();
	}

	/** 添加玩家。 / Adds player. */
	@Override
	public AGQuestion addPlayer(Player player, SearchInstance searchInstance) {
		return AGQuestion.FAILED;
	}

	/** 进入副本 / On Enter Instance*/
	@Override
	public void onEnterInstance(Player player) {
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
		players.get(player.getObjectId()).setPressEnter(true);
	}

	/** 注销。 / Unregister. */
	@Override
	public void unregister(Player player) {
		Integer obj = player.getObjectId();
		if (players.containsKey(obj)) {
			players.remove(obj);
		}
	}

	/** 清空。 / Clear. */
	@Override
	public void clear() {
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
