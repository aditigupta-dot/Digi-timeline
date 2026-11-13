package com.digitaltimeline.integration;
import static org.junit.jupiter.api.Assertions.*;
import com.digitaltimeline.config.AppProperties;
import com.digitaltimeline.exception.OAuthException;
import com.digitaltimeline.security.TokenCipher;
import java.util.Base64;
import java.util.UUID;
import org.junit.jupiter.api.Test;
class IntegrationUnitTests {
  private static TokenCipher cipher(byte fill) { byte[] k = new byte[32]; java.util.Arrays.fill(k, fill);
    return new TokenCipher(new AppProperties(null, Base64.getEncoder().encodeToString(k), "x")); }
  @Test void encryptDecryptRoundTrip() { var c = cipher((byte) 1); String t = "gho_secret"; String e = c.encrypt(t);
    assertNotEquals(t, e); assertEquals(t, c.decrypt(e)); }
  @Test void sameTextEncryptsDifferentlyEachTime() { var c = cipher((byte) 1); assertNotEquals(c.encrypt("a"), c.encrypt("a")); }
  @Test void wrongKeyCannotDecrypt() { String e = cipher((byte) 1).encrypt("a"); assertThrows(IllegalStateException.class, () -> cipher((byte) 2).decrypt(e)); }
  @Test void stateIsOneTimeUse() { var s = new OAuthStateStore(); var u = UUID.randomUUID(); String st = s.create(u, Provider.GITHUB);
    assertEquals(u, s.consume(st, Provider.GITHUB)); assertThrows(OAuthException.class, () -> s.consume(st, Provider.GITHUB)); }
  @Test void unknownStateRejected() { assertThrows(OAuthException.class, () -> new OAuthStateStore().consume("nope", Provider.GITHUB)); }
  @Test void stateCannotBeUsedForAnotherProvider() { var s = new OAuthStateStore(); String st = s.create(UUID.randomUUID(), Provider.GITHUB);
    assertThrows(OAuthException.class, () -> s.consume(st, Provider.GMAIL)); }
}
