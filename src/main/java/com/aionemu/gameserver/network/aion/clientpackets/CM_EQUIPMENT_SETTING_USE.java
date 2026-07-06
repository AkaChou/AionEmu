package com.aionemu.gameserver.network.aion.clientpackets;

import java.util.ArrayList;
import java.util.List;

import com.aionemu.gameserver.model.TaskId;
import com.aionemu.gameserver.model.gameobjects.Item;
import com.aionemu.gameserver.model.gameobjects.player.Equipment;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.AionClientPacket;
import com.aionemu.gameserver.network.aion.AionConnection.State;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.network.aion.serverpackets.SM_UPDATE_PLAYER_APPEARANCE;
import com.aionemu.gameserver.restrictions.RestrictionsManager;
import com.aionemu.gameserver.skillengine.effect.AbnormalState;
import com.aionemu.gameserver.utils.PacketSendUtility;

public class CM_EQUIPMENT_SETTING_USE extends AionClientPacket {

	private final List<EquipmentSettingUseAction> actions = new ArrayList<EquipmentSettingUseAction>();

	public CM_EQUIPMENT_SETTING_USE(int opcode, State state, State... restStates) {
		super(opcode, state, restStates);
	}

	@Override
	protected void readImpl() {
		actions.clear();
		int size = readH();
		for (int i = 0; i < size; i++) {
			int action = readD();
			long slot = readD() & 0xFFFFFFFFL;
			readD();
			int itemObjectId = readD();
			actions.add(new EquipmentSettingUseAction(action, slot, itemObjectId));
		}
	}

	@Override
	protected void runImpl() {
		Player activePlayer = getConnection().getActivePlayer();
		if (activePlayer == null) {
			return;
		}
		activePlayer.getController().cancelUseItem();
		if (!activePlayer.isSpawned() || activePlayer.getController().isInShutdownProgress()) {
			return;
		}
		if (!RestrictionsManager.canChangeEquip(activePlayer)) {
			return;
		}
		if (activePlayer.getEffectController().isAbnormalState(AbnormalState.CANT_ATTACK_STATE)) {
			PacketSendUtility.sendPacket(activePlayer, SM_SYSTEM_MESSAGE.STR_SKILL_CAN_NOT_ACT_WHILE_IN_ABNORMAL_STATE);
			return;
		}

		Equipment equipment = activePlayer.getEquipment();
		EquipmentSettingUseTarget target = new EquipmentTarget(activePlayer, equipment);
		boolean changed = EquipmentSettingUseAction.applyAll(actions, target);

		if (changed) {
			PacketSendUtility.sendPacket(activePlayer, new SM_SYSTEM_MESSAGE(1404124, new Object[0]));
			PacketSendUtility.broadcastPacket(activePlayer,
					new SM_UPDATE_PLAYER_APPEARANCE(activePlayer.getObjectId(), equipment.getEquippedForApparence()),
					true);
		}
	}

	private static class EquipmentTarget implements EquipmentSettingUseTarget {
		private final Player player;
		private final Equipment equipment;

		private EquipmentTarget(Player player, Equipment equipment) {
			this.player = player;
			this.equipment = equipment;
		}

		@Override
		public boolean equipItem(int itemObjectId, long slot) {
			return equipment.equipItem(itemObjectId, slot) != null;
		}

		@Override
		public boolean unEquipItem(int itemObjectId, long slot) {
			return equipment.unEquipItem(itemObjectId, slot) != null;
		}

		@Override
		public boolean canSwitchHands() {
			return !player.getController().hasTask(TaskId.ITEM_USE) || player.getController().getTask(TaskId.ITEM_USE).isDone();
		}

		@Override
		public boolean switchHands() {
			if (!canSwitchHands()) {
				PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_CANT_EQUIP_ITEM_IN_ACTION);
				return false;
			}
			if (player.getController().isUnderStance()) {
				player.getController().stopStance();
			}
			equipment.switchHands();
			return true;
		}

		@Override
		public long getEquippedSlot(int itemObjectId) {
			Item item = equipment.getEquippedItemByObjId(itemObjectId);
			return item == null ? 0 : item.getEquipmentSlot();
		}
	}
}
