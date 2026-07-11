package com.aionemu.gameserver.model.templates.housing;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class HousingObjectTypeTest {

	@Test
	void usesThe58HousingFunctionTypeIds() {
		assertEquals(0, new HousingMoveableItem().getTypeId());
		assertEquals(4, new HousingPicture().getTypeId());
		assertEquals(8, new HousingJukeBox().getTypeId());
		assertEquals(9, new HousingMovieJukeBox().getTypeId());
	}
}
