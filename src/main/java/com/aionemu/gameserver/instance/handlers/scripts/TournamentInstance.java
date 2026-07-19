package com.aionemu.gameserver.instance.handlers.scripts;

import com.aionemu.gameserver.instance.handlers.GeneralInstanceHandler;
import com.aionemu.gameserver.instance.handlers.InstanceID;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.services.instance.TournamentService;
import com.aionemu.gameserver.world.WorldMapInstance;

@InstanceID({ 900230000, 900210000, 302320000, 302310000, 302370000, 302360000, 302390000, 302380000,
		302420000, 302410000 })
public class TournamentInstance extends GeneralInstanceHandler {
	@Override
	public void onInstanceCreate(WorldMapInstance instance) {
		super.onInstanceCreate(instance);
		TournamentService.attachInstance(instance);
	}

	@Override
	public void onInstanceDestroy() {
		TournamentService.onInstanceDestroy(instance);
	}

	@Override
	public void onPlayerLogin(Player player) {
		TournamentService.onEnterInstance(instance, player);
	}

	@Override
	public void onEnterInstance(Player player) {
		TournamentService.onEnterInstance(instance, player);
	}

	@Override
	public boolean onDie(Player player, Creature lastAttacker) {
		return TournamentService.onDie(instance, player, lastAttacker);
	}

	@Override
	public boolean onReviveEvent(Player player) {
		return TournamentService.onRevive(instance, player);
	}
}
