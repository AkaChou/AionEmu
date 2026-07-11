package com.aionemu.gameserver.network.aion.serverpackets;

import com.aionemu.gameserver.model.gameobjects.Item;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;
import com.aionemu.gameserver.world.World;

/**
 * 向客户端播放物品使用动画的服务端包。
 * Server packet that plays an item usage animation on the client.
 */
public class SM_ITEM_USAGE_ANIMATION extends AionServerPacket {
	private int playerObjId;
	private int targetObjId;
	private int itemObjId;
	private int itemId;
	private int time;
	private int end;
	private int unk;

	/**
	 * 构造即时完成的物品使用动画包（目标为自己）。
	 * Creates an immediate item usage animation packet targeting the caster.
	 *
	 * @param playerObjId 使用者对象 ID / caster object id
	 * item object id
	 * item template id
	 */
	public SM_ITEM_USAGE_ANIMATION(int playerObjId, int itemObjId, int itemId) {
		this.playerObjId = playerObjId;
		this.targetObjId = playerObjId;
		this.itemObjId = itemObjId;
		this.itemId = itemId;
		this.time = 0;
		this.end = 1;
		this.unk = 1;
	}

	/**
	 * 构造带读条时长与结束状态的物品使用动画包（目标为自己）。
	 * Creates an item usage animation packet with cast time and end flag targeting the caster.
	 *
	 * @param playerObjId 使用者对象 ID / caster object id
	 * item object id
	 * item template id
	 * @param time 读条时长（毫秒） / cast duration in milliseconds
	 * @param end 结束标志 / end flag
	 */
	public SM_ITEM_USAGE_ANIMATION(int playerObjId, int itemObjId, int itemId, int time, int end) {
		this.playerObjId = playerObjId;
		this.targetObjId = playerObjId;
		this.itemObjId = itemObjId;
		this.itemId = itemId;
		this.time = time;
		this.end = end;
	}

	/**
	 * 构造带读条时长、结束状态与附加标志的物品使用动画包（目标为自己）。
	 * Creates an item usage animation packet with cast time, end flag and extra flag targeting the caster.
	 *
	 * @param playerObjId 使用者对象 ID / caster object id
	 * item object id
	 * item template id
	 * @param time 读条时长（毫秒） / cast duration in milliseconds
	 * @param end 结束标志 / end flag
	 * @param unk 附加未知标志 / extra unknown flag
	 */
	public SM_ITEM_USAGE_ANIMATION(int playerObjId, int itemObjId, int itemId, int time, int end, int unk) {
		this.playerObjId = playerObjId;
		this.targetObjId = playerObjId;
		this.itemObjId = itemObjId;
		this.itemId = itemId;
		this.time = time;
		this.end = end;
		this.unk = unk;
	}

	/**
	 * 构造完整物品使用动画包（可指定目标）。
	 * Creates a full item usage animation packet with an explicit target.
	 *
	 * @param playerObjId 使用者对象 ID / caster object id
	 * target object id
	 * item object id
	 * item template id
	 * @param time 读条时长（毫秒） / cast duration in milliseconds
	 * @param end 结束标志 / end flag
	 * @param unk 附加未知标志 / extra unknown flag
	 */
	public SM_ITEM_USAGE_ANIMATION(int playerObjId, int targetObjId, int itemObjId, int itemId, int time, int end,
			int unk) {
		this.playerObjId = playerObjId;
		this.targetObjId = targetObjId;
		this.itemObjId = itemObjId;
		this.itemId = itemId;
		this.time = time;
		this.end = end;
		this.unk = unk;
	}

	@Override
	protected void writeImpl(AionConnection con) {
		if (time > 0) {
			final Player player = com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().findPlayer(playerObjId);
			final Item item = player.getInventory().getItemByObjId(itemObjId);
			player.setUsingItem(item);
		}
		writeD(playerObjId);
		writeD(targetObjId);
		writeD(itemObjId);
		writeD(itemId);
		writeD(time);
		writeC(end);
		writeC(0);
		writeC(1);
		writeD(unk);
		writeC(0);
	}
}
