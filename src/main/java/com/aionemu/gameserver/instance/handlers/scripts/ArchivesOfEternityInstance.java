package com.aionemu.gameserver.instance.handlers.scripts;

import com.aionemu.gameserver.instance.handlers.GeneralInstanceHandler;
import com.aionemu.gameserver.instance.handlers.InstanceID;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.gameobjects.player.Player;

@InstanceID(301540000)
public class ArchivesOfEternityInstance extends GeneralInstanceHandler {

	private Race spawnRace;

	@Override
	public void onEnterInstance(Player player) {
		player.getController().updateNearbyQuests();
		if (spawnRace != null) {
			return;
		}
		spawnRace = player.getRace();
		spawnRaceBooks();
	}

	private void spawnRaceBooks() {
		int book1 = spawnRace == Race.ASMODIANS ? 703149 : 703131;
		int book2 = spawnRace == Race.ASMODIANS ? 703150 : 703132;
		int book3 = spawnRace == Race.ASMODIANS ? 703151 : 703133;

		spawn(book2, 625.339844f, 500.463898f, 469.338898f, (byte) 0, 133);
		spawn(book2, 619.741150f, 600.201233f, 469.338898f, (byte) 0, 220);
		spawn(book2, 619.315063f, 422.597473f, 469.338898f, (byte) 0, 137);
		spawn(book2, 570.299194f, 525.224304f, 469.338898f, (byte) 0, 229);

		spawn(book3, 527.469543f, 599.944214f, 469.338898f, (byte) 0, 372);
		spawn(book3, 549.448547f, 649.634094f, 469.338898f, (byte) 0, 373);
		spawn(book3, 411.850159f, 569.924133f, 470.323364f, (byte) 0, 371);
		spawn(book3, 480.262695f, 679.539795f, 470.323364f, (byte) 0, 394);

		spawn(book1, 570.225403f, 338.267609f, 469.338898f, (byte) 0, 343);
		spawn(book1, 503.304504f, 454.293396f, 469.338898f, (byte) 0, 268);
		spawn(book1, 394.908081f, 443.110809f, 469.338898f, (byte) 0, 360);
		spawn(book1, 443.361664f, 341.348846f, 469.338898f, (byte) 0, 355);
	}
}
