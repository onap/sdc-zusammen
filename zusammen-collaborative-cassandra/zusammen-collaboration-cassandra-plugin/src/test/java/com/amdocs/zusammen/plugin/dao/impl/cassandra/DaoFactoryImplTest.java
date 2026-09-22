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

package com.amdocs.zusammen.plugin.dao.impl.cassandra;

import com.amdocs.zusammen.datatypes.SessionContext;
import com.amdocs.zusammen.datatypes.UserInfo;
import com.amdocs.zusammen.plugin.dao.ElementRepository;
import com.amdocs.zusammen.plugin.dao.ElementRepositoryFactory;
import com.amdocs.zusammen.plugin.dao.ElementStageRepository;
import com.amdocs.zusammen.plugin.dao.ElementStageRepositoryFactory;
import com.amdocs.zusammen.plugin.dao.ElementSynchronizationStateRepository;
import com.amdocs.zusammen.plugin.dao.ElementSynchronizationStateRepositoryFactory;
import com.amdocs.zusammen.plugin.dao.VersionDao;
import com.amdocs.zusammen.plugin.dao.VersionDaoFactory;
import com.amdocs.zusammen.plugin.dao.VersionStageRepository;
import com.amdocs.zusammen.plugin.dao.VersionStageRepositoryFactory;
import com.amdocs.zusammen.plugin.dao.VersionSynchronizationStateRepository;
import com.amdocs.zusammen.plugin.dao.VersionSynchronizationStateRepositoryFactory;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * The six {@code *FactoryImpl} classes of this package share one contract - resolve from
 * factoryConfiguration.json and hand out a single stateless repository instance regardless of the
 * session - so they are covered together rather than in six near-identical files.
 */
public class DaoFactoryImplTest {

    private SessionContext firstContext;
    private SessionContext secondContext;

    @BeforeMethod
    public void setUp() {
        // VersionStageRepositoryFactoryImpl's class initialiser builds a VersionStageRepositoryImpl,
        // whose own initialiser registers a codec through the Cassandra connector.
        CassandraAccessorSeam.install();
        firstContext = sessionContext("tenant-1");
        secondContext = sessionContext("tenant-2");
    }

    @AfterMethod
    public void tearDown() throws Exception {
        CassandraAccessorSeam.uninstall();
    }

    @Test
    public void testElementRepositoryFactoryResolvesToASingleCassandraRepository() {
        ElementRepositoryFactory factory = ElementRepositoryFactory.getInstance();
        Assert.assertTrue(factory instanceof ElementRepositoryFactoryImpl);

        ElementRepository repository = factory.createInterface(firstContext);
        Assert.assertTrue(repository instanceof ElementRepositoryImpl);
        Assert.assertSame(factory.createInterface(secondContext), repository);
    }

    @Test
    public void testElementStageRepositoryFactoryResolvesToASingleCassandraRepository() {
        ElementStageRepositoryFactory factory = ElementStageRepositoryFactory.getInstance();
        Assert.assertTrue(factory instanceof ElementStageRepositoryFactoryImpl);

        ElementStageRepository repository = factory.createInterface(firstContext);
        Assert.assertTrue(repository instanceof ElementStageRepositoryImpl);
        Assert.assertSame(factory.createInterface(secondContext), repository);
    }

    @Test
    public void testElementSynchronizationStateRepositoryFactoryResolvesToASingleCassandraRepository() {
        ElementSynchronizationStateRepositoryFactory factory =
                ElementSynchronizationStateRepositoryFactory.getInstance();
        Assert.assertTrue(factory instanceof ElementSynchronizationStateRepositoryFactoryImpl);

        ElementSynchronizationStateRepository repository = factory.createInterface(firstContext);
        Assert.assertTrue(repository instanceof ElementSynchronizationStateRepositoryImpl);
        Assert.assertSame(factory.createInterface(secondContext), repository);
    }

    @Test
    public void testVersionDaoFactoryResolvesToASingleCassandraDao() {
        VersionDaoFactory factory = VersionDaoFactory.getInstance();
        Assert.assertTrue(factory instanceof VersionDaoFactoryImpl);

        VersionDao versionDao = factory.createInterface(firstContext);
        Assert.assertTrue(versionDao instanceof VersionDaoImpl);
        Assert.assertSame(factory.createInterface(secondContext), versionDao);
    }

    @Test
    public void testVersionStageRepositoryFactoryResolvesToASingleCassandraRepository() {
        VersionStageRepositoryFactory factory = VersionStageRepositoryFactory.getInstance();
        Assert.assertTrue(factory instanceof VersionStageRepositoryFactoryImpl);

        VersionStageRepository repository = factory.createInterface(firstContext);
        Assert.assertTrue(repository instanceof VersionStageRepositoryImpl);
        Assert.assertSame(factory.createInterface(secondContext), repository);
    }

    @Test
    public void testVersionSynchronizationStateRepositoryFactoryResolvesToASingleCassandraRepository() {
        VersionSynchronizationStateRepositoryFactory factory =
                VersionSynchronizationStateRepositoryFactory.getInstance();
        Assert.assertTrue(factory instanceof VersionSynchronizationStateRepositoryFactoryImpl);

        VersionSynchronizationStateRepository repository = factory.createInterface(firstContext);
        Assert.assertTrue(repository instanceof VersionSynchronizationStateRepositoryImpl);
        Assert.assertSame(factory.createInterface(secondContext), repository);
    }

    private static SessionContext sessionContext(String tenant) {
        SessionContext context = new SessionContext();
        context.setUser(new UserInfo("DaoFactoryImplTest_user"));
        context.setTenant(tenant);
        return context;
    }
}
