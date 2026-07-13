package com.aionemu.loginserver.network.sts;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

class StsVipProtocolTest {

    @Test
    void scoreUsesVipExpFirstThenLevelFloor() {
        assertEquals(3759L, StsVipProtocol.resolveScore(6, 3759L, 0L));
        assertEquals(3759L, StsVipProtocol.resolveScore(6, 0L, 0L));
        assertEquals(178L, StsVipProtocol.resolveScore(1, 0L, 0L));
        assertEquals(0L, StsVipProtocol.resolveScore(0, 0L, 0L));
        assertEquals(12L, StsVipProtocol.resolveScore(0, 0L, 12L));
    }

    @Test
    void detectsLevelGetLevelRequests() {
        assertTrue(StsVipProtocol.isLevelGetLevel(
            "POST /Level/GetLevel STS/1.0",
            "s:1\r\nl:10",
            "<Request><UserId>cc</UserId><GameTierKey>AION_MEMBERSHIP</GameTierKey></Request>"
        ));
        assertFalse(StsVipProtocol.isLevelGetLevel(
            "POST /Auth/LoginStart STS/1.0",
            "s:1",
            "<Request/>"
        ));
    }

    @Test
    void parsesPipelinedRequestAndBuildsReply() {
        String body = "<Request><UserId>cc</UserId><GameTierKey>AION_MEMBERSHIP</GameTierKey><IncludePolicy>true</IncludePolicy></Request>";
        String request = "POST /Level/GetLevel STS/1.0\r\n"
            + "s:abc\r\n"
            + "R:7\r\n"
            + "l:" + body.getBytes(StandardCharsets.UTF_8).length + "\r\n\r\n"
            + body;
        StsVipProtocol.ParsedRequest parsed = StsVipProtocol.tryParse(request);
        assertTrue(parsed != null);
        assertEquals("cc", parsed.userId());
        assertEquals("abc", parsed.session());
        assertEquals("7", parsed.seq());

        String replyBody = StsVipProtocol.buildLevelReplyBody("cc", "AION", 3759L);
        assertTrue(replyBody.contains("<AccumulateGradeScore>3759</AccumulateGradeScore>"));
        byte[] response = StsVipProtocol.buildResponse(replyBody, "abc", "7");
        String text = new String(response, StandardCharsets.UTF_8);
        assertTrue(text.startsWith("STS/1.0 200 OK\r\n"));
        assertTrue(text.contains("s:abc\r\n"));
        assertTrue(text.contains("R:7\r\n"));
        assertTrue(text.contains("l:" + replyBody.getBytes(StandardCharsets.UTF_8).length + "\r\n\r\n"));
        assertTrue(text.endsWith(replyBody));
    }
}
