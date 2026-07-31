package com.aionemu.gameserver.dao;

import com.aionemu.commons.database.dao.DAO;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphZoneMissionSignalBridge.Signal;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphZoneMissionSignalBridge.SignalClaim;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphZoneMissionSignalBridge.SignalLease;

/** Durable acceptance ledger for cross-owner zone-mission signals. */
public abstract class QuestGraphZoneMissionSignalDAO implements DAO {

	@Override
	public String getClassName() {
		return QuestGraphZoneMissionSignalDAO.class.getName();
	}

	/** Claims a signal for dispatch; an acknowledged signal is already applied. */
	public abstract SignalLease accept(Signal signal);

	/** Acknowledges a signal after its target owner has consumed it. */
	public abstract SignalClaim acknowledge(Signal signal, long claimGeneration);
}
