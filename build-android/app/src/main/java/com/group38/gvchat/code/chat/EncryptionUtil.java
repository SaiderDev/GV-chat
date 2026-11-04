/**
 * Copyright Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.group38.gvchat.code.chat;

import android.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;

public class EncryptionUtil {
    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_TAG_LENGTH = 128;
    private static final int IV_LENGTH = 12;

    // Shared 256-bit key for chat encryption
    private static final String SECRET_KEY_STR = "12345678901234567890123456789012"; 

    private static SecretKey getSecretKey() {
        return new SecretKeySpec(SECRET_KEY_STR.getBytes(), "AES");
    }

    public static String encrypt(String plainText) {
        if (plainText == null || plainText.isEmpty()) return plainText;
        try {
            byte[] iv = new byte[IV_LENGTH];
            new SecureRandom().nextBytes(iv);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, getSecretKey(), spec);

            byte[] cipherText = cipher.doFinal(plainText.getBytes("UTF-8"));

            byte[] combined = new byte[iv.length + cipherText.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(cipherText, 0, combined, iv.length, cipherText.length);

            return Base64.encodeToString(combined, Base64.NO_WRAP);
        } catch (Exception e) {
            e.printStackTrace();
            return plainText;
        }
    }

    public static String decrypt(String encryptedBase64) {
        if (encryptedBase64 == null || encryptedBase64.isEmpty()) return encryptedBase64;
        try {
            byte[] combined = Base64.decode(encryptedBase64, Base64.NO_WRAP);

            byte[] iv = new byte[IV_LENGTH];
            System.arraycopy(combined, 0, iv, 0, IV_LENGTH);

            byte[] cipherText = new byte[combined.length - IV_LENGTH];
            System.arraycopy(combined, IV_LENGTH, cipherText, 0, cipherText.length);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, getSecretKey(), spec);

            byte[] plainText = cipher.doFinal(cipherText);
            return new String(plainText, "UTF-8");
        } catch (Exception e) {
            return encryptedBase64; // Fallback to raw string if not encrypted
        }
    }
}
