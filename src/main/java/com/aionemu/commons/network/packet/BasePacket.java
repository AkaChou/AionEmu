package com.aionemu.commons.network.packet;

/**
 * 网络数据包基类，定义类型与操作码。
 * Base network packet class defining type and opcode.
 */
public abstract class BasePacket {

    /**
     * 数据包信息格式化模式。
     * Packet info format pattern.
     */
    public static final String TYPE_PATTERN = "[%s] 0x%02X %s";

    /**
     * 数据包类型（服务端/客户端）。
     * Packet type (server/client).
     */
    private final BasePacket.PacketType packetType;

    /**
     * 操作码。
     * Opcode.
     */
    private int opcode;

    /**
     * 构造带操作码的数据包。
     * Construct packet with opcode.
     *
     * @param packetType 数据包类型 / Packet type
     * Opcode
     */
    protected BasePacket(BasePacket.PacketType packetType, int opcode) {
        this.packetType = packetType;
        this.opcode = opcode;
    }

    /**
     * 构造无操作码数据包。
     * Construct packet without opcode.
     *
     * @param packetType 数据包类型 / Packet type
     */
    protected BasePacket(BasePacket.PacketType packetType) {
        this.packetType = packetType;
    }

    /**
     * 设置操作码。
     * Set opcode.
     *
     * Opcode
     */
    protected void setOpcode(int opcode) {
        this.opcode = opcode;
    }

    /**
     * 获取操作码。
     * Get opcode.
     *
     * Opcode
     */
    public final int getOpcode() {
        return this.opcode;
    }

    /**
     * 获取数据包类型。
     * Get packet type.
     *
     * @return 数据包类型 / Packet type
     */
    public final BasePacket.PacketType getPacketType() {
        return this.packetType;
    }

    /**
     * 获取数据包名称（类简名）。
     * Get packet name (simple class name).
     *
     * @return 数据包名称 / Packet name
     */
    public String getPacketName() {
        return this.getClass().getSimpleName();
    }

    /**
     * 转换为字符串表示。
     * Convert to string representation.
     *
     * String
     */
    public String toString() {
        return String.format("[%s] 0x%02X %s", this.getPacketType().getName(), this.getOpcode(), this.getPacketName());
    }

    /**
     * 数据包方向类型。
     * Packet direction type.
     */
    public static enum PacketType {
        /** 服务器数据包 / Server packet */
        SERVER("S"),
        /** 客户端数据包 / Client packet */
        CLIENT("C");

        private final String name;

        /**
         * 构造类型。
         * Construct type.
         *
         * Short name
         */
        private PacketType(String name) {
            this.name = name;
        }

        /**
         * 获取短名称。
         * Get short name.
         *
         * Short name
         */
        public String getName() {
            return this.name;
        }
    }
}
