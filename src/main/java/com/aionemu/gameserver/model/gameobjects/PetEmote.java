package com.aionemu.gameserver.model.gameobjects;

import java.util.HashMap;
import java.util.Map;
import lombok.Getter;

/**
 * 宠物 Emote 枚举。
 * Pet Emote enumeration.
 *
 * @author ATracer
 */
@Getter
public enum PetEmote {

	/** 停止移动 / Move Stop. */
	MOVE_STOP(0), MOVETO(12), ALARM(-114), UNK_M110(-110), UNK_M111(-111), UNK_M123(-123), FLY(-125), UNK_M128(-128),
	/** 未知 / Unknown. */
	UNKNOWN(255);

	private static final Map<Integer, PetEmote> petEmotes;

	static {
		petEmotes = new HashMap<>();
		for (PetEmote emote : values()) {
			petEmotes.put(emote.getEmoteId(), emote);
		}
	}

	/** 返回表情 ID / Returns the emote id */
	private final int emoteId;

	PetEmote(int emoteId) {
		this.emoteId = emoteId;
	}

	/** 按 ID 返回表情 / Returns the emote by id */
	public static PetEmote getEmoteById(int emoteId) {
		PetEmote emote = petEmotes.get(emoteId);
		return emote != null ? emote : UNKNOWN;
	}
}
