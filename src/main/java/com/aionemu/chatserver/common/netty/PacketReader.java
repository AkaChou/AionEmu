package com.aionemu.chatserver.common.netty;

public interface PacketReader {

    int readableBytes();

    int readC();

    int readH();

    int readD();

    long readQ();

    float readF();

    double readDF();

    char readChar();

    void readBytes(byte[] destination);
}
