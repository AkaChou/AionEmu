package com.aionemu.gameserver.network.aion.serverpackets;

import com.aionemu.gameserver.lifecycle.GameHousingServices;

import java.util.Map.Entry;

import org.apache.commons.lang3.StringUtils;

import com.aionemu.gameserver.model.NpcType;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.TribeClass;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.Summon;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.items.ItemSlot;
import com.aionemu.gameserver.model.items.NpcEquippedGear;
import com.aionemu.gameserver.model.templates.BoundRadius;
import com.aionemu.gameserver.model.templates.item.ItemTemplate;
import com.aionemu.gameserver.model.templates.npc.NpcTemplate;
import com.aionemu.gameserver.model.templates.spawns.SpawnTemplate;
import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;
import com.aionemu.gameserver.services.TownService;

/**
 * NPC/召唤物外观与状态信息同步服务端包。
 * summon appearance and state to the client. / summon appearance and state to the client.
 * <p>
 * 根据观察者阵营与关系重算 NPC 类型（可攻击、敌对、和平、不可攻击等），并写入装备、
 * 生命、移动与生成实体等字段。
 * Recalculates NPC type (attackable, aggressive, peace, non-attackable, etc.) based on
 * the observer's faction/relation, and writes equipment, life, movement, and spawn entity fields.
 *
 * Credits To Magenik and to the Cinus
 */
public class SM_NPC_INFO extends AionServerPacket {
	private Creature _npc;
	private NpcTemplate npcTemplate;
	private int npcId;
	private int creatorId;
	private String masterName = StringUtils.EMPTY;
	@SuppressWarnings("unused")
	private float speed = 0.3f;
	private int npcTypeId;

	/**
	 * 由 NPC 与观察者玩家构造信息包（按关系重算类型）。
	 * Builds an info packet from an NPC and the observing player (type recalculated by relation).
	 *
	 * target NPC
	 * @param player 观察者玩家 / observing player
	 */
	public SM_NPC_INFO(Npc npc, Player player) {
		this._npc = npc;
		npcTemplate = npc.getObjectTemplate();
		npcTypeId = npc.getNpcType().getId();
		if (npc.isPeace()) {
			if (npc.getRace().equals(player.getRace()) || (player.getRace().equals(Race.ELYOS)
					&& (npc.getTribe().equals(TribeClass.FIELD_OBJECT_LIGHT)
							|| npc.getTribe().equals(TribeClass.GENERAL))
					|| player.getRace().equals(Race.ASMODIANS) && (npc.getTribe().equals(TribeClass.FIELD_OBJECT_DARK)
							|| npc.getTribe().equals(TribeClass.GENERAL_DARK)))) {
				npcTypeId = NpcType.NON_ATTACKABLE.getId();
			}
		} else if (npc.isFriendTo(player)) {
			npcTypeId = NpcType.NON_ATTACKABLE.getId();
		} else if (npc.isAggressiveTo(player)) {
			npcTypeId = NpcType.AGGRESSIVE.getId();
		} else if (player.isEnemy(npc)) {
			npcTypeId = NpcType.ATTACKABLE.getId();
		} else if (npc.isNoneRelation(player)) {
			npcTypeId = NpcType.PEACE.getId();
		}
		npcId = npc.getNpcId();
		creatorId = npc.getCreatorId();
		masterName = npc.getMasterName();
	}

	/**
	 * 由召唤物构造信息包（主人信息写入 creator/masterName）。
	 * Builds an info packet from a summon (master written as creator/masterName).
	 *
	 * summon entity
	 */
	public SM_NPC_INFO(Summon summon) {
		this._npc = summon;
		npcTemplate = summon.getObjectTemplate();
		npcTypeId = npcTemplate.getNpcType().getId();
		npcId = summon.getNpcId();
		Player owner = summon.getMaster();
		if (owner != null) {
			creatorId = owner.getObjectId();
			masterName = owner.getName();
			speed = owner.getGameStats().getMovementSpeedFloat();
		} else {
			masterName = "LOST";
		}
	}

	@Override
	protected void writeImpl(AionConnection con) {
		writeF(_npc.getX());
		writeF(_npc.getY());
		writeF(_npc.getZ());
		writeD(_npc.getObjectId());
		writeD(npcId);
		writeD(npcId);
		// 修复 / fix
		writeC(npcTypeId);
		if (_npc.getLifeStats().isAlreadyDead()) {
			_npc.setState(39);
		}
		writeH(_npc.getState());
		writeC(_npc.getHeading());
		writeD(npcTemplate.getNameId());
		writeD(npcTemplate.getTitleId());
		writeH(0x00);
		writeC(0x00);
		writeD(0x00);
		writeD(creatorId);
		if (con.getActivePlayer().isGM()) {
			Player gm = con.getActivePlayer();
			if ((gm != null) && (masterName.isEmpty())) {
				masterName = "ID: " + npcId;
			}
		}

		writeS(masterName);
		int maxHp = _npc.getLifeStats().getMaxHp();
		int currHp = _npc.getLifeStats().getCurrentHp();
		writeC((int) (100f * currHp / maxHp));
		writeD(_npc.getGameStats().getMaxHp().getCurrent());
		writeC(_npc.getLevel());
		NpcEquippedGear gear = npcTemplate.getEquipment();
		boolean hasWeapon = false;
		BoundRadius boundRadius = npcTemplate.getBoundRadius();
		if (gear == null) {
			writeH(0x00);
			writeH(0x00);
			writeF(boundRadius.getFront());
		} else {
			writeD(gear.getItemsMask());
			for (Entry<ItemSlot, ItemTemplate> item : gear) {
				if (item.getValue().getWeaponType() != null) {
					hasWeapon = true;
				}
				writeD(item.getValue().getTemplateId());
				writeD(0x00);
				writeD(0x00);
				writeH(0x00);
				writeH(0x00);
			}
			writeF(boundRadius.getFront() + 0.125f + (hasWeapon ? 0.1f : 0f));
		}
		writeF(npcTemplate.getHeight());
		writeF(_npc.getGameStats().getMovementSpeedFloat());
		writeH(npcTemplate.getAttackDelay());
		writeH(npcTemplate.getAttackDelay());
		writeC(_npc.isNewSpawn() ? 0x01 : 0x00);
		writeF(_npc.getMoveController().getTargetX2());
		writeF(_npc.getMoveController().getTargetY2());
		writeF(_npc.getMoveController().getTargetZ2());
		writeC(_npc.getMoveController().getMovementMask());
		SpawnTemplate spawn = _npc.getSpawn();
		if (spawn == null) {
			writeH(0);
		} else {
			writeH(spawn.getEntityId());
		}
		writeC(0);
		writeC(0);
		writeC(0);
		writeC(0);
		writeC(0);
		writeC(0);
		writeC(0);
		writeC(0);
		writeC(_npc.getVisualState());
		writeH(_npc.getNpcObjectType().getId());
		writeC(0x00);
		writeD(_npc.getTarget() == null ? 0 : _npc.getTarget().getObjectId());
		writeD(GameHousingServices.townService().getTownIdByPosition(_npc));
		writeD(0x00);
	}
}
