package com.aionemu.gameserver.questEngine.runtime;

import com.aionemu.gameserver.model.house.House;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.definition.QuestEvent;
import com.aionemu.gameserver.questEngine.definition.QuestHousingFacts;
import com.aionemu.gameserver.questEngine.model.QuestEnv;

import java.util.Objects;
import java.util.function.Function;

/** Captures the active-house ownership boundary for a house item event. */
public final class PlayerQuestHousingEventPort implements QuestHousingEventPort {
	private final Function<Player, House> activeHouse;

	public PlayerQuestHousingEventPort() {
		this(Player::getActiveHouse);
	}

	PlayerQuestHousingEventPort(Function<Player, House> activeHouse) {
		this.activeHouse = Objects.requireNonNull(activeHouse, "activeHouse");
	}

	@Override
	public QuestEvent.HouseItemUse houseItemUse(QuestEnv env, int itemTemplateId) {
		return houseItemUse(env, itemTemplateId, 0);
	}

	@Override
	public QuestEvent.HouseItemUse houseItemUse(QuestEnv env, int itemTemplateId, int itemObjectId) {
		if (itemTemplateId <= 0) throw new IllegalArgumentException("itemTemplateId must be positive");
		if (itemObjectId < 0) throw new IllegalArgumentException("itemObjectId must be non-negative");
		if (env == null || env.getPlayer() == null) throw new IllegalArgumentException("housing player is required");
		Player player = env.getPlayer();
		House house = activeHouse.apply(player);
		if (house == null || house.getAddress() == null || house.getPosition() == null) {
			throw new IllegalStateException("active house is unavailable");
		}
		int ownerId = house.getOwnerId();
		boolean ownerMatch = ownerId == player.getObjectId();
		if (!ownerMatch) throw new IllegalArgumentException("house owner does not match event player");
		QuestHousingFacts facts = new QuestHousingFacts(player.getObjectId(), house.getObjectId(), ownerId,
			house.getAddress().getId(), house.getWorldId(), house.getInstanceId(), itemTemplateId, itemObjectId, true, true);
		return new QuestEvent.HouseItemUse(itemTemplateId, facts);
	}
}
