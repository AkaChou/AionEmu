package com.aionemu.gameserver.instance.handlers.scripts;

import com.aionemu.gameserver.instance.handlers.InstanceID;

@InstanceID(300120000)
public class GraveOfSteelStoreroomInstance extends AbyssStoreroomInstance {
	public GraveOfSteelStoreroomInstance() {
		super(new Config(
				215414,
				"Kysis Chamber Artifact",
				"KYSIS_ARTIFACT_CONTROL_ROOM_300120000",
				new int[] { 215173, 215174, 215175, 215176 },
				new int[] { 215178, 215179 },
				new String[] { "Weakened Kysis Duke", "Awakened Kysis Duke" },
				new int[] { 215147, 215159, 215172, 215177 },
				185000061,
				new int[] { 11, 15, 17, 18, 19, 20, 28, 74, 76, 79, 80 }));
	}
}
