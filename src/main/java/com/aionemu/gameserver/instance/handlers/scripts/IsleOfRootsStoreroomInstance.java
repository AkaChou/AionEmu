package com.aionemu.gameserver.instance.handlers.scripts;

import com.aionemu.gameserver.instance.handlers.InstanceID;

@InstanceID(300140000)
public class IsleOfRootsStoreroomInstance extends AbyssStoreroomInstance {
	public IsleOfRootsStoreroomInstance() {
		super(new Config(
				215413,
				"Krotan Chamber Artifact",
				"KROTAN_ARTIFACT_CONTROL_ROOM_300140000",
				new int[] { 215130, 215131, 215132, 215133 },
				new int[] { 215135, 215136 },
				new String[] { "Weakened Krotan Lord", "Awakened Krotan Lord" },
				new int[] { 215104, 215116, 215128, 215134 },
				185000056,
				new int[] { 11, 15, 17, 18, 19, 20, 28, 74, 76, 79, 80 }));
	}
}
