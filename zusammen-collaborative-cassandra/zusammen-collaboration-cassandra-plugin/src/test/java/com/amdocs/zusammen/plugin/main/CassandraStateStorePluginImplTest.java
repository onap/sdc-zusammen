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

package com.amdocs.zusammen.plugin.main;

import com.amdocs.zusammen.datatypes.Id;
import com.amdocs.zusammen.datatypes.Namespace;
import com.amdocs.zusammen.datatypes.SessionContext;
import com.amdocs.zusammen.datatypes.Space;
import com.amdocs.zusammen.datatypes.UserInfo;
import com.amdocs.zusammen.datatypes.item.ElementContext;
import com.amdocs.zusammen.datatypes.item.ItemVersion;
import com.amdocs.zusammen.datatypes.item.Relation;
import com.amdocs.zusammen.datatypes.response.Response;
import com.amdocs.zusammen.plugin.ZusammenPluginConstants;
import com.amdocs.zusammen.plugin.collaboration.ElementPrivateStore;
import com.amdocs.zusammen.plugin.collaboration.TestUtils;
import com.amdocs.zusammen.plugin.dao.ElementRepository;
import com.amdocs.zusammen.plugin.dao.ElementRepositoryFactory;
import com.amdocs.zusammen.plugin.dao.VersionDao;
import com.amdocs.zusammen.plugin.dao.VersionDaoFactory;
import com.amdocs.zusammen.plugin.dao.impl.cassandra.ElementRepositoryFactoryImpl;
import com.amdocs.zusammen.plugin.dao.impl.cassandra.VersionDaoFactoryImpl;
import com.amdocs.zusammen.plugin.dao.types.ElementEntity;
import com.amdocs.zusammen.plugin.dao.types.VersionEntity;
import com.amdocs.zusammen.plugin.statestore.cassandra.dao.ItemDao;
import com.amdocs.zusammen.plugin.statestore.cassandra.dao.ItemDaoFactory;
import com.amdocs.zusammen.plugin.statestore.cassandra.dao.impl.ItemCassandraDaoFactory;
import com.amdocs.zusammen.plugin.statestore.cassandra.dao.types.ElementEntityContext;
import com.amdocs.zusammen.sdk.state.types.StateElement;
import com.amdocs.zusammen.utils.facade.api.AbstractComponentFactory;
import com.amdocs.zusammen.utils.facade.impl.AbstractFactoryBase;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class CassandraStateStorePluginImplTest {

    private static final String USER = "CassandraStateStorePluginImplTest_user";
    private static final String TENANT = "CassandraStateStorePluginImplTest_tenant";

    private static final Id ITEM_ID = new Id("item-1");
    private static final Id VERSION_ID = new Id("version-2");
    private static final Id BASE_VERSION_ID = new Id("base-version-3");
    private static final Id ELEMENT_ID = new Id("element-4");
    private static final Id SUB_ELEMENT_ID = new Id("sub-element-5");

    private static final SessionContext context =
            TestUtils.createSessionContext(new UserInfo(USER), TENANT);

    @Mock
    private ElementPrivateStore elementPrivateStore;
    @Mock
    private ItemDao itemDao;
    @Mock
    private VersionDao versionDao;
    @Mock
    private ElementRepository elementRepository;

    @InjectMocks
    private CassandraStateStorePluginImpl plugin;

    private AutoCloseable mocks;

    @BeforeMethod
    public void setUp() throws Exception {
        mocks = MockitoAnnotations.openMocks(this);

        // AbstractComponentFactory's one-shot static initialiser loads every factoryConfiguration.json
        // into the same static registry that registerFactory writes to, and a class literal does not
        // trigger class initialisation. Unless it is forced to run first, it runs when the first
        // factory is actually resolved and overwrites the stubs registered below.
        Class.forName(AbstractComponentFactory.class.getName());

        StubItemDaoFactory.itemDao = itemDao;
        StubVersionDaoFactory.versionDao = versionDao;
        StubElementRepositoryFactory.elementRepository = elementRepository;
        AbstractFactoryBase.registerFactory(ItemDaoFactory.class, StubItemDaoFactory.class);
        AbstractFactoryBase.registerFactory(VersionDaoFactory.class, StubVersionDaoFactory.class);
        AbstractFactoryBase
                .registerFactory(ElementRepositoryFactory.class, StubElementRepositoryFactory.class);
    }

    /**
     * The registry has no removal API and its one-shot initialiser cannot be re-run, so the production
     * mappings have to be put back by hand - otherwise every later test class in this JVM resolves
     * these three factories to the stubs left behind here.
     */
    @AfterMethod
    public void tearDown() throws Exception {
        AbstractFactoryBase.registerFactory(ItemDaoFactory.class, ItemCassandraDaoFactory.class);
        AbstractFactoryBase.registerFactory(VersionDaoFactory.class, VersionDaoFactoryImpl.class);
        AbstractFactoryBase
                .registerFactory(ElementRepositoryFactory.class, ElementRepositoryFactoryImpl.class);
        StubItemDaoFactory.itemDao = null;
        StubVersionDaoFactory.versionDao = null;
        StubElementRepositoryFactory.elementRepository = null;
        mocks.close();
    }

    /**
     * The subject hard-instantiates its element store as a private field, so it is only replaced by
     * reflection - a {@code @Mock} whose type matches no field is skipped silently, which would leave
     * the real store and its Cassandra calls in place.
     */
    @Test
    public void testElementPrivateStoreIsReplacedByAMock() throws Exception {
        Field field =
                CassandraStateStorePluginImpl.class.getDeclaredField("elementPrivateStore");
        field.setAccessible(true);

        Assert.assertTrue(Mockito.mockingDetails(field.get(plugin)).isMock(),
                "elementPrivateStore was not replaced by a mock");
    }

    @Test
    public void testDeleteItemDeletesOnlyTheItem() {
        Response<Void> response = plugin.deleteItem(context, ITEM_ID);

        Assert.assertTrue(response.isSuccessful());
        verify(itemDao).delete(context, ITEM_ID);
        Mockito.verifyNoInteractions(versionDao, elementPrivateStore);
    }

    @Test
    public void testListItemVersionsOfPublicSpace() {
        VersionEntity versionEntity = versionEntity();
        when(versionDao.list(context, ZusammenPluginConstants.PUBLIC_SPACE, ITEM_ID))
                .thenReturn(Collections.singletonList(versionEntity));
        versionDataExistsFor(ZusammenPluginConstants.PUBLIC_SPACE);

        Response<Collection<ItemVersion>> response =
                plugin.listItemVersions(context, Space.PUBLIC, ITEM_ID);

        Assert.assertEquals(response.getValue().size(), 1);
        ItemVersion itemVersion = response.getValue().iterator().next();
        Assert.assertEquals(itemVersion.getId(), VERSION_ID);
        Assert.assertEquals(itemVersion.getBaseId(), BASE_VERSION_ID);
        Assert.assertEquals(itemVersion.getCreationTime(), new Date(1000L));
        Assert.assertEquals(itemVersion.getModificationTime(), new Date(2000L));
        Assert.assertNotNull(itemVersion.getData().getInfo());

        ElementEntityContext versionDataContext = versionDataQueryContext();
        Assert.assertEquals(versionDataContext.getSpace(), ZusammenPluginConstants.PUBLIC_SPACE);
        Assert.assertEquals(versionDataContext.getItemId(), ITEM_ID);
        Assert.assertEquals(versionDataContext.getVersionId(), VERSION_ID);
        Assert.assertNull(versionDataContext.getRevisionId());
    }

    @Test
    public void testListItemVersionsOfPrivateSpaceUsesTheUserName() {
        when(versionDao.list(context, USER, ITEM_ID))
                .thenReturn(Collections.singletonList(versionEntity()));
        versionDataExistsFor(USER);

        Response<Collection<ItemVersion>> response =
                plugin.listItemVersions(context, Space.PRIVATE, ITEM_ID);

        Assert.assertEquals(response.getValue().size(), 1);
        Assert.assertEquals(versionDataQueryContext().getSpace(), USER);
    }

    @Test
    public void testListItemVersionsWhenItemHasNoVersions() {
        when(versionDao.list(context, USER, ITEM_ID)).thenReturn(Collections.emptyList());

        Response<Collection<ItemVersion>> response =
                plugin.listItemVersions(context, Space.PRIVATE, ITEM_ID);

        Assert.assertTrue(response.getValue().isEmpty());
        Mockito.verifyNoInteractions(elementRepository);
    }

    @Test(expectedExceptions = IllegalStateException.class,
            expectedExceptionsMessageRegExp = "Version must have data")
    public void testListItemVersionsFailsWhenAVersionHasNoData() {
        when(versionDao.list(context, USER, ITEM_ID))
                .thenReturn(Collections.singletonList(versionEntity()));
        when(elementRepository.get(eq(context), any(ElementEntityContext.class),
                any(ElementEntity.class))).thenReturn(Optional.empty());

        plugin.listItemVersions(context, Space.PRIVATE, ITEM_ID);
    }

    @Test
    public void testIsItemVersionExistWhenItDoes() {
        when(versionDao.get(context, ZusammenPluginConstants.PUBLIC_SPACE, ITEM_ID, VERSION_ID))
                .thenReturn(Optional.of(versionEntity()));

        Response<Boolean> response =
                plugin.isItemVersionExist(context, Space.PUBLIC, ITEM_ID, VERSION_ID);

        Assert.assertTrue(response.getValue());
    }

    @Test
    public void testIsItemVersionExistWhenItDoesNot() {
        when(versionDao.get(context, USER, ITEM_ID, VERSION_ID)).thenReturn(Optional.empty());

        Response<Boolean> response =
                plugin.isItemVersionExist(context, Space.PRIVATE, ITEM_ID, VERSION_ID);

        Assert.assertFalse(response.getValue());
    }

    @Test
    public void testGetItemVersion() {
        when(versionDao.get(context, USER, ITEM_ID, VERSION_ID))
                .thenReturn(Optional.of(versionEntity()));
        versionDataExistsFor(USER);

        Response<ItemVersion> response =
                plugin.getItemVersion(context, Space.PRIVATE, ITEM_ID, VERSION_ID);

        Assert.assertEquals(response.getValue().getId(), VERSION_ID);
        Assert.assertEquals(response.getValue().getBaseId(), BASE_VERSION_ID);
        Assert.assertNotNull(response.getValue().getData().getInfo());
        Assert.assertEquals(versionDataQueryContext().getSpace(), USER);
    }

    @Test
    public void testGetItemVersionWhenNotFound() {
        when(versionDao.get(context, USER, ITEM_ID, VERSION_ID)).thenReturn(Optional.empty());

        Response<ItemVersion> response =
                plugin.getItemVersion(context, Space.PRIVATE, ITEM_ID, VERSION_ID);

        Assert.assertTrue(response.isSuccessful());
        Assert.assertNull(response.getValue());
        Mockito.verifyNoInteractions(elementRepository);
    }

    @Test
    public void testCreateItemVersionIsDoneByTheCollaborationStore() {
        Response<Void> response = plugin.createItemVersion(context, Space.PRIVATE, ITEM_ID,
                BASE_VERSION_ID, VERSION_ID, null, new Date(3000L));

        Assert.assertTrue(response.isSuccessful());
        Mockito.verifyNoInteractions(versionDao, elementRepository, elementPrivateStore, itemDao);
    }

    @Test
    public void testUpdateItemVersionIsDoneByTheCollaborationStore() {
        Response<Void> response = plugin.updateItemVersion(context, Space.PRIVATE, ITEM_ID,
                VERSION_ID, null, new Date(4000L));

        Assert.assertTrue(response.isSuccessful());
        Mockito.verifyNoInteractions(versionDao, elementRepository, elementPrivateStore, itemDao);
    }

    @Test
    public void testDeleteItemVersionIsDoneByTheCollaborationStore() {
        Response<Void> response =
                plugin.deleteItemVersion(context, Space.PRIVATE, ITEM_ID, VERSION_ID);

        Assert.assertTrue(response.isSuccessful());
        Mockito.verifyNoInteractions(versionDao, elementRepository, elementPrivateStore, itemDao);
    }

    @Test
    public void testCreateElementCreatesTheNamespaceInThePrivateSpace() {
        StateElement element = stateElement(Space.PRIVATE);

        Response<Void> response = plugin.createElement(context, element);

        Assert.assertTrue(response.isSuccessful());
        ArgumentCaptor<ElementEntityContext> contextCaptor =
                ArgumentCaptor.forClass(ElementEntityContext.class);
        ArgumentCaptor<ElementEntity> elementCaptor = ArgumentCaptor.forClass(ElementEntity.class);
        verify(elementRepository)
                .createNamespace(eq(context), contextCaptor.capture(), elementCaptor.capture());
        Assert.assertEquals(contextCaptor.getValue().getSpace(), USER);
        Assert.assertEquals(contextCaptor.getValue().getItemId(), ITEM_ID);
        Assert.assertEquals(contextCaptor.getValue().getVersionId(), VERSION_ID);
        Assert.assertEquals(elementCaptor.getValue().getId(), ELEMENT_ID);
        Assert.assertEquals(elementCaptor.getValue().getNamespace(), element.getNamespace());
    }

    @Test
    public void testCreateElementOfPublicSpaceElement() {
        plugin.createElement(context, stateElement(Space.PUBLIC));

        ArgumentCaptor<ElementEntityContext> contextCaptor =
                ArgumentCaptor.forClass(ElementEntityContext.class);
        verify(elementRepository)
                .createNamespace(eq(context), contextCaptor.capture(), any(ElementEntity.class));
        Assert.assertEquals(contextCaptor.getValue().getSpace(),
                ZusammenPluginConstants.PUBLIC_SPACE);
    }

    @Test
    public void testUpdateElementIsDoneByTheCollaborationStore() {
        Response<Void> response = plugin.updateElement(context, stateElement(Space.PRIVATE));

        Assert.assertTrue(response.isSuccessful());
        Mockito.verifyNoInteractions(elementRepository, elementPrivateStore);
    }

    @Test
    public void testDeleteElementIsDoneByTheCollaborationStore() {
        Response<Void> response = plugin.deleteElement(context, stateElement(Space.PRIVATE));

        Assert.assertTrue(response.isSuccessful());
        Mockito.verifyNoInteractions(elementRepository, elementPrivateStore);
    }

    @Test
    public void testListElements() {
        ElementContext elementContext = new ElementContext(ITEM_ID, VERSION_ID);
        ElementEntity subElement = new ElementEntity(SUB_ELEMENT_ID);
        subElement.setParentId(ELEMENT_ID);
        subElement.setNamespace(namespace("sub-element-namespace"));
        subElement.setInfo(TestUtils.createInfo("sub-element"));
        subElement.setRelations(Arrays.asList(new Relation()));
        subElement.setSubElementIds(new HashSet<>(Collections.singletonList(new Id("leaf-6"))));
        when(elementPrivateStore.listSubs(context, elementContext, ELEMENT_ID))
                .thenReturn(Collections.singletonList(subElement));

        Response<Collection<StateElement>> response =
                plugin.listElements(context, elementContext, ELEMENT_ID);

        Assert.assertEquals(response.getValue().size(), 1);
        StateElement stateElement = response.getValue().iterator().next();
        Assert.assertEquals(stateElement.getId(), SUB_ELEMENT_ID);
        Assert.assertEquals(stateElement.getParentId(), ELEMENT_ID);
        Assert.assertEquals(stateElement.getItemId(), ITEM_ID);
        Assert.assertEquals(stateElement.getVersionId(), VERSION_ID);
        Assert.assertEquals(stateElement.getNamespace(), subElement.getNamespace());
        Assert.assertSame(stateElement.getInfo(), subElement.getInfo());
        Assert.assertEquals(stateElement.getSubElements(), subElement.getSubElementIds());
    }

    @Test
    public void testListElementsWhenThereAreNoSubElements() {
        ElementContext elementContext = new ElementContext(ITEM_ID, VERSION_ID);
        when(elementPrivateStore.listSubs(context, elementContext, ELEMENT_ID))
                .thenReturn(Collections.emptyList());

        Response<Collection<StateElement>> response =
                plugin.listElements(context, elementContext, ELEMENT_ID);

        Assert.assertTrue(response.getValue().isEmpty());
    }

    @Test
    public void testGetElement() {
        ElementContext elementContext = new ElementContext(ITEM_ID, VERSION_ID);
        ElementEntity elementEntity = new ElementEntity(ELEMENT_ID);
        elementEntity.setParentId(ZusammenPluginConstants.ROOT_ELEMENTS_PARENT_ID);
        elementEntity.setNamespace(namespace("element-namespace"));
        when(elementPrivateStore.get(context, elementContext, ELEMENT_ID))
                .thenReturn(Optional.of(elementEntity));

        Response<StateElement> response = plugin.getElement(context, elementContext, ELEMENT_ID);

        Assert.assertEquals(response.getValue().getId(), ELEMENT_ID);
        Assert.assertEquals(response.getValue().getNamespace(), elementEntity.getNamespace());
        Assert.assertNull(response.getValue().getParentId());
    }

    @Test
    public void testGetElementWhenNotFound() {
        ElementContext elementContext = new ElementContext(ITEM_ID, VERSION_ID);
        when(elementPrivateStore.get(context, elementContext, ELEMENT_ID))
                .thenReturn(Optional.empty());

        Response<StateElement> response = plugin.getElement(context, elementContext, ELEMENT_ID);

        Assert.assertTrue(response.isSuccessful());
        Assert.assertNull(response.getValue());
    }

    private void versionDataExistsFor(String spaceName) {
        ElementEntity versionData = new ElementEntity(ZusammenPluginConstants.ROOT_ELEMENTS_PARENT_ID);
        versionData.setInfo(TestUtils.createInfo("version-data"));
        versionData.setRelations(Arrays.asList(new Relation()));
        when(elementRepository.get(eq(context),
                eq(new ElementEntityContext(spaceName, ITEM_ID, VERSION_ID)),
                any(ElementEntity.class))).thenReturn(Optional.of(versionData));
    }

    private ElementEntityContext versionDataQueryContext() {
        ArgumentCaptor<ElementEntityContext> captor =
                ArgumentCaptor.forClass(ElementEntityContext.class);
        verify(elementRepository).get(eq(context), captor.capture(), any(ElementEntity.class));
        return captor.getValue();
    }

    private static VersionEntity versionEntity() {
        VersionEntity versionEntity = new VersionEntity(VERSION_ID);
        versionEntity.setBaseId(BASE_VERSION_ID);
        versionEntity.setCreationTime(new Date(1000L));
        versionEntity.setModificationTime(new Date(2000L));
        return versionEntity;
    }

    private static StateElement stateElement(Space space) {
        StateElement element =
                new StateElement(ITEM_ID, VERSION_ID, namespace("state-element-namespace"),
                        ELEMENT_ID);
        element.setSpace(space);
        return element;
    }

    private static Namespace namespace(String value) {
        Namespace namespace = new Namespace();
        namespace.setValue(value);
        return namespace;
    }

    public static class StubItemDaoFactory extends ItemDaoFactory {
        private static ItemDao itemDao;

        @Override
        public ItemDao createInterface(SessionContext context) {
            return itemDao;
        }
    }

    public static class StubVersionDaoFactory extends VersionDaoFactory {
        private static VersionDao versionDao;

        @Override
        public VersionDao createInterface(SessionContext context) {
            return versionDao;
        }
    }

    public static class StubElementRepositoryFactory extends ElementRepositoryFactory {
        private static ElementRepository elementRepository;

        @Override
        public ElementRepository createInterface(SessionContext context) {
            return elementRepository;
        }
    }
}
