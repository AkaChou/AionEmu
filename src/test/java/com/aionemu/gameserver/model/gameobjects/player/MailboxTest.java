package com.aionemu.gameserver.model.gameobjects.player;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.aionemu.gameserver.model.gameobjects.Letter;
import com.aionemu.gameserver.model.gameobjects.LetterType;
import java.lang.reflect.Field;
import java.sql.Timestamp;
import java.util.Map;
import org.junit.jupiter.api.Test;

class MailboxTest {

	@Test
	void uploadReserveLettersPromotesOnlyFreeSlotsWithoutConcurrentModification() throws ReflectiveOperationException {
		Mailbox mailbox = new Mailbox(null);
		for (int i = 1; i <= 100; i++) {
			mailbox.putLetterToMailbox(letter(i));
		}
		mailbox.putLetterToMailbox(letter(201));
		mailbox.putLetterToMailbox(letter(202));

		assertDoesNotThrow(() -> mailbox.removeLetter(1));
		assertEquals(100, mailbox.size());
		assertNotNull(mailbox.getLetterFromMailbox(201));
		assertEquals(1, reserveMail(mailbox).size());
	}

	private static Letter letter(int objectId) {
		return new Letter(objectId, 1, null, 0, 0, "title", "message", "sender",
				new Timestamp(objectId), true, LetterType.NORMAL);
	}

	@SuppressWarnings("unchecked")
	private static Map<Integer, Letter> reserveMail(Mailbox mailbox) throws ReflectiveOperationException {
		Field field = Mailbox.class.getDeclaredField("reserveMail");
		field.setAccessible(true);
		return (Map<Integer, Letter>) field.get(mailbox);
	}
}
