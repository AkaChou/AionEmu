package com.aionemu.gameserver.network.aion.serverpackets;


import java.util.Map.Entry;


import com.aionemu.gameserver.configs.administration.AdminConfig;
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
 * summon appearance and state to the client.
 * <p>
 * 根据观察者阵营与关系重算 NPC 类型（可攻击、敌对、和平、不可攻击等），并写入装备、
 * 生命、移动与生成实体等字段。
 * Recalculates NPC type (attackable, aggressive, peace, non-attackable, etc.) based on
 * the observer's faction/relation, and writes equipment, life, movement, and spawn entity fields.
 * Credits To Magenik and to the Cinus
 */
public class SM_NPC_INFO extends AionServerPacket {
	private final Creature _npc;
	private final NpcTemplate npcTemplate;
	private final int npcId;
	/** 隐藏型 NPC 类型：客户端据此不渲染对象（原版模型替换语义）。 / Hidden npc type: the client renders nothing (retail mesh-swap semantics). */
	private static final int HIDDEN_NPC_TYPE_ID = NpcType.INVULNERABLE.getId();
	private int creatorId;
	private String masterName;
	private int npcTypeId;

	/**
	 * 由 NPC 与观察者玩家构造信息包（按关系重算类型）。
	 * Builds an info packet from an NPC and the observing player (type recalculated by relation).
	 * target NPC
	 * @param player 观察者玩家 / observing player
	 */
	public SM_NPC_INFO(Npc npc, Player player) {
		this._npc = npc;
		npcTemplate = npc.getObjectTemplate();
		npcTypeId = npc.getNpcType().getId();
		if (player == null) {
			// 无观察者时保留模板关系类型（用于替换体撤销后原样重发生成者）。
			// Without an observer keep the template relation type (used to re-send a generator verbatim).
		} else if (npc.isPeace()) {
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
	 * 构造“从客户端隐藏”的 NPC 信息包：对象仍留在世界与已知列表，只是客户端不再渲染。
	 * Builds an info packet that hides the NPC on the client: it stays in the world and known list, just unrendered.
	 * @param npc 目标 NPC / target NPC
	 */
	public SM_NPC_INFO(Npc npc) {
		this._npc = npc;
		npcTemplate = npc.getObjectTemplate();
		npcTypeId = HIDDEN_NPC_TYPE_ID;
		npcId = npc.getNpcId();
		creatorId = npc.getCreatorId();
		masterName = npc.getMasterName();
	}

	/**
	 * 由召唤物构造信息包（主人信息写入 creator/masterName）。
	 * Builds an info packet from a summon (master written as creator/masterName).
	 * @param summon summon entity
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
		if (AdminConfig.SHOW_NPC_ID && con.getActivePlayer().isGM()) {
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
			writeF(boundRadius.getHalfFront());
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
			writeF(boundRadius.getHalfFront() + 0.125f + (hasWeapon ? 0.1f : 0f));
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
		writeD(TownService.getInstance().getTownIdByPosition(_npc));
		writeD(0x00);
	}
}
