/*
 * Copyright © 2016-2017 European Support Limited
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

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.FileInputStream;
import java.security.KeyStore;
import java.security.SecureRandom;

class CassandraSslContextFactory {

    private CassandraSslContextFactory() {
    }

    static SSLContext createFromTrustStore(String truststorePath, String truststorePassword) {
        try (FileInputStream truststoreStream = new FileInputStream(truststorePath)) {
            KeyStore truststore = KeyStore.getInstance("JKS");
            truststore.load(truststoreStream, truststorePassword.toCharArray());

            TrustManagerFactory trustManagerFactory =
                    TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            trustManagerFactory.init(truststore);

            SSLContext context = SSLContext.getInstance("TLS");
            context.init(null, trustManagerFactory.getTrustManagers(), new SecureRandom());
            return context;
        } catch (Exception e) {
            throw new RuntimeException(
                    "Unable to build SSL context from Cassandra truststore " + truststorePath, e);
        }
    }
}
