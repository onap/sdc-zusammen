/*
 * Copyright © 2026 Deutsche Telekom AG
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

package com.amdocs.zusammen.commons.db.impl.cassandra;

import org.testng.Assert;
import org.testng.annotations.Test;

import javax.net.ssl.SSLContext;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyStore;

public class CassandraSslContextFactoryTest {

    private static final String TRUSTSTORE_PASSWORD = "truststore-password";

    @Test
    public void testTrustStoreYieldsTlsContext() throws Exception {
        Path truststore = createTrustStore(TRUSTSTORE_PASSWORD);
        try {
            SSLContext context =
                    CassandraSslContextFactory.createFromTrustStore(truststore.toString(), TRUSTSTORE_PASSWORD);

            Assert.assertEquals(context.getProtocol(), "TLS");
        } finally {
            Files.deleteIfExists(truststore);
        }
    }

    @Test
    public void testMissingTrustStoreThrows() {
        Path truststore = Paths.get(System.getProperty("java.io.tmpdir"), "no-such-cassandra-truststore.jks");

        RuntimeException exception = expectFailure(truststore, TRUSTSTORE_PASSWORD);

        Assert.assertTrue(exception.getMessage().contains(truststore.toString()),
                "the truststore path must be in the message, it is the whole point of the failure: "
                        + exception.getMessage());
    }

    @Test
    public void testTrustStoreThatIsNotAJksThrows() throws Exception {
        Path truststore = Files.createTempFile("cassandra-truststore", ".jks");
        Files.write(truststore, "this is not a keystore".getBytes(StandardCharsets.UTF_8));
        try {
            expectFailure(truststore, TRUSTSTORE_PASSWORD);
        } finally {
            Files.deleteIfExists(truststore);
        }
    }

    @Test
    public void testWrongTrustStorePasswordThrowsWithoutDisclosingIt() throws Exception {
        Path truststore = createTrustStore(TRUSTSTORE_PASSWORD);
        try {
            RuntimeException exception = expectFailure(truststore, "wrong-password");

            Assert.assertFalse(exception.getMessage().contains("wrong-password"),
                    "the truststore password must not reach the logs: " + exception.getMessage());
        } finally {
            Files.deleteIfExists(truststore);
        }
    }

    private static RuntimeException expectFailure(Path truststore, String password) {
        try {
            SSLContext context = CassandraSslContextFactory.createFromTrustStore(truststore.toString(), password);
            Assert.fail("expected a RuntimeException but got an SSL context: " + context);
            return null;
        } catch (RuntimeException e) {
            return e;
        }
    }

    private static Path createTrustStore(String password) throws Exception {
        Path truststore = Files.createTempFile("cassandra-truststore", ".jks");
        KeyStore keyStore = KeyStore.getInstance("JKS");
        keyStore.load(null, null);
        try (OutputStream out = Files.newOutputStream(truststore)) {
            keyStore.store(out, password.toCharArray());
        }
        return truststore;
    }
}
