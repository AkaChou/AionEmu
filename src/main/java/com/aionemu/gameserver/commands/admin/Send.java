package com.aionemu.gameserver.commands.admin;

import com.aionemu.boot.i18n.I18n;
import lombok.extern.slf4j.Slf4j;
import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import com.aionemu.gameserver.GameServerError;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_CUSTOM_PACKET;
import com.aionemu.gameserver.network.aion.serverpackets.SM_CUSTOM_PACKET.PacketElementType;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.chathandlers.AdminCommand;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.annotation.*;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 * 自定义数据包发送指令；按 {@code ./data/packets} 下 XML 映射向目标客户端下发包序列。
 * Admin command that sends sequenced custom packets defined by XML mappings under {@code ./data/packets}.
 *
 * @author Aquanox
 */
@Slf4j
public class Send extends AdminCommand {

	public Send() {
		super("send");
		// 初始化反编组器一次。 / init unmrshaller once.
		try {
			unmarshaller = JAXBContext.newInstance(Packets.class, Packet.class, Part.class).createUnmarshaller();
		}
		catch (Exception e) {
			throw new GameServerError("Failed to initialize unmarshaller.", e);
		}
	}


	private static final File FOLDER = new File("./data/packets");

	private Unmarshaller unmarshaller;

	/**
	 * 执行该管理指令。
	 * Executes this admin command.
	 *
	 * @param admin 执行指令的管理员 / admin executing the command
	 * command arguments
	 */
	@Override
	public void execute(Player admin, String... params) {
		if (params.length != 1) {
			PacketSendUtility.sendMessage(admin, "Example: //send [file] ");
			return;
		}

		final String mappingName = params[0];
		final Player target = getTargetPlayer(admin);

		// log.debug("Mapping: " + mappingName);
		// log.debug("Target: " + target);

		File packetsData = new File(FOLDER, mappingName + ".xml");

		if (!packetsData.exists()) {
			PacketSendUtility.sendMessage(admin, "Mapping with name " + mappingName + " not found");
			return;
		}

		final Packets packetsTemplate;

		try {
			packetsTemplate = (Packets) unmarshaller.unmarshal(packetsData);
		}
		catch (JAXBException e) {
			log.error(I18n.get("log.79e5f0e82fd8", e), e);
			return;
		}

		if (packetsTemplate.getPackets().isEmpty()) {
			PacketSendUtility.sendMessage(admin, "No packets to send.");
			return;
		}

		send(admin, target, packetsTemplate);
	}

	private void send(Player sender, final Player target, Packets packets) {
		final String senderObjectId = String.valueOf(sender.getObjectId());
		final String targetObjectId = String.valueOf(target.getObjectId());

		long delay = 0;
		for (final Packet packetTemplate : packets) {
			// log.debug("Processing: " + packetTemplate);

			final SM_CUSTOM_PACKET packet = new SM_CUSTOM_PACKET(packetTemplate.getOpcode());

			for (Part part : packetTemplate.getParts()) {
				PacketElementType byCode = PacketElementType.getByCode(part.getType());

				String value = part.getValue();

				if (value.indexOf("${objectId}") != -1)
					value = value.replace("${objectId}", targetObjectId);
				if (value.indexOf("${senderObjectId}") != -1)
					value = value.replace("${senderObjectId}", senderObjectId);
				if (value.indexOf("${targetObjectId}") != -1)
					value = value.replace("${targetObjectId}", targetObjectId);

				if (part.getRepeatCount() == 1) // skip loop
				{
					packet.addElement(byCode, value);
				}
				else {
					for (int i = 0; i < part.getRepeatCount(); i++)
						packet.addElement(byCode, value);
				}
			}

			delay += packetTemplate.getDelay();

			GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {

				@Override
				public void run() {
					// log.debug("Sending: " + packetTemplate);
					PacketSendUtility.sendPacket(target, packet);
				}
			}, delay);

			delay += packets.getDelay();
		}
	}

	private Player getTargetPlayer(Player admin) {
		if (admin.getTarget() instanceof Player) {
			return (Player) admin.getTarget();
		}
		return admin;
	}

	@XmlAccessorType(XmlAccessType.FIELD)
	@XmlRootElement(name = "packets")
	private static class Packets implements Iterable<Packet> {

		@XmlElement(name = "packet")
		private List<Packet> packets = new ArrayList<Packet>();

		@XmlAttribute(name = "delay")
		private long delay = -1;

		public long getDelay() {
			return delay;
		}

		public List<Packet> getPackets() {
			return packets;
		}

		@SuppressWarnings("unused")
		public boolean add(Packet packet) {
			return packets.add(packet);
		}

		@Override
		public Iterator<Packet> iterator() {
			return packets.iterator();
		}

		@Override
		public String toString() {
			final StringBuilder sb = new StringBuilder();
			sb.append("Packets");
			sb.append("{delay=").append(delay);
			sb.append(", packets=").append(packets);
			sb.append('}');
			return sb.toString();
		}
	}

	@XmlAccessorType(XmlAccessType.FIELD)
	@XmlRootElement(name = "packet")
	private static class Packet {

		@XmlElement(name = "part")
		private Collection<Part> parts = new ArrayList<Part>();

		@XmlAttribute(name = "opcode")
		private String opcode = "-1";

		@XmlAttribute(name = "delay")
		private long delay = 0;

		public int getOpcode() {
			return Integer.decode(opcode);
		}

		public Collection<Part> getParts() {
			return parts;
		}

		public long getDelay() {
			return delay;
		}

		@Override
		public String toString() {
			final StringBuilder sb = new StringBuilder();
			sb.append("Packet");
			sb.append("{opcode=").append(opcode);
			sb.append(", parts=").append(parts);
			sb.append('}');
			return sb.toString();
		}
	}

	@XmlAccessorType(XmlAccessType.FIELD)
	@XmlRootElement(name = "part")
	private static class Part {

		@XmlAttribute(name = "type", required = true)
		private String type = null;

		@XmlAttribute(name = "value", required = true)
		private String value = null;

		@XmlAttribute(name = "repeat", required = true)
		private int repeatCount = 1;

		public char getType() {
			return type.charAt(0);
		}

		public String getValue() {
			return value;
		}

		public int getRepeatCount() {
			return repeatCount;
		}

		@Override
		public String toString() {
			final StringBuilder sb = new StringBuilder();
			sb.append("Part");
			sb.append("{type='").append(type).append('\'');
			sb.append(", value='").append(value).append('\'');
			sb.append(", repeatCount=").append(repeatCount);
			sb.append('}');
			return sb.toString();
		}
	}

}