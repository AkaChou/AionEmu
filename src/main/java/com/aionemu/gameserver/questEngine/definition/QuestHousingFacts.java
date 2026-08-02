package com.aionemu.gameserver.questEngine.definition;

/** Immutable ownership and location facts for one house-object interaction. */
public record QuestHousingFacts(int playerId, int houseObjectId, int houseOwnerId,
		int houseAddressId, int houseWorldId, int houseInstanceId, int itemTemplateId, int itemObjectId,
		boolean ownerMatch, boolean houseLoaded) {
	public QuestHousingFacts {
		positive(playerId, "playerId");
		positive(houseObjectId, "houseObjectId");
		positive(houseOwnerId, "houseOwnerId");
		positive(houseAddressId, "houseAddressId");
		positive(houseWorldId, "houseWorldId");
		positive(houseInstanceId, "houseInstanceId");
		positive(itemTemplateId, "itemTemplateId");
		if (itemObjectId < 0) throw new IllegalArgumentException("itemObjectId must be non-negative");
		if (!houseLoaded) throw new IllegalArgumentException("house must be loaded");
		if (!ownerMatch || playerId != houseOwnerId) {
			throw new IllegalArgumentException("house ownership fact does not match player");
		}
	}

	private static void positive(int value, String field) {
		if (value <= 0) throw new IllegalArgumentException(field + " must be positive");
	}
}
