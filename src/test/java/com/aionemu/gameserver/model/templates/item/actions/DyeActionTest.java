package com.aionemu.gameserver.model.templates.item.actions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.StringReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import com.aionemu.gameserver.network.aion.AionConnection.State;
import com.aionemu.gameserver.network.aion.clientpackets.CM_USE_ITEM;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

class DyeActionTest {

	@Test
	void parsesCustomDyeAction() throws Exception {
		ActionWrapper wrapper = (ActionWrapper) JAXBContext.newInstance(ActionWrapper.class, DyeAction.class)
			.createUnmarshaller()
			.unmarshal(new StringReader("<actions><dye custom=\"true\"/></actions>"));

		assertTrue(wrapper.dye.isCustom());
		assertEquals(0x112233FF, wrapper.dye.getColor(0x112233FF));
		assertEquals(0, wrapper.dye.getColor());
	}

	@Test
	void versatileDyeStaticDataHasCustomDyeAction() throws Exception {
		String xml = Files.readString(Path.of(
			"src/main/resources/aion/data/static_data/items/item/item_template_152209118_182005538.xml"));
		int itemStart = xml.indexOf("<item_template id=\"169250002\"");
		int nextItem = xml.indexOf("<item_template id=\"169250003\"", itemStart);

		assertTrue(itemStart >= 0);
		assertTrue(xml.substring(itemStart, nextItem).contains("<dye custom=\"true\"/>"));
	}

	@Test
	void useItemTypeSevenReadsTargetAndCustomColor() {
		CM_USE_ITEM packet = new CM_USE_ITEM(0, State.IN_GAME);
		ByteBuffer buffer = ByteBuffer.allocate(13).order(ByteOrder.LITTLE_ENDIAN)
			.putInt(100)
			.put((byte) 7)
			.putInt(200)
			.putInt(0x112233FF);
		buffer.flip();
		packet.setBuffer(buffer);

		assertTrue(packet.read());
		assertEquals(7, packet.type);
		assertEquals(200, packet.targetItemId);
		assertEquals(0x112233FF, packet.customDyeColor);
	}

	@XmlRootElement(name = "actions")
	@XmlAccessorType(XmlAccessType.FIELD)
	private static class ActionWrapper {
		@XmlElement(name = "dye")
		DyeAction dye;
	}
}
