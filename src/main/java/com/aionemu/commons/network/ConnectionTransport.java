package com.aionemu.commons.network;

public interface ConnectionTransport {

    String getIP();

    void enableWriteInterest();

    void close(boolean forced);

    boolean onlyClose();
}
