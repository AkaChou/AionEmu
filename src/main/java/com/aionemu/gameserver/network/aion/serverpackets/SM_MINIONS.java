package com.aionemu.gameserver.network.aion.serverpackets;

import java.util.Collection;

import lombok.extern.slf4j.Slf4j;

import com.aionemu.gameserver.model.gameobjects.player.MinionCommonData;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;

/**
 * 随从（Minion）多模式操作服务端包。
 * Multi-mode server packet for minion (companion) operations.
 * <p>
 * action 取值概览：0=列表、1=新增/升级、2=删除、3=重命名、4=锁定、5=召唤、6=收回、
 * 7=成长、8=功能子切换（物品/自动拾取/移动/Buff）、9=激活功能、10=关闭功能、
 * 11=技能点/自动充能、12=自动充能开关、13=测试。
 * action overview: 0=list, 1=add/level-up, 2=delete, 3=rename, 4=lock, 5=spawn, 6=despawn,
 * 7=growth, 8=function sub-switch (item/auto-loot/move/buff), 9=activate function,
 * 10=deactivate function, 11=skill points/auto-charge, 12=auto-charge toggle, 13=test.
 *
 * @author Falke_34, FrozenKiller Reworked by G-Robson26
 */
@Slf4j
public class SM_MINIONS extends AionServerPacket {
	private int action;
	@SuppressWarnings("unused")
	private int expiredTimeMillis;
	private int minionSkillPoints;
	private boolean autoCharge;
	private MinionCommonData commonData;
	private Collection<MinionCommonData> minions;
	private long timeLeft;
	private int subSwitch;
	private int minionObjectId;
	private int ItemId;
	private int slot;
	private int slot2;
	Player player;
	private int addType;
	private boolean isMaterial;
	private boolean isloot;
	private int lootNpcId;

	/**
	 * 仅指定 action 的简单构造。
	 * Simple constructor with action only.
	 *
	 * operation type
	 */
	public SM_MINIONS(int action) {
		this.action = action;
	}

	/**
	 * 技能点与自动充能状态（action=11）。
	 * Skill points and auto-charge state (action=11).
	 *
	 * operation type
	 * @param minionSkillPoints 随从技能点 / minion skill points
	 * @param autoCharge 是否自动充能 / auto-charge enabled
	 */
	public SM_MINIONS(int action, int minionSkillPoints, boolean autoCharge) {
		this.action = action;
		this.minionSkillPoints = minionSkillPoints;
		this.autoCharge = autoCharge;
	}

	/**
	 * 带新增类型的单随从数据（action=1 新增/合成/升级特效）。
	 * Single minion data with add type (action=1 add/combine/level-up effect).
	 *
	 * operation type
	 * @param commonData 随从公共数据 / minion common data
	 * @param addType 新增类型（0=新随从、1=升级、2=合成成功、3=合成失败） / add type
	 */
	public SM_MINIONS(int action, MinionCommonData commonData, int addType) {
		this.action = action;
		this.commonData = commonData;
		this.addType = addType;
	}

	/**
	 * 单随从数据操作（召唤/收回/重命名/锁定/成长等）。
	 * Single-minion data operations (spawn/despawn/rename/lock/growth, etc.).
	 *
	 * operation type
	 * @param commonData 随从公共数据 / minion common data
	 */
	public SM_MINIONS(int action, MinionCommonData commonData) {
		this.action = action;
		this.commonData = commonData;
	}

	/**
	 * 同步全部随从列表（action=0）。
	 * Syncs the full minion list (action=0).
	 *
	 * operation type
	 * minion collection
	 */
	public SM_MINIONS(int action, Collection<MinionCommonData> minions) {
		this.action = action;
		this.minions = minions;
	}

	/**
	 * 功能剩余时间（action=9）。
	 * Function remaining time (action=9).
	 *
	 * operation type
	 * remaining time
	 */
	public SM_MINIONS(int action, long timeLeft) {
		this.action = action;
		this.timeLeft = timeLeft;
	}

	/**
	 * 自动拾取子功能（action=8, subSwitch=1）。
	 * Auto-loot sub-function (action=8, subSwitch=1).
	 *
	 * operation type
	 * @param subSwitch 子切换类型 / sub-switch type
	 * loot npc id
	 * @param isloot 是否启用拾取 / whether looting is enabled
	 */
	public SM_MINIONS(int action, int subSwitch, int lootNpcId, boolean isloot) {
		this.action = action;
		this.subSwitch = subSwitch;
		this.isloot = isloot;
		this.lootNpcId = lootNpcId;
	}

	/**
	 * 功能子切换（物品增删移/Buff 等，action=8）。
	 * Function sub-switch (item add/remove/move/buff, action=8).
	 * <p>
	 * subSwitch 映射：0→0 加物品、1→256 功能、2→512 移动物品、3→768 Buff、4→1 自动拾取。
	 * subSwitch mapping: 0→0 add item, 1→256 function, 2→512 move item, 3→768 buff, 4→1 auto-loot.
	 *
	 * operation type
	 * @param subSwitch 子切换原始值 / raw sub-switch value
	 * minion object id
	 * item template id
	 * slot
	 * @param slot2 目标槽位 / target slot
	 */
	public SM_MINIONS(int action, int subSwitch, int minionObjectId, int ItemId, int slot, int slot2) {
		this.action = action;
		switch (subSwitch) {
		case 0: {
			this.subSwitch = 0;
			this.minionObjectId = minionObjectId;
			this.ItemId = ItemId;
			this.slot = slot;
			break;
		}
		case 2: {
			this.subSwitch = 512;
			this.minionObjectId = minionObjectId;
			this.slot = slot;
			this.slot2 = slot2;
			break;
		}
		case 1: {
			this.subSwitch = 256;
			this.minionObjectId = minionObjectId;
			this.slot = slot;
			break;
		}
		case 3: {
			this.subSwitch = 768;
			this.minionObjectId = minionObjectId;
			this.ItemId = ItemId;
			this.slot = slot;
			break;
		}
		case 4: {
			this.subSwitch = 1;
		}
		}
	}

	/**
	 * 删除随从（可选作为材料，action=2）。
	 * Deletes a minion (optionally as material, action=2).
	 *
	 * operation type
	 * @param isMaterial 是否作为材料删除 / whether deleted as material
	 * @param commonData 随从公共数据 / minion common data
	 */
	public SM_MINIONS(int action, boolean isMaterial, MinionCommonData commonData) {
		this.action = action;
		this.isMaterial = isMaterial;
		this.commonData = commonData;
	}

	@Override
	protected void writeImpl(AionConnection con) {
		writeH(action);
		switch (action) {
		case 0: {
			writeC(0);
			if (minions == null) {
				writeH(0);
				break;
			}
			writeH(minions.size());
			for (MinionCommonData commonData : minions) {
				writeD(commonData.getObjectId());
				writeD(commonData.getMinionId());
				writeD(0);
				writeD(commonData.getMasterObjectId());
				writeD(commonData.getMinionId());
				writeS(commonData.getName());
				writeD(commonData.getBirthday());
				writeD(0);
				writeD(commonData.getMinionGrowthPoint());
				writeC(commonData.isLock() ? 1 : 0);
				for (int slot = 0; slot < 6; slot++) {
					writeD(commonData.getDopingBag() == null ? 0 : commonData.getDopingBag().getItem(slot));
				}
				writeC(0);
			}
			break;
		}
		case 1: {
			if (commonData == null) {
				return;
			}
			writeD(addType);// 3 nem siker combination, 2 siker combination, 1 levelup, 0 new minion
							// （效果） / (effect)
			writeD(0);
			writeH(0);
			writeD(commonData.getObjectId());
			writeD(commonData.getMinionId());
			writeD(0);
			writeD(commonData.getMasterObjectId());
			writeD(commonData.getMinionId());
			writeS(commonData.getName());
			writeD(commonData.getBirthday());
			writeB(new byte[34]);
			break;
		}
		case 2: {// delete
			if (commonData == null) {
				return;
			}
			writeH(isMaterial ? 1 : 0);
			writeD(commonData.getObjectId());
			break;
		}
		case 3: {// rename
			if (commonData == null) {
				return;
			}
			writeD(commonData.getObjectId());
			writeS(commonData.getName());
			break;
		}
		case 4: {// lock
			if (commonData == null) {
				return;
			}
			writeD(commonData.getObjectId());
			writeC(commonData.isLock() ? 1 : 0);
			break;
		}
		case 5: {// spawn
			if (commonData == null) {
				return;
			}
			writeS(commonData.getName());
			writeD(commonData.getObjectId());
			writeD(commonData.getMinionId());
			writeD(commonData.getMasterObjectId());
			break;
		}
		case 6: {// despawn
			if (commonData == null) {
				return;
			}
			writeD(commonData.getObjectId());
			// if (player != null && player.getLifeStats().isAlreadyDead()) {
			// writeC(0);
			// break;
			// }
			writeC(21);
			break;
		}
		case 7: {// growthUp alpha
			if (commonData == null) {
				return;
			}
			writeD(commonData.getObjectId());
			writeD(commonData.getMinionGrowthPoint());
			break;
		}
		case 8: {
			writeH(subSwitch);
			log.debug("SM_MINIONS subSwitch={}", subSwitch);
			switch (subSwitch) {
			case 0: { // Add item
				writeD(minionObjectId);
				writeD(ItemId);
				writeD(slot);
				log.debug("SM_MINIONS add item. minionObjectId={} itemId={} slot={} slot2={}", minionObjectId, ItemId,
						slot, slot2);
				break;
			}
			case 1: {// Auto Loot
				if (lootNpcId != 0) {
					writeC(isloot ? 1 : 2); // 0x02 display looted msg.
					writeD(lootNpcId);
				} else {
					writeC(isloot ? 1 : 0);
				}
				break;
			}
			case 256: {
				writeD(minionObjectId);
				writeD(slot);
				log.debug("SM_MINIONS function. minionObjectId={} itemId={} slot={} slot2={}", minionObjectId, ItemId,
						slot, slot2);
				break;
			}
			case 512: {
				writeD(minionObjectId);
				writeD(slot);
				writeD(slot2);
				log.debug("SM_MINIONS move item. minionObjectId={} itemId={} slot={} slot2={}", minionObjectId, ItemId,
						slot, slot2);
				break;
			}
			case 768: {// BUFF
				writeD(minionObjectId);
				writeD(ItemId);
				// writeD(slot);
				break;
			}
			}
			break;
		}
		case 9: { // activate minion function warning
			writeD((int) timeLeft);
			writeD(1);
			break;
		}
		case 10: // Deaktivate Miol funktion Warn
			writeC(0);
			break;
		case 11: {
			writeD(minionSkillPoints); // Minion SkillPoints
			writeH(autoCharge ? 1 : 0); // Auto Recharge ?
			break;
		}
		case 12: { // Miol funktion Warn AutoCharge (1 = ON 0 = OFF)
			writeD(0);
			break;
		}
		case 13: { // Test!
			writeD(319480);
			writeD(319480);
			writeD(319480);
			writeD(319480);
			writeD(319480);
			writeD(319480);
			writeD(319480);
			writeD(319480);
			writeD(319480);
			writeD(319480);
			writeD(319480);
			writeD(319480);
			break;
		}
		}
	}
}
