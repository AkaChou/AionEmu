package com.aionemu.gameserver.dao;

import java.util.List;

import com.aionemu.commons.database.dao.DAO;
import com.aionemu.gameserver.model.templates.rewards.RewardEntryItem;

import java.util.ArrayList;
import java.util.List;

public abstract class RewardServiceDAO implements DAO {
	@Override
	public final String getClassName() {
		return RewardServiceDAO.class.getName();
	}

	public abstract List<RewardEntryItem> getAvailable(int playerId);

	public abstract void uncheckAvailable(List<Integer> ids);

	public abstract void setUpdateDown(int unique);

	public abstract boolean setUpdate(int unique);
}
