package com.aionemu.commons.configuration.transformers;

import com.aionemu.commons.configuration.PropertyTransformer;
import com.aionemu.commons.configuration.TransformationException;
import java.lang.reflect.Field;
import java.net.InetAddress;
import java.net.InetSocketAddress;

/**
 * 网络套接字地址属性转换器
 * InetSocketAddress property transformer class that handles conversion of string values to InetSocketAddress objects.
 *
 * 支持的输入格式:
 * Supported input format:
 * - "address:port" 格式的字符串，其中address可以是:
 *   String in "address:port" format, where address can be:
 *   - IP地址 IP address
 *   - 主机名 hostname
 *   - * (通配符，表示所有地址) wildcard (represents all addresses)
 */
public class InetSocketAddressTransformer implements PropertyTransformer<InetSocketAddress> {
    
    /**
     * 共享实例
     * Shared instance of the transformer
     */
    public static final InetSocketAddressTransformer SHARED_INSTANCE = new InetSocketAddressTransformer();

    /**
     * 将字符串值转换为InetSocketAddress对象
     * Transforms string value into InetSocketAddress object
     *
     * @param value 要转换的字符串值（格式："address:port"）String value to transform (format: "address:port")
     * @param field 字段对象 Field that will be transformed
     * @return 转换后的InetSocketAddress对象 Transformed InetSocketAddress object
     * @throws TransformationException 如果输入格式无效或地址解析失败 if input format is invalid or address resolution fails
     */
    public InetSocketAddress transform(String value, Field field) throws TransformationException {
        String[] parts = value.split(":");
        if (parts.length != 2) {
            throw new TransformationException("Can't transform property, must be in format \"address:port\"");
        }

        try {
            if ("*".equals(parts[0])) {
                return new InetSocketAddress(Integer.parseInt(parts[1]));
            } else {
                InetAddress address = InetAddress.getByName(parts[0]);
                int port = Integer.parseInt(parts[1]);
                return new InetSocketAddress(address, port);
            }
        } catch (Exception var6) {
            throw new TransformationException(var6);
        }
    }
}
