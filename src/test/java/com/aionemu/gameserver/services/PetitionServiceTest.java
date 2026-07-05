package com.aionemu.gameserver.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.aionemu.gameserver.model.Petition;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentNavigableMap;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.objenesis.ObjenesisStd;

class PetitionServiceTest {

	private final ObjenesisStd objenesis = new ObjenesisStd();
	private SortedMap<Integer, Petition> originalPetitions;

	@AfterEach
	void restorePetitions() throws ReflectiveOperationException {
		if (originalPetitions != null) {
			petitionsField().set(null, originalPetitions);
		}
	}

	@Test
	void registeredPetitionsUseConcurrentSortedMap() throws ReflectiveOperationException {
		assertTrue(petitionsField().get(null) instanceof ConcurrentNavigableMap);
	}

	@Test
	void getRegisteredPetitionsReturnsSnapshot() throws ReflectiveOperationException {
		PetitionService service = objenesis.newInstance(PetitionService.class);
		SortedMap<Integer, Petition> petitions = new TreeMap<Integer, Petition>();
		petitions.put(1, new Petition(1));
		petitions.put(2, new Petition(2));
		replacePetitions(petitions);

		Collection<Petition> snapshot = service.getRegisteredPetitions();
		petitions.clear();

		assertEquals(2, snapshot.size());
	}

	private void replacePetitions(SortedMap<Integer, Petition> petitions) throws ReflectiveOperationException {
		Field field = petitionsField();
		originalPetitions = (SortedMap<Integer, Petition>) field.get(null);
		field.set(null, petitions);
	}

	private static Field petitionsField() throws ReflectiveOperationException {
		Field field = PetitionService.class.getDeclaredField("registeredPetitions");
		field.setAccessible(true);
		return field;
	}
}
