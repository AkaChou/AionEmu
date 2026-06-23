package com.aionemu.gameserver.utils.xml;

import javax.xml.bind.SchemaOutputResolver;
import javax.xml.transform.Result;
import javax.xml.transform.stream.StreamResult;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * @author ginho1
 */
public class StringSchemaOutputResolver extends SchemaOutputResolver {

    private ByteArrayOutputStream baos = new ByteArrayOutputStream();

    @Override
    public Result createOutput(String namespaceUri, String suggestedFileName) throws IOException {
        StreamResult result = new StreamResult(baos);
        result.setSystemId(suggestedFileName);
        return result;
    }
    public String getSchemaString() {
        return baos.toString();
    }
}