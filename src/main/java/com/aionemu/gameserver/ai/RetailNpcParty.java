package com.aionemu.gameserver.ai;

import com.aionemu.gameserver.model.gameobjects.Npc;

import java.util.List;
import lombok.NoArgsConstructor;
import lombok.AccessLevel;

/**
 * 真实 NPC Party 的显式成员关系。
 * Explicit member relations of a retail NPC party.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class RetailNpcParty {

	public static List<Npc> members(Npc sender) {
		if (sender == null || !sender.isSpawned() || sender.getNpcPartyId() == null) {
			return List.of();
		}
		return sender.getPosition().getWorldMapInstance().getNpcs().stream()
			.filter(candidate -> isMember(sender, candidate))
			.toList();
	}

	public static boolean isMember(Npc sender, Npc candidate) {
		return sender != null && candidate != null && sender.isSpawned() && candidate.isSpawned()
			&& matches(sender.getObjectId(), sender.getInstanceId(), sender.getNpcPartyId(),
				candidate.getObjectId(), candidate.getInstanceId(), candidate.getNpcPartyId());
	}

	static boolean matches(int senderId, int senderInstanceId, String senderPartyId,
			int candidateId, int candidateInstanceId, String candidatePartyId) {
		return senderId != candidateId && senderInstanceId == candidateInstanceId && senderPartyId != null
			&& senderPartyId.equals(candidatePartyId);
	}
}
