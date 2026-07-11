package com.aionemu.gameserver.model.gameobjects;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Field;

import org.junit.jupiter.api.Test;

import com.aionemu.gameserver.model.templates.item.ItemTemplate;

class ItemTest {

	@Test
	void usesTheTemplateNameAsTheObjectName() throws ReflectiveOperationException {
		ItemTemplate template = new ItemTemplate();
		Field name = ItemTemplate.class.getDeclaredField("name");
		name.setAccessible(true);
		name.set(template, "Test Item");

		assertEquals("Test Item", new Item(1, template).getName());
	}
}
