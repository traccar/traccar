/*
 * Copyright 2022 Anton Tananaev (anton@traccar.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.traccar.api.signature;

import org.traccar.storage.Storage;
import org.traccar.storage.StorageException;
import org.traccar.storage.query.Columns;
import org.traccar.storage.query.Request;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

@Singleton
public class CryptoManager {

    private final Storage storage;

    private PublicKey publicKey;
    private PrivateKey privateKey;

    @Inject
    public CryptoManager(Storage storage) {
        this.storage = storage;
    }

    public byte[] sign(byte[] data) throws GeneralSecurityException, StorageException {
        if (privateKey == null) {
            initializeKeys();
        }
        Signature signature = Signature.getInstance("SHA256withECDSA");
        signature.initSign(privateKey);
        signature.update(data);
        byte[] block = signature.sign();
        byte[] combined = new byte[1 + block.length + data.length];
        combined[0] = (byte) block.length;
        System.arraycopy(block, 0, combined, 1, block.length);
        System.arraycopy(data, 0, combined, 1 + block.length, data.length);
        return combined;
    }

    public byte[] verify(byte[] data) throws GeneralSecurityException, StorageException {
        if (publicKey == null) {
            initializeKeys();
        }
        Signature signature = Signature.getInstance("SHA256withECDSA");
        signature.initVerify(publicKey);
        int length = data[0];
        byte[] originalData = new byte[data.length - 1 - length];
        System.arraycopy(data, 1 + length, originalData, 0, originalData.length);
        signature.update(originalData);
        if (!signature.verify(data, 1, length)) {
            throw new SecurityException("Invalid signature");
        }
        return originalData;
    }

    private void initializeKeys() throws StorageException, GeneralSecurityException {
        KeystoreModel model = storage.getObject(KeystoreModel.class, new Request(new Columns.All()));
        if (model != null) {
            publicKey = KeyFactory.getInstance("EC")
                    .generatePublic(new X509EncodedKeySpec(model.getPublicKey()));
            privateKey = KeyFactory.getInstance("EC")
                    .generatePrivate(new PKCS8EncodedKeySpec(model.getPrivateKey()));
        } else {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("EC");
            generator.initialize(new ECGenParameterSpec("secp256r1"), new SecureRandom());
            KeyPair pair = generator.generateKeyPair();

            publicKey = pair.getPublic();
            privateKey = pair.getPrivate();

            model = new KeystoreModel();
            model.setPublicKey(publicKey.getEncoded());
            model.setPrivateKey(privateKey.getEncoded());
            storage.addObject(model, new Request(new Columns.Exclude("id")));
        }
    }

}
