package com.aionemu.gameserver.instance.handlers.scripts;

import com.aionemu.gameserver.instance.handlers.InstanceID;

@InstanceID(300130000)
public class TwilightBattlefieldStoreroomInstance extends AbyssStoreroomInstance {
	public TwilightBattlefieldStoreroomInstance() {
		super(new Config(
				215415,
				"Miren Chamber Artifact",
				"MIREN_ARTIFACT_CONTROL_ROOM_300130000",
				new int[] { 215216, 215217, 215218, 215219 },
				new int[] { 215221, 215222 },
				new String[] { "Weakened Miren Prince", "Awakened Miren Prince" },
				new int[] { 215189, 215200, 215214, 215220 },
				185000066,
				new int[] { 2, 3, 5, 6, 17, 18, 28, 74, 76, 79, 80 }));
	}
}
