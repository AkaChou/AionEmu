package com.aionemu.gameserver.network.aion.serverpackets;

import java.util.ArrayList;
import java.util.List;

import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;

/**
 * 自定义调试发包：由管理员 //fsc 命令按元素类型动态组装的任意服务端包。
 * Custom admin debug packet assembled dynamically by //fsc from typed elements.
 *
 * @author Luno
 */
public class SM_CUSTOM_PACKET extends AionServerPacket {

	/**
	 * 包元素类型枚举，按单字符编码写入对应二进制字段。
	 * Packet element types; each code writes the matching binary field.
	 */
	public static enum PacketElementType {
		D('d') {

			@Override
			public void write(SM_CUSTOM_PACKET packet, String value) {
				packet.writeD(Integer.decode(value));
			}
		},
		B('b') {

			@Override
			public void write(SM_CUSTOM_PACKET packet, String value) {
				packet.writeB(new byte[Integer.valueOf(value)]);
			}
		},
		BB('B') {

			@Override
			public void write(SM_CUSTOM_PACKET packet, String value) {
				packet.writeB(value);
			}
		},
		H('h') {

			@Override
			public void write(SM_CUSTOM_PACKET packet, String value) {
				packet.writeH(Integer.decode(value));
			}
		},
		C('c') {

			@Override
			public void write(SM_CUSTOM_PACKET packet, String value) {
				packet.writeC(Integer.decode(value));
			}
		},
		F('f') {

			@Override
			public void write(SM_CUSTOM_PACKET packet, String value) {
				packet.writeF(Float.valueOf(value));
			}
		},
		DF('e') {

			@Override
			public void write(SM_CUSTOM_PACKET packet, String value) {
				packet.writeDF(Double.valueOf(value));
			}
		},
		Q('q') {

			@Override
			public void write(SM_CUSTOM_PACKET packet, String value) {
				packet.writeQ(Long.decode(value));
			}
		},
		S('s') {

			@Override
			public void write(SM_CUSTOM_PACKET packet, String value) {
				packet.writeS(value);
			}
		};

		private final char code;

		private PacketElementType(char code) {
			this.code = code;
		}

		public static PacketElementType getByCode(char code) {
			for (PacketElementType type : values()) {
				if (type.code == code) {
					return type;
				}
			}
			return null;
		}

		/**
		 * 按元素类型将 value 写入包缓冲。
		 * Writes {@code value} into the packet buffer according to this element type.
		 *
		 * packet instance
		 * @param value  元素字符串值 / element string value
		 */
		public abstract void write(SM_CUSTOM_PACKET packet, String value);
	}

	/**
	 * 单个自定义包字段（类型 + 字符串值）。
	 * Single custom-packet field (type + string value).
	 */
	public static class PacketElement {

		private final PacketElementType type;
		private final String value;

		public PacketElement(PacketElementType type, String value) {
			this.type = type;
			this.value = value;
		}

		/**
		 * 将本字段写入目标包。
		 * Writes this field into the target packet.
		 *
		 * packet instance
		 */
		public void writeValue(SM_CUSTOM_PACKET packet) {
			type.write(packet, value);
		}
	}

	private List<PacketElement> elements = new ArrayList<PacketElement>();

	/**
	 * @param opcode 自定义包操作码 / custom packet opcode
	 */
	public SM_CUSTOM_PACKET(int opcode) {
		super();
		setOpcode(opcode);
	}

	/**
	 * 追加一个已构造的元素。
	 * Appends a pre-built element.
	 *
	 * element
	 */
	public void addElement(PacketElement packetElement) {
		elements.add(packetElement);
	}

	/**
	 * 按类型与值追加元素。
	 * Appends an element by type and value.
	 *
	 * @param type 元素类型 / element type
	 * @param value 字符串值 / string value
	 */
	public void addElement(PacketElementType type, String value) {
		elements.add(new PacketElement(type, value));
	}

	@Override
	public void writeImpl(AionConnection con) {
		for (PacketElement el : elements) {
			el.writeValue(this);
		}
	}
}
