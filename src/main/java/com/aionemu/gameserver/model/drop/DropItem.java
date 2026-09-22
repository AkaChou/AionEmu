package com.aionemu.gameserver.model.drop;

import java.util.ArrayList;
import java.util.List;

import com.aionemu.commons.utils.Rnd;
import com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.templates.item.ItemTemplate;
import lombok.Getter;
import lombok.Setter;

/**
 * 掉落物品模型。
 * Drop Item model.
 *
 * @author ATracer
 */
@Getter
@Setter
public class DropItem {

	/**
	 * @return 索引 / the index
	 */
	private int index = 0;
	/**
	 * @return 数量 / the count
	 */
	private long count = 0;
	/**
	 * @return 掉落模板 / the dropTemplate
	 */
	private final Drop dropTemplate;
	/**
	 * @return 玩家对象 ID 列表 / the playerObjId
	 */
	private final List<Integer> playerObjIds = new ArrayList<>();
	/**
	 * @return 是否自由拾取 / whether free for all
	 */
	private boolean isFreeForAll = false;
	/**
	 * @return 最高出价 / highestValue
	 */
	private long highestValue = 0;
	/**
	 * @param winningPlayer 要设置的中奖玩家 / the winningPlayer to set
	 */
	private Player winningPlayer = null;
	/**
	 * @return 是否已中奖但未拾取 / whether won but not collected
	 */
	private boolean isItemWonNotCollected = false;
	/**
	 * @return 是否分配物品 / whether to distribute the item
	 */
	private boolean isDistributeItem = false;
	/** 返回 npc obj / Returns the npc obj */
	private int npcObj;

	/** 返回 optional socket / Returns the optional socket */
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
	 * 按掉落模板的数量范围重新生成物品数量。
	 * Regenerates item count from the drop template range.
	 */
	public void calculateCount() {
		count = Rnd.get(dropTemplate.getMinAmount(), dropTemplate.getMaxAmount());
	}

	/** 是否可查看掉落物 / Whether view drop item */
	public boolean canViewDropItem(int objectId) {
		return playerObjIds.isEmpty() || playerObjIds.contains(objectId);
	}

	/**
	 * @param playerObjId 要设置的玩家对象 ID / the playerObjId to set
	 */
	public void setPlayerObjId(int playerObjId) {
		if (playerObjId > 0 && !playerObjIds.contains(playerObjId)) {
			playerObjIds.add(playerObjId);
		}
	}

	/**
	 * @param isFreeForAll 是否自由拾取 / whether free for all
	 */
	public void isFreeForAll(boolean isFreeForAll) {
		this.isFreeForAll = isFreeForAll;
	}

	/**
	 * @return 中奖玩家 / the winningPlayer
	 */
	public Player getWinningPlayer() {
		if (winningPlayer != null && !winningPlayer.isOnline()) {
			Player onlinePlayer = GameWorldBootstrapServices.world().findPlayer(winningPlayer.getObjectId());
			if (onlinePlayer != null) {
				return onlinePlayer;
			}
		}
		return winningPlayer;
	}

	/**
	 * @param isItemWonNotCollected 是否已中奖但未拾取 / whether won but not collected
	 */
	public void isItemWonNotCollected(boolean isItemWonNotCollected) {
		this.isItemWonNotCollected = isItemWonNotCollected;
	}

	/**
	 * @param isDistributeItem 是否分配物品 / whether to distribute the item
	 */
	public void isDistributeItem(boolean isDistributeItem) {
		this.isDistributeItem = isDistributeItem;
	}

	/**
	 * 是否只有该玩家是可能的拾取者（单人归属的掉落）。
	 * Whether only this player is a possible looter (solo-owned drops).
	 *
	 * @param player 玩家 / player
	 * @return 是否仅该玩家可拾取 / whether only this player may loot
	  */
	public boolean isOnlyPossibleLooter(Player player) {
		return playerObjIds.size() == 1 && playerObjIds.contains(player.getObjectId());
	}

	/** 返回 loot effect id / Returns the loot effect id */
	public int getLootEffectId() {
        return switch (dropTemplate.getItemId()) {
            case 166020000, 166020001, 166020002, 166020003 -> 1003;
            case 168000034, 168000035, 168000073, 168000074, 168000117, 168000118, 168000120, 168000121, 168000161,
                 168000162, 168000164, 168000165, 168000213, 168000216, 168000223, 168000228, 168000230, 168000233,
                 168000240, 168000245 -> 1003;
            case 188053083 -> 1003;
            case 188053547, 188053548, 188053646, 188053647 -> 1002;
            case 190100004, 190100052 -> 1003;
            default -> 0;
        };
	}
}
