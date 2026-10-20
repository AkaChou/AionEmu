package com.aionemu.commons.utils.xml;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.StringReader;
import java.util.List;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElementWrapper;
import jakarta.xml.bind.annotation.XmlElements;
import jakarta.xml.bind.annotation.XmlList;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.adapters.XmlAdapter;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import org.junit.jupiter.api.Test;

import com.aionemu.gameserver.dataholders.ItemSetData;
import com.aionemu.gameserver.skillengine.model.SkillTemplate;

class StreamingXmlBinderTest {

	@Test
	void bindsAttributesCollectionsPolymorphicElementsAndXmlList() {
		Root root = StreamingXmlBinder.unmarshal(new StringReader("""
			<root name="sample" ids="3 5,8">
				<add name="MAXHP" value="10"/>
				<rate name="SPEED" value="20"/>
				<child value="7"/>
			</root>
			"""), Root.class);

		assertEquals("sample", root.name);
		assertEquals(List.of(3, 5, 8), root.ids);
		assertInstanceOf(AddModifier.class, root.modifiers.get(0));
		assertInstanceOf(RateModifier.class, root.modifiers.get(1));
		assertEquals("SPEED", root.modifiers.get(1).name);
		assertEquals(7, root.child.value);
		assertTrue(root.afterUnmarshalCalled);
	}

	@Test
	void appliesTypeAdapterAndRejectsInvalidScalarValue() {
		AdaptedRoot root = StreamingXmlBinder.unmarshal(
			new StringReader("<adapted><wrapped>42</wrapped></adapted>"), AdaptedRoot.class);

		assertEquals(42, root.value.number);

		IllegalStateException failure = assertThrows(IllegalStateException.class,
			() -> StreamingXmlBinder.unmarshal(new StringReader("<root><child value=\"bad\"/></root>"), Root.class));
		assertTrue(failure.getCause() instanceof NumberFormatException);
	}

	@Test
	void bindsDirectSkillPropertiesNode() {
		SkillTemplate template = StreamingXmlBinder.unmarshal(new StringReader("""
			<skill_template skill_id="1" name="test" nameId="2" skilltype="MAGICAL"
				skillsubtype="BUFF" activation="ACTIVE" duration="0">
				<properties first_target="ME" first_target_range="1" target_relation="FRIEND"
					target_type="ONLYONE"/>
			</skill_template>
			"""), SkillTemplate.class);

		assertEquals(1, template.getProperties().getFirstTargetRange());
	}

	@Test
	void ignoresNestedRootAnnotationWhenBindingChildElement() {
		ItemSetData data = StreamingXmlBinder.unmarshal(new StringReader("""
			<item_sets>
			  <itemset id="1">
			    <itempart itemid="100900544"/>
			  </itemset>
			</item_sets>
			"""), ItemSetData.class);

		assertEquals(1, data.size());
	}

	@Test
	void bindsWrappedCollection() {
		WrappedRoot root = StreamingXmlBinder.unmarshal(new StringReader("""
			<wrapped><items><item value="1"/><item value="2"/></items></wrapped>
			"""), WrappedRoot.class);

		assertEquals(2, root.items.size());
		assertEquals(2, root.items.get(1).value);
	}

	@XmlRootElement(name = "root")
	@XmlAccessorType(XmlAccessType.FIELD)
	static class Root {
		@XmlAttribute
		private String name;
		@XmlAttribute
		@XmlList
		private List<Integer> ids;
		@XmlElements({
			@XmlElement(name = "add", type = AddModifier.class),
			@XmlElement(name = "rate", type = RateModifier.class)
		})
		private List<Modifier> modifiers;
		@XmlElement
		private Child child;
		private boolean afterUnmarshalCalled;

		@SuppressWarnings("unused")
		private void afterUnmarshal(Object unmarshaller, Object parent) {
			afterUnmarshalCalled = true;
		}
	}

	@XmlAccessorType(XmlAccessType.FIELD)
	static class Modifier {
		@XmlAttribute
		protected String name;
		@XmlAttribute
		protected int value;
	}

	@XmlAccessorType(XmlAccessType.FIELD)
	static class AddModifier extends Modifier {
	}

	@XmlAccessorType(XmlAccessType.FIELD)
	static class RateModifier extends Modifier {
	}

	@XmlAccessorType(XmlAccessType.FIELD)
	static class Child {
		@XmlAttribute
		private int value;
	}

	@XmlRootElement(name = "wrapped")
	@XmlAccessorType(XmlAccessType.FIELD)
	static class WrappedRoot {
		@XmlElementWrapper(name = "items")
		@XmlElement(name = "item")
		private List<Child> items;
	}

	@XmlRootElement(name = "adapted")
	@XmlAccessorType(XmlAccessType.FIELD)
	static class AdaptedRoot {
		@XmlElement(name = "wrapped")
		@XmlJavaTypeAdapter(NumberAdapter.class)
		private Number value;
	}

	static class Number {
		private final int number;

		private Number(int number) {
			this.number = number;
		}
	}

	static class NumberAdapter extends XmlAdapter<String, Number> {
		@Override
		public Number unmarshal(String value) {
			return new Number(Integer.parseInt(value));
		}

		@Override
		public String marshal(Number value) {
			return Integer.toString(value.number);
		}
	}

}
