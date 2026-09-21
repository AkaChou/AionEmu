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
        switch (dropTemplate.getItemId()) {
            case 166020000:
            case 166020001:
            case 166020002:
            case 166020003:
                return 1003;
            case 168000034:
            case 168000035:
            case 168000073:
            case 168000074:
            case 168000117:
            case 168000118:
            case 168000120:
            case 168000121:
            case 168000161:
            case 168000162:
            case 168000164:
            case 168000165:
            case 168000213:
            case 168000216:
            case 168000223:
            case 168000228:
            case 168000230:
            case 168000233:
            case 168000240:
            case 168000245:
                return 1003;
            case 188053083:
                return 1003;
            case 188053547:
            case 188053548:
            case 188053646:
            case 188053647:
                return 1002;
            case 190100004:
            case 190100052:
                return 1003;
            default:
                return 0;
        }
	}
}
