package com.aionemu.chatserver.common.netty;

public interface PacketWriter {

    int readableBytes();

    void writeC(int value);

    void writeH(int value);

    void writeD(int value);

    void writeQ(long value);

    void writeF(float value);

    void writeDF(double value);

    void writeChar(char value);

    void writeB(byte[] data);

    void setH(int index, int value);

    void getBytes(int index, byte[] destination);
}
