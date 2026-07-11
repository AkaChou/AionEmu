package com.aionemu.gameserver.model.gameobjects;

import java.util.HashMap;
import java.util.Map;

/**
 * 宠物 Emote 枚举。
 * Pet Emote enumeration.
 *
 * @author ATracer
 */
public enum PetEmote {

	/** 转移停止 / Move Stop*/
	MOVE_STOP(0), MOVETO(12), ALARM(-114), UNK_M110(-110), UNK_M111(-111), UNK_M123(-123), FLY(-125), UNK_M128(-128),
	/** 未知 / Unknown. */
	UNKNOWN(255);

	private static Map<Integer, PetEmote> petEmotes;

	static {
		petEmotes = new HashMap<Integer, PetEmote>();
		for (PetEmote emote : values()) {
			petEmotes.put(emote.getEmoteId(), emote);
		}
	}

	private int emoteId;

	private PetEmote(int emoteId) {
		this.emoteId = emoteId;
	}

	/** 返回 emote id / Returns the emote id */
	public int getEmoteId() {
		return emoteId;
	}

	/** 按 ID 返回 emote / Returns the emote by id */
	public static PetEmote getEmoteById(int emoteId) {
		PetEmote emote = petEmotes.get(emoteId);
		return emote != null ? emote : UNKNOWN;
	}
}
