package com.aionemu.commons.network.packet;

/**
 * 网络数据包的基础抽象类
 * Base abstract class for network packets
 *
 * 定义了网络数据包的基本属性和行为，包括包类型和操作码
 * Defines basic properties and behaviors of network packets, including packet type and opcode
 */
public abstract class BasePacket {
    /**
     * 数据包信息的格式化模式
     * Format pattern for packet information
     */
    public static final String TYPE_PATTERN = "[%s] 0x%02X %s";
    
    /**
     * 数据包类型(服务器/客户端)
     * Packet type (SERVER/CLIENT)
     */
    private final BasePacket.PacketType packetType;
    
    /**
     * 操作码，标识数据包的类型
     * Opcode that identifies the type of packet
     */
    private int opcode;

    /**
     * 构造函数
     * Constructor
     *
     * @param packetType 数据包类型 / Packet type
     * @param opcode 操作码 / Operation code
     */
    protected BasePacket(BasePacket.PacketType packetType, int opcode) {
        this.packetType = packetType;
        this.opcode = opcode;
    }

    /**
     * 构造函数
     * Constructor
     *
     * @param packetType 数据包类型 / Packet type
     */
    protected BasePacket(BasePacket.PacketType packetType) {
        this.packetType = packetType;
    }

    /**
     * 设置操作码
     * Set operation code
     *
     * @param opcode 操作码 / Operation code
     */
    protected void setOpcode(int opcode) {
        this.opcode = opcode;
    }

    /**
     * 获取操作码
     * Get operation code
     *
     * @return 操作码 / Operation code
     */
    public final int getOpcode() {
        return this.opcode;
    }

    /**
     * 获取数据包类型
     * Get packet type
     *
     * @return 数据包类型 / Packet type
     */
    public final BasePacket.PacketType getPacketType() {
        return this.packetType;
    }

    /**
     * 获取数据包名称
     * Get packet name
     *
     * @return 数据包名称 / Packet name
     */
    public String getPacketName() {
        return this.getClass().getSimpleName();
    }

    /**
     * 转换为字符串表示
     * Convert to string representation
     *
     * @return 数据包的字符串表示 / String representation of the packet
     */
    public String toString() {
        return String.format("[%s] 0x%02X %s", this.getPacketType().getName(), this.getOpcode(), this.getPacketName());
    }

    /**
     * 数据包类型枚举
     * Packet type enumeration
     */
    public static enum PacketType {
        /** 服务器数据包 / Server packet */
        SERVER("S"),
        /** 客户端数据包 / Client packet */
        CLIENT("C");

      private final String name;

      private PacketType(String name) {
         this.name = name;
      }

      public String getName() {
         return this.name;
      }
   }
}
