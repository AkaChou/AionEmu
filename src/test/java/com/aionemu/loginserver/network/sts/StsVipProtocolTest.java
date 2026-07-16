package com.aionemu.loginserver.network.sts;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class StsVipProtocolTest {

    @AfterEach
    void clearBindings() {
        StsVipServer.clearAuthenticatedAccountsForTests();
    }

    @Test
    void scoreUsesVipExpFirstThenLevelFloor() {
        assertEquals(3759L, StsVipProtocol.resolveScore(6, 3759L));
        assertEquals(3759L, StsVipProtocol.resolveScore(6, 0L));
        assertEquals(178L, StsVipProtocol.resolveScore(1, 0L));
        assertEquals(0L, StsVipProtocol.resolveScore(0, 0L));
    }

    @Test
    void readsRealConnectAndMembershipRequestsFromOneConnection() throws Exception {
        String connectBody = "<Connect><ConnType>1</ConnType><AppIndex>1</AppIndex></Connect>";
        String levelBody = "<Request><GameTierKey>AION_MEMBERSHIP</GameTierKey><IncludePolicy>1</IncludePolicy></Request>";
        ByteArrayInputStream input = new ByteArrayInputStream((request("/Sts/Connect", "31", connectBody)
            + request("/Level/GetLevel", "32", levelBody)).getBytes(StandardCharsets.UTF_8));

        StsVipProtocol.ParsedRequest connect = StsVipProtocol.readRequest(input);
        StsVipProtocol.ParsedRequest level = StsVipProtocol.readRequest(input);

        assertTrue(StsVipProtocol.isConnect(connect));
        assertEquals("31", connect.sequence());
        assertTrue(StsVipProtocol.isLevelGetLevel(level));
        assertEquals("32", level.sequence());
        assertFalse(level.body().contains("UserId"));
        assertEquals(null, StsVipProtocol.readRequest(input));
    }

    @Test
    void detectsAuthChainPaths() throws Exception {
        String loginStart = request(
            "/Auth/LoginTokenStart",
            "1",
            "<Request><ClientRand>YWJj</ClientRand></Request>"
        );
        String tokenKey = request(
            "/Auth/TokenKeyData",
            "2",
            "<Request><PremasterSecret>x</PremasterSecret><AuthnToken>t</AuthnToken><AppId>aion</AppId><AuthProviderCode/></Request>"
        );
        String loginFinish = request("/Auth/LoginFinish", "3", "<Request></Request>");
        ByteArrayInputStream input = new ByteArrayInputStream(
            (loginStart + tokenKey + loginFinish).getBytes(StandardCharsets.UTF_8)
        );

        assertTrue(StsVipProtocol.isLoginTokenStart(StsVipProtocol.readRequest(input)));
        assertTrue(StsVipProtocol.isTokenKeyData(StsVipProtocol.readRequest(input)));
        assertTrue(StsVipProtocol.isLoginFinish(StsVipProtocol.readRequest(input)));
    }

    @Test
    void loginTokenStartReplyIsSignedWithClasspathKey() throws Exception {
        assertTrue(StsAuthCrypto.isAvailable(), "STS keys must be on test classpath");
        String body = StsVipProtocol.buildLoginTokenStartReplyBody();
        String randB64 = StsVipProtocol.extractXmlField(body, "ServerRand");
        String keyB64 = StsVipProtocol.extractXmlField(body, "ServerPublicKey");
        String sigB64 = StsVipProtocol.extractXmlField(body, "ServerSignature");

        assertNotNull(randB64);
        assertNotNull(keyB64);
        assertNotNull(sigB64);
        assertEquals(StsAuthCrypto.get().publicKeyB64(), keyB64);

        byte[] serverRand = Base64.getDecoder().decode(randB64);
        byte[] signature = Base64.getDecoder().decode(sigB64);
        assertEquals(32, serverRand.length);
        assertEquals(256, signature.length);
        assertEquals(268, Base64.getDecoder().decode(keyB64).length);

        byte[] publicBlob = Base64.getDecoder().decode(keyB64);
        assertArrayEquals(
            MessageDigest.getInstance("MD5").digest(publicBlob),
            gameDllVerify(publicBlob, signature)
        );
    }

    @Test
    void loginFinishReplyCarriesUserIdAndUserName() {
        String body = StsVipProtocol.buildLoginFinishReplyBody("cc");
        assertEquals("cc", StsVipProtocol.extractXmlField(body, "UserId"));
        assertEquals("cc", StsVipProtocol.extractXmlField(body, "UserName"));
    }

    @Test
    void listMyAccountsReplyCarriesGameAccountAlias() {
        String body = StsVipProtocol.buildListMyAccountsReplyBody("cc");
        assertTrue(body.contains("<GameAccount>"));
        assertEquals("cc", StsVipProtocol.extractXmlField(body, "Alias"));
        assertTrue(StsVipProtocol.isListMyAccounts(new StsVipProtocol.ParsedRequest(
            "POST /GameAccount/ListMyAccounts STS/1.0",
            "<Request><GameCode>aion</GameCode></Request>",
            "9"
        )));
    }

    @Test
    void requestGameTokenReplyCarriesToken() {
        String body = StsVipProtocol.buildRequestGameTokenReplyBody("local-cc");
        assertEquals("local-cc", StsVipProtocol.extractXmlField(body, "Token"));
        assertTrue(StsVipProtocol.isRequestGameToken(new StsVipProtocol.ParsedRequest(
            "POST /Auth/RequestGameToken STS/1.0",
            "<Request><AccountAlias>cc</AccountAlias></Request>",
            "10"
        )));
    }

    @Test
    void responseEchoesSSequenceAndContainsClientMembershipFields() {
        String body = StsVipProtocol.buildLevelReplyBody(3758L);
        String response = new String(StsVipProtocol.buildResponse(body, "42"), StandardCharsets.UTF_8);

        assertTrue(response.startsWith("STS/1.0 200 OK\r\ns:42\r\n"));
        assertFalse(response.contains("\r\nR:"));
        assertTrue(response.contains("l:" + body.getBytes(StandardCharsets.UTF_8).length + "\r\n\r\n"));
        assertTrue(body.contains("<AccumulateGradeScore>3758</AccumulateGradeScore>"));
        assertTrue(body.contains("<MaximumLevelScore>3759</MaximumLevelScore>"));
        assertTrue(response.endsWith(body));
    }

    @Test
    void accountBindingUsesLatestLoginAndExpires() {
        long now = 1_000L;
        StsVipServer.rememberAuthenticatedAccount("192.168.1.20", 7, now);
        StsVipServer.rememberAuthenticatedAccount("192.168.1.20", 8, now + 1);

        assertEquals(8, StsVipServer.authenticatedAccountId("192.168.1.20", now + 2));
        assertEquals(null, StsVipServer.authenticatedAccountId(
            "192.168.1.20",
            now + 1 + StsVipServer.ACCOUNT_BINDING_TTL_MILLIS
        ));
    }

    private static byte[] gameDllVerify(byte[] publicBlob, byte[] signatureLe) {
        int exp = java.nio.ByteBuffer.wrap(publicBlob, 4, 4).order(java.nio.ByteOrder.LITTLE_ENDIAN).getInt();
        byte[] modLe = new byte[256];
        System.arraycopy(publicBlob, 12, modLe, 0, 256);
        byte[] signatureBe = signatureLe.clone();
        reverse(modLe);
        reverse(signatureBe);
        byte[] verifiedBe = new BigInteger(1, signatureBe)
            .modPow(BigInteger.valueOf(exp), new BigInteger(1, modLe))
            .toByteArray();
        int first = verifiedBe[0] == 0 ? 1 : 0;
        byte[] verifiedLe = new byte[verifiedBe.length - first];
        for (int i = first; i < verifiedBe.length; i++) {
            verifiedLe[verifiedBe.length - 1 - i] = verifiedBe[i];
        }
        return verifiedLe;
    }

    private static void reverse(byte[] bytes) {
        for (int i = 0, j = bytes.length - 1; i < j; i++, j--) {
            byte value = bytes[i];
            bytes[i] = bytes[j];
            bytes[j] = value;
        }
    }

    private static String request(String path, String sequence, String body) {
        return "POST " + path + " STS/1.0\r\n"
            + "s:" + sequence + "\r\n"
            + "l:" + body.getBytes(StandardCharsets.UTF_8).length + "\r\n\r\n"
            + body;
    }
}
