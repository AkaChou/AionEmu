package com.aionemu.gameserver.network.aion.clientpackets;

import com.aionemu.commons.database.dao.DAOManager;
import com.aionemu.gameserver.dao.PlayerEquipmentSettingDAO;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.gameobjects.player.equipmentsetting.EquipmentSetting;
import com.aionemu.gameserver.network.aion.AionClientPacket;
import com.aionemu.gameserver.network.aion.AionConnection.State;

public class CM_EQUIPMENT_SETTING_SAVE extends AionClientPacket {

	private int slot;
	private int displayType;
	private int mHand;
	private int sHand;
	private int helmet;
	private int torso;
	private int glove;
	private int boots;
	private int earringsLeft;
	private int earringsRight;
	private int ringLeft;
	private int ringRight;
	private int necklace;
	private int shoulder;
	private int pants;
	private int powershardLeft;
	private int powershardRight;
	private int wings;
	private int waist;
	private int mOffHand;
	private int sOffHand;
	private int plume;
	private int bracelet;

	public CM_EQUIPMENT_SETTING_SAVE(int opcode, State state, State... restStates) {
		super(opcode, state, restStates);
	}

	@Override
	protected void readImpl() {
		slot = readD();
		displayType = readD();
		mHand = readD();
		sHand = readD();
		helmet = readD();
		torso = readD();
		glove = readD();
		boots = readD();
		earringsLeft = readD();
		earringsRight = readD();
		ringLeft = readD();
		ringRight = readD();
		necklace = readD();
		shoulder = readD();
		pants = readD();
		powershardLeft = readD();
		powershardRight = readD();
		wings = readD();
		waist = readD();
		mOffHand = readD();
		sOffHand = readD();
		plume = readD();
		readD();
		bracelet = readD();
		readD();
	}

	@Override
	protected void runImpl() {
		Player player = getConnection().getActivePlayer();
		if (player == null || !player.isSpawned()) {
			return;
		}
		EquipmentSetting setting = player.getEquipmentSettingList().add(slot, displayType, mHand, sHand, helmet, torso,
				glove, boots, earringsLeft, earringsRight, ringLeft, ringRight, necklace, shoulder, pants,
				powershardLeft, powershardRight, wings, waist, mOffHand, sOffHand, plume, bracelet, true);
		DAOManager.getDAO(PlayerEquipmentSettingDAO.class).insertEquipmentSetting(player, setting);
	}
}
