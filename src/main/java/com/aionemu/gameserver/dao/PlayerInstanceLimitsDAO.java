package com.aionemu.gameserver.dao;

import com.aionemu.commons.database.dao.DAO;
import com.aionemu.gameserver.model.gameobjects.player.Player;

public abstract class PlayerInstanceLimitsDAO implements DAO {
	@Override
	public final String getClassName() {
		return PlayerInstanceLimitsDAO.class.getName();
	}

	public abstract void load(Player player);

	public abstract void store(Player player);
}
