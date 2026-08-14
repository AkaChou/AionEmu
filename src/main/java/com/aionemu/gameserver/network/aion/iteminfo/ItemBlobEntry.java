package com.aionemu.gameserver.network.aion.iteminfo;

import java.nio.ByteBuffer;

import com.aionemu.gameserver.model.gameobjects.Item;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.stats.calc.functions.IStatFunction;
import com.aionemu.gameserver.network.PacketWriteHelper;
import com.aionemu.gameserver.network.aion.iteminfo.ItemInfoBlob.ItemBlobType;

/**
 * 物品信息 Blob 条目基类，封装详细物品属性的序列化。
 * 客户端以 Blob 序列形式接收数据，每个条目先写类型 ID，再写具体负载。
 * Base class for item-info blob entries that serialize detailed item attributes.
 * The client receives a sequence of blobs; each entry writes its type id first, then the payload.
 *
 * @author -Nemesiss-
 * @modified Rolandas
 */
public abstract class ItemBlobEntry extends PacketWriteHelper {

	/** Blob 类型。 / Blob type. */
	private final ItemBlobType type;
	/** 所属玩家。 / Owning player. */
	Player owner;
	/** 所属物品。 / Owning item. */
	Item ownerItem;
	/** 关联属性修正（用于加成条目）。 / Associated stat modifier (for bonus blobs). */
	IStatFunction modifier;

	/**
	 * 以指定 Blob 类型构造条目。
	 * Constructs an entry with the given blob type.
	 *
	 * @param type blob 类型 / blob type
	 */
	ItemBlobEntry(ItemBlobType type) {
		this.type = type;
	}

	/**
	 * 绑定所属玩家、物品及可选属性修正。
	 * Binds the owning player, item, and optional stat modifier.
	 *
	 * @param owner 所属玩家 / owning player
	 * @param item 所属物品 / owning item
	 * @param modifier 属性修正，可为 null / stat modifier, may be null
	 */
	void setOwner(Player owner, Item item, IStatFunction modifier) {
		this.owner = owner;
		this.ownerItem = item;
		this.modifier = modifier;
	}

	/**
	 * 写入类型 ID 后委托 {@link #writeThisBlob(ByteBuffer)} 写具体负载。
	 * Writes the type id, then delegates payload writing to {@link #writeThisBlob(ByteBuffer)}.
	 */
	@Override
	protected void writeMe(ByteBuffer buf) {
		writeC(buf, type.getEntryId());
		writeThisBlob(buf);
	}

	/**
	 * 将本 Blob 的具体内容写入缓冲区。
	 * Writes this blob's concrete payload into the buffer.
	 *
	 * @param buf 目标缓冲区 / target buffer
	 */
	public abstract void writeThisBlob(ByteBuffer buf);

	/**
	 * 返回本 Blob 负载的字节长度（不含类型 ID）。
	 * Returns the payload size of this blob in bytes (excluding the type id).
	 *
	 * @return 负载字节数 / payload size in bytes
	 */
	public abstract int getSize();

}
