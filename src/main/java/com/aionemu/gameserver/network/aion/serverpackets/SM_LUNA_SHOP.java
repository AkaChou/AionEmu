package com.aionemu.gameserver.network.aion.serverpackets;

import java.util.HashMap;
import java.util.Map;

import com.aionemu.gameserver.model.DescriptionId;
import com.aionemu.gameserver.model.dorinerk_wardrobe.PlayerWardrobeEntry;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.templates.item.ItemTemplate;
import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;
import com.aionemu.gameserver.utils.PacketSendUtility;
import lombok.RequiredArgsConstructor;

/**
 * 月之商城（Luna Shop）操作结果的服务端包。
 * Server packet for Luna shop operation results.
 */
@RequiredArgsConstructor
public class SM_LUNA_SHOP extends AionServerPacket {

	private final int actionId;
	private int unk1;
	private int slotSize;
	private int fail;
	private ItemTemplate item;
	// 卡鲁内克的工坊 / Karunerk's Workshop
	private int craftItemId;
	private int craftItemCount;
	// 塔基的冒险 / Taki's Adventure
	private int indun_id;
	// 穆尼伦克的宝藏 / Munirunerk's Treasure
	private HashMap<Integer, Long> munirunerk_treasure;

	private int isApply;
	private int applySlot;
	private int itemId;
	private int itemSize;
	private boolean success;
	private long itemCount;
	// 月之骰子掷骰结果 / Luna dice roll result
	private int rollResult;
	private int diceValue = -1;
	// 月之骰子领奖结果 / Luna dice reward claim result
	private int rewardResult;
	private int rewardCount = -1;

	/**
	 * 卡鲁内克工坊（Karunerk's Workshop）制作结果。
	 * Karunerk's Workshop craft result.
	 * @param actionId action type
	 * @param craftItemId crafted item id
	 * @param craftItemCount crafted item count
	 * @param success whether craft succeeded
	 */
	public SM_LUNA_SHOP(int actionId, int craftItemId, int craftItemCount, boolean success) {
		this.actionId = actionId;
		this.craftItemId = craftItemId;
		this.craftItemCount = craftItemCount;
		this.success = success;
	}

	/**
	 * 塔基冒险（Taki's Adventure）副本相关。
	 * Taki's Adventure instance-related payload.
	 * action type
	 * @param indun_id 副本/实例 ID / instance id
	 */
	public SM_LUNA_SHOP(int actionId, int indun_id) {
		this.actionId = actionId;
		this.indun_id = indun_id;
	}

	/**
	 * 穆尼鲁内克宝藏箱开启结果。
	 * Munirunerk's Treasure chest open result.
	 * @param munirunerk_treasure 奖励物品映射（物品 ID → 数量） / reward map (item id → count)
	 */
	public SM_LUNA_SHOP(HashMap<Integer, Long> munirunerk_treasure) {
		this.actionId = 12;
		this.munirunerk_treasure = munirunerk_treasure;
	}

	/**
	 * 多里内克衣柜（Dorinerk's Wardrobe）外观应用。
	 * Dorinerk's Wardrobe appearance apply.
	 * action type
	 * whether applied
	 * apply slot
	 * item id
	 * @param unk1 未知字段 / unknown field
	 */
	public SM_LUNA_SHOP(int actionId, int isApply, int applySlot, int itemId, int unk1) {
		this.actionId = actionId;
		this.isApply = isApply;
		this.applySlot = applySlot;
		this.itemId = itemId;
		this.unk1 = unk1;
	}

	/**
	 * 衣柜槽位/物品数量同步。
	 * Wardrobe slot and item size sync.
	 * @param actionId action type
	 * @param slotSize slot size
	 * @param itemSize item size
	 */
	public SM_LUNA_SHOP(int actionId, int slotSize, int itemSize) {
		this.actionId = actionId;
		this.slotSize = slotSize;
		this.itemSize = itemSize;
	}

	/**
	 * 物品相关操作结果（含成功/失败标志）。
	 * Item-related operation result with success/fail flag.
	 * action type
	 * @param item 物品模板 / item template
	 * @param fail 失败标志（0 成功 / 1 失败） / fail flag (0 success / 1 fail)
	 */
	public SM_LUNA_SHOP(int actionId, ItemTemplate item, int fail) {
		this.actionId = actionId;
		this.item = item;
		this.fail = fail;
	}

	/**
	 * 物品奖励/展示（含数量）。
	 * Item reward/display payload with count.
	 * @param actionId action type
	 * @param itemId item id
	 * @param itemCount item count
	 */
	public SM_LUNA_SHOP(int actionId, int itemId, long itemCount) {
		this.actionId = actionId;
		this.itemId = itemId;
		this.itemCount = itemCount;
	}

	/**
	 * 月之骰子掷骰结果（真端 User::SendLunaDiceGotchaRollResult 同构载荷）。
	 * Luna dice roll result (mirrors retail User::SendLunaDiceGotchaRollResult payload).
	 * @param rollResult 结果码（0 成功 / 1 其他失败 / 2 露娜不足）/ result code (0 success / 1 other failure / 2 not enough luna)
	 * @param diceValue 骰子值（-1 表示无）/ dice value (-1 for none)
	 * @return 掷骰结果包 / the roll result packet
	 */
	public static SM_LUNA_SHOP diceRollResult(int rollResult, int diceValue) {
		SM_LUNA_SHOP packet = new SM_LUNA_SHOP(15);
		packet.rollResult = rollResult;
		packet.diceValue = diceValue;
		return packet;
	}

	/**
	 * 月之骰子领奖失败结果（真端 result=1 + 空奖励列表）。
	 * Luna dice reward claim failure (retail result=1 with an empty reward list).
	 * @return 领奖失败包 / the claim failure packet
	 */
	public static SM_LUNA_SHOP diceRewardFailure() {
		SM_LUNA_SHOP packet = new SM_LUNA_SHOP(16);
		packet.rewardResult = 1;
		packet.rewardCount = 0;
		return packet;
	}

	@Override
	protected void writeImpl(AionConnection con) {
		Player player = con.getActivePlayer();
		writeC(actionId);
		switch (actionId) {
		case 0:
			writeC(0);
			writeD(indun_id);
			break;
		case 2:
			writeC(fail);
			if (fail == 0) {
				PacketSendUtility.sendPacket(player,
										new SM_SYSTEM_MESSAGE(1330049, new DescriptionId(item.getNameId())));
			} else if (fail == 1) {
				PacketSendUtility.sendPacket(player,
										new SM_SYSTEM_MESSAGE(1330050, new DescriptionId(item.getNameId())));
			}
			break;
		case 3:
			writeC(success ? 0 : 1);// Success = 0 Fail = 1
			writeH(1);// unk 0x01
			writeD(craftItemId);// productid
			writeQ(craftItemCount);// quantity
			break;
		case 4:
			writeC(0);
			break;
		case 5:
			writeC(0);
			writeC(0);
			writeC(0);
			break;
		case 6:
			writeD(53);
			break;
		case 7:
			writeD(55);
			break;
		case 8:// dorinerk'swardrobe
			writeC(0x00);
			writeC(slotSize);
			writeH(itemSize);
			for (PlayerWardrobeEntry ce : player.getWardrobe().getAllWardrobe()) {
				writeC(ce.getSlot());
				writeD(ce.getItemId());
				writeD(0x00);
				writeD(0x01);
			}
			break;
		case 9:
			writeC(0x00);
			writeC(slotSize); // Also possible = writeH(slotSize * 256)
			break;
		case 10:
			writeC(isApply);
			writeC(applySlot);
			writeD(itemId);
			writeD(unk1);
			break;
		case 11:
			writeC(0x00);
			writeC(indun_id);
			writeD(0x01);
			break;
		case 12:// open chest
			writeC(0);// 未知 / unk
			writeH(3);// size always 3
			for (Map.Entry<Integer, Long> e : munirunerk_treasure.entrySet()) {
				writeD(e.getKey());
				writeQ(e.getValue());
			}
			break;
		case 14:
			writeC(1); // free enter = 1
			writeD(indun_id);
			break;
		case 15:
			int dice = diceValue >= 0 ? diceValue : player.getLunaDiceGame();
			writeC(rollResult);
			writeH(dice);
			writeC(0);
			break;
		case 16:
			writeC(rewardResult);
			writeH(rewardCount >= 0 ? rewardCount : 1);
			if (rewardCount != 0) {
				writeD(itemId); // ItemId
				writeQ(itemCount); // Item Count
			}
			break;
		}
	}
}
