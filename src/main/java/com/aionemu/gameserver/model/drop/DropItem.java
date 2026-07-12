package com.aionemu.gameserver.model.drop;

import java.util.ArrayList;
import java.util.List;

import com.aionemu.commons.utils.Rnd;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.templates.item.ItemTemplate;
import com.aionemu.gameserver.world.World;

/**
 * 掉落物品模型。
 * Drop Item model.
 *
 * @author ATracer
 */
public class DropItem {

	private int index = 0;
	private long count = 0;
	private final Drop dropTemplate;
	private List<Integer> playerObjIds = new ArrayList<>();
	private boolean isFreeForAll = false;
	private long highestValue = 0;
	private Player winningPlayer = null;
	private boolean isItemWonNotCollected = false;
	private boolean isDistributeItem = false;
	private int npcObj;

	private int optionalSocket = 0;

	public DropItem(Drop dropTemplate) {
		this.dropTemplate = dropTemplate;
		ItemTemplate template = dropTemplate.getItemTemplate();
		int optionalBonus = template.getOptionSlotBonus();
		if (optionalBonus != 0) {
			optionalSocket = -1;
		}
	}

	/**
	 * Regenerates item count from the drop template range
	 */
	public void calculateCount() {
		count = Rnd.get(dropTemplate.getMinAmount(), dropTemplate.getMaxAmount());
	}

	/**
	 * @return the index
	 */
	public int getIndex() {
		return index;
	}

	/**
	 * @param index the index to set
	 */
	public void setIndex(int index) {
		this.index = index;
	}

	/**
	 * @return the count
	 */
	public long getCount() {
		return count;
	}

	/**
	 * @param count
	 */
	public void setCount(long count) {
		this.count = count;
	}

	/**
	 * @return the dropTemplate
	 */
	public Drop getDropTemplate() {
		return dropTemplate;
	}

	/**
	 * @return the playerObjId
	 */
	public List<Integer> getPlayerObjIds() {
		return playerObjIds;
	}

	/** 是否可查看掉落物 / Whether view drop item */
	public boolean canViewDropItem(int objectId) {
		return playerObjIds.isEmpty() || playerObjIds.contains(objectId);
	}

	/**
	 * @param playerObjId the playerObjId to set
	 */
	public void setPlayerObjId(int playerObjId) {
		if (playerObjId > 0 && !playerObjIds.contains(playerObjId)) {
			playerObjIds.add(playerObjId);
		}
	}

	/**
	 * @param isFreeForAll to set
	 */
	public void isFreeForAll(boolean isFreeForAll) {
		this.isFreeForAll = isFreeForAll;
	}

	/**
	 * @return isFreeForAll
	 */
	public boolean isFreeForAll() {
		return isFreeForAll;
	}

	/**
	 * @return highestValue
	 */
	public long getHighestValue() {
		return highestValue;
	}

	/**
	 * @param highestValue to set
	 */
	public void setHighestValue(long highestValue) {
		this.highestValue = highestValue;
	}

	/**
	 * @param winningPlayer to set
	 */
	public void setWinningPlayer(Player winningPlayer) {
		this.winningPlayer = winningPlayer;

	}

	/**
	 * @return winningPlayer
	 */
	public Player getWinningPlayer() {
		if (winningPlayer != null && !winningPlayer.isOnline()) {
			Player onlinePlayer = World.getInstance().findPlayer(winningPlayer.getObjectId());
			if (onlinePlayer != null) {
				return onlinePlayer;
			}
		}
		return winningPlayer;
	}

	/**
	 * @param isItemWonNotCollected to set
	 */
	public void isItemWonNotCollected(boolean isItemWonNotCollected) {
		this.isItemWonNotCollected = isItemWonNotCollected;
	}

	/**
	 * @return isItemWonNotCollected
	 */
	public boolean isItemWonNotCollected() {
		return isItemWonNotCollected;
	}

	/**
	 * @param isDistributeItem to set
	 */
	public void isDistributeItem(boolean isDistributeItem) {
		this.isDistributeItem = isDistributeItem;
	}

	/**
	 * @return isDistributeItem
	 */
	public boolean isDistributeItem() {
		return isDistributeItem;
	}

	/** 返回 npc obj / Returns the npc obj */
	public int getNpcObj() {
		return npcObj;
	}

	/** 设置 npc obj / Sets the npc obj */
	public void setNpcObj(int npcObj) {
		this.npcObj = npcObj;
	}

	/** 返回 optional socket / Returns the optional socket */
	public int getOptionalSocket() {
		return optionalSocket;
	}

	/**
	 * @param player 是否仅 possiblelooter。 / Whether only possible looter
	  */
	public boolean isOnlyPossibleLooter(Player player) {
		return playerObjIds.size() == 1 && playerObjIds.contains(player.getObjectId());
	}

	/** 返回 loot effect id / Returns the loot effect id */
	public int getLootEffectId() {
		return switch (dropTemplate.getItemId()) {
			case 166020000, 166020001, 166020002, 166020003 -> 1003;
			case 168000034, 168000035, 168000073, 168000074, 168000117, 168000118, 168000120, 168000121,
					168000161, 168000162, 168000164, 168000165, 168000213, 168000216, 168000223, 168000228,
					168000230, 168000233, 168000240, 168000245 -> 1003;
			case 188053083 -> 1003;
			case 188053547, 188053548, 188053646, 188053647 -> 1002;
			case 190100004, 190100052 -> 1003;
			default -> 0;
		};
	}
}
