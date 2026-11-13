package com.digitaltimeline.security;
import com.digitaltimeline.config.AppProperties;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Component;
/** AES-256-GCM encryption for provider tokens at rest. Key = base64 of 32 bytes from TOKEN_ENC_KEY. */
@Component
public class TokenCipher {
  private final SecretKeySpec key; private final SecureRandom random = new SecureRandom();
  public TokenCipher(AppProperties props) {
    byte[] k = Base64.getDecoder().decode(props.tokenEncryptionKey());
    if (k.length != 32) throw new IllegalStateException("TOKEN_ENC_KEY must be base64 of exactly 32 bytes (openssl rand -base64 32)");
    key = new SecretKeySpec(k, "AES");
  }
  public String encrypt(String plain) {
    try {
      byte[] iv = new byte[12]; random.nextBytes(iv);
      Cipher c = Cipher.getInstance("AES/GCM/NoPadding"); c.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(128, iv));
      byte[] ct = c.doFinal(plain.getBytes(StandardCharsets.UTF_8));
      return Base64.getEncoder().encodeToString(ByteBuffer.allocate(iv.length + ct.length).put(iv).put(ct).array());
    } catch (Exception e) { throw new IllegalStateException("Encryption failed", e); }
  }
  public String decrypt(String stored) {
    try {
      byte[] all = Base64.getDecoder().decode(stored);
      Cipher c = Cipher.getInstance("AES/GCM/NoPadding"); c.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(128, all, 0, 12));
      return new String(c.doFinal(all, 12, all.length - 12), StandardCharsets.UTF_8);
    } catch (Exception e) { throw new IllegalStateException("Decryption failed (wrong key or corrupted data)", e); }
  }
}
