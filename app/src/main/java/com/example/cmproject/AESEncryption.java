package com.example.cmproject;

import android.util.Base64;

import java.security.Key;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

public class AESEncryption {

    private static final String ALGORITHM = "AES";
    private static final String KEY = "s6v9y$B&E)H@MbQeThWmZq4t7w!z%C*F";

    // Função responsável pela encriptação da password
    public static String encrypt(String value) throws Exception {
        Key key = generateKey();
        Cipher cipher = Cipher.getInstance(AESEncryption.ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, key);
        byte [] encryptedByteValue = cipher.doFinal(value.getBytes("utf-8"));
        String encryptedValue64 = Base64.encodeToString(encryptedByteValue, Base64.DEFAULT);
        return encryptedValue64;
    }

    private static Key generateKey() {
        Key key = new SecretKeySpec(AESEncryption.KEY.getBytes(), AESEncryption.ALGORITHM);
        return key;
    }
}
