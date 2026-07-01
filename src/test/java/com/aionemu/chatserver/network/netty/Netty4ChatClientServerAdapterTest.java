package com.aionemu.chatserver.network.netty;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.aionemu.chatserver.common.netty.PacketReader;
import com.aionemu.chatserver.common.netty.PacketWriter;
import com.aionemu.chatserver.network.aion.AbstractServerPacket;
import com.aionemu.chatserver.network.aion.ClientPacketHandler;
import com.aionemu.chatserver.network.netty.handler.ClientChannelHandler;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.embedded.EmbeddedChannel;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class Netty4ChatClientServerAdapterTest {

    @Test
    void inboundByteBufIsAdaptedToLittleEndianPacketReader() throws Exception {
        CapturingClientChannelHandler delegate = new CapturingClientChannelHandler();
        EmbeddedChannel channel = new EmbeddedChannel(newNetty4ClientChannelHandler(delegate));
        ByteBuf input = Unpooled.wrappedBuffer(new byte[] {0x05, 0x34, 0x12, (byte) 0x88});

        channel.writeInbound(input);

        assertNotNull(delegate.receivedValues);
        assertArrayEquals(new int[] {0x05, 0x1234, 0x88}, delegate.receivedValues);
        assertEquals(0, delegate.remainingBytes);
        assertEquals(0, input.refCnt());

        channel.finishAndReleaseAll();
    }

    @Test
    void outboundPacketWriterIsWrittenAsByteBuf() {
        AtomicReference<ByteBuf> written = new AtomicReference<>();
        ClientChannelHandler handler = new ClientChannelHandler(new ClientPacketHandler());

        handler.nettyChannelActive(nettyChannelCapturingWrites(written, new AtomicBoolean()));
        handler.sendPacket(new TestServerPacket());

        ByteBuf output = written.get();
        assertNotNull(output);
        try {
            byte[] actual = new byte[output.readableBytes()];
            output.getBytes(0, actual);
            assertArrayEquals(
                new byte[] {0x09, 0x00, 0x11, 0x33, 0x22, 0x77, 0x66, 0x55, 0x44},
                actual
            );
        } finally {
            output.release();
        }
    }

    @Test
    void closeClosesActiveNettyChannel() {
        AtomicBoolean closed = new AtomicBoolean();
        ClientChannelHandler handler = new ClientChannelHandler(new ClientPacketHandler());

        handler.nettyChannelActive(nettyChannelCapturingWrites(new AtomicReference<>(), closed));
        handler.close();

        assertTrue(closed.get());
    }

    private static ChannelHandler newNetty4ClientChannelHandler(ClientChannelHandler delegate) throws Exception {
        Class<?> handlerType = Class.forName("com.aionemu.chatserver.network.netty.Netty4ChatClientServer$Netty4ClientChannelHandler");
        Constructor<?> constructor = handlerType.getDeclaredConstructor(ClientChannelHandler.class);
        constructor.setAccessible(true);
        return (ChannelHandler) constructor.newInstance(delegate);
    }

    private static Channel nettyChannelCapturingWrites(AtomicReference<ByteBuf> written, AtomicBoolean closed) {
        InvocationHandler handler = (proxy, method, args) -> {
            if (method.getDeclaringClass() == Object.class) {
                return switch (method.getName()) {
                    case "equals" -> proxy == args[0];
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "toString" -> "capturing-netty-channel";
                    default -> null;
                };
            }
            return switch (method.getName()) {
                case "remoteAddress" -> new InetSocketAddress("127.0.0.1", 2106);
                case "writeAndFlush" -> {
                    written.set((ByteBuf) args[0]);
                    yield null;
                }
                case "close" -> {
                    closed.set(true);
                    yield null;
                }
                default -> defaultValue(method.getReturnType());
            };
        };
        return (Channel) Proxy.newProxyInstance(Channel.class.getClassLoader(), new Class<?>[] {Channel.class}, handler);
    }

    private static Object defaultValue(Class<?> returnType) {
        if (!returnType.isPrimitive()) {
            return null;
        }
        if (returnType == boolean.class) {
            return false;
        }
        if (returnType == byte.class) {
            return (byte) 0;
        }
        if (returnType == short.class) {
            return (short) 0;
        }
        if (returnType == int.class) {
            return 0;
        }
        if (returnType == long.class) {
            return 0L;
        }
        if (returnType == float.class) {
            return 0F;
        }
        if (returnType == double.class) {
            return 0D;
        }
        if (returnType == char.class) {
            return '\0';
        }
        return null;
    }

    private static final class CapturingClientChannelHandler extends ClientChannelHandler {

        private int[] receivedValues;
        private int remainingBytes;

        private CapturingClientChannelHandler() {
            super(new ClientPacketHandler());
        }

        @Override
        public void nettyChannelActive(Channel channel) {
        }

        @Override
        public void nettyMessageReceived(PacketReader message) {
            receivedValues = new int[] {
                message.readC(),
                message.readH(),
                message.readC()
            };
            remainingBytes = message.readableBytes();
        }
    }

    private static final class TestServerPacket extends AbstractServerPacket {

        private TestServerPacket() {
            super(0x77);
        }

        @Override
        protected void writeImpl(ClientChannelHandler cHandler, PacketWriter buf) {
            writeC(buf, 0x11);
            writeH(buf, 0x2233);
            writeD(buf, 0x44556677);
        }
    }
}
