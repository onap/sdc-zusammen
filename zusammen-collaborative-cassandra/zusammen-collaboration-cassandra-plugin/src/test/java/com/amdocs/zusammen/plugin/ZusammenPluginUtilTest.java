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

package com.amdocs.zusammen.plugin;

import com.amdocs.zusammen.datatypes.Id;
import com.amdocs.zusammen.datatypes.Namespace;
import com.amdocs.zusammen.datatypes.SessionContext;
import com.amdocs.zusammen.datatypes.Space;
import com.amdocs.zusammen.datatypes.UserInfo;
import com.amdocs.zusammen.datatypes.item.Action;
import com.amdocs.zusammen.datatypes.item.ElementContext;
import com.amdocs.zusammen.datatypes.item.Info;
import com.amdocs.zusammen.datatypes.item.ItemVersion;
import com.amdocs.zusammen.datatypes.item.ItemVersionChange;
import com.amdocs.zusammen.datatypes.item.ItemVersionData;
import com.amdocs.zusammen.datatypes.item.ItemVersionDataConflict;
import com.amdocs.zusammen.datatypes.item.Relation;
import com.amdocs.zusammen.plugin.collaboration.TestUtils;
import com.amdocs.zusammen.plugin.dao.types.ElementEntity;
import com.amdocs.zusammen.plugin.dao.types.VersionEntity;
import com.amdocs.zusammen.sdk.collaboration.types.CollaborationElement;
import com.amdocs.zusammen.sdk.collaboration.types.CollaborationElementChange;
import com.amdocs.zusammen.sdk.collaboration.types.CollaborationElementConflict;
import com.amdocs.zusammen.sdk.state.types.StateElement;
import com.amdocs.zusammen.sdk.types.ElementDescriptor;
import com.amdocs.zusammen.utils.fileutils.FileUtils;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

public class ZusammenPluginUtilTest {

    private static final String USER = "ZusammenPluginUtilTest_user";
    private static final String TENANT = "ZusammenPluginUtilTest_tenant";

    private static final Id ITEM_ID = new Id("item-1");
    private static final Id VERSION_ID = new Id("version-2");
    private static final Id REVISION_ID = new Id("revision-3");
    private static final Id BASE_VERSION_ID = new Id("base-version-4");
    private static final Id ELEMENT_ID = new Id("element-5");
    private static final Id PARENT_ID = new Id("parent-6");
    private static final Id SUB_ELEMENT_ID_A = new Id("sub-element-7");
    private static final Id SUB_ELEMENT_ID_B = new Id("sub-element-8");
    private static final Id ELEMENT_HASH = new Id("element-hash-9");

    private static final String DATA = "element-data";
    private static final String SEARCHABLE_DATA = "element-searchable-data";
    private static final String VISUALIZATION = "element-visualization";

    @Test
    public void testGetSpaceNameOfPublic() {
        Assert.assertEquals(ZusammenPluginUtil.getSpaceName(sessionContext(), Space.PUBLIC),
                ZusammenPluginConstants.PUBLIC_SPACE);
    }

    @Test
    public void testGetSpaceNameOfPrivateIsTheUserName() {
        Assert.assertEquals(ZusammenPluginUtil.getSpaceName(sessionContext(), Space.PRIVATE), USER);
    }

    @Test(expectedExceptions = IllegalArgumentException.class,
            expectedExceptionsMessageRegExp = "Space BOTH is not supported\\.")
    public void testGetSpaceNameOfBothIsRejected() {
        ZusammenPluginUtil.getSpaceName(sessionContext(), Space.BOTH);
    }

    @Test
    public void testGetPrivateSpaceName() {
        Assert.assertEquals(ZusammenPluginUtil.getPrivateSpaceName(sessionContext()), USER);
    }

    @Test
    public void testGetPrivateElementContextForcesZeroRevision() {
        ElementContext privateContext = ZusammenPluginUtil
                .getPrivateElementContext(new ElementContext(ITEM_ID, VERSION_ID, REVISION_ID));

        Assert.assertEquals(privateContext.getItemId(), ITEM_ID);
        Assert.assertEquals(privateContext.getVersionId(), VERSION_ID);
        Assert.assertEquals(privateContext.getRevisionId(), Id.ZERO);
    }

    @Test
    public void testConvertToVersionEntity() {
        Date creationTime = new Date(1000L);
        Date modificationTime = new Date(2000L);

        VersionEntity version = ZusammenPluginUtil
                .convertToVersionEntity(VERSION_ID, BASE_VERSION_ID, creationTime, modificationTime);

        Assert.assertEquals(version.getId(), VERSION_ID);
        Assert.assertEquals(version.getBaseId(), BASE_VERSION_ID);
        Assert.assertEquals(version.getCreationTime(), creationTime);
        Assert.assertEquals(version.getModificationTime(), modificationTime);
    }

    @Test
    public void testConvertToVersionEntityWithRevisionId() {
        Date creationTime = new Date(3000L);
        Date modificationTime = new Date(4000L);

        VersionEntity version = ZusammenPluginUtil.convertToVersionEntity(VERSION_ID, REVISION_ID,
                BASE_VERSION_ID, creationTime, modificationTime);

        Assert.assertEquals(version.getId(), VERSION_ID);
        Assert.assertEquals(version.getBaseId(), BASE_VERSION_ID);
        Assert.assertEquals(version.getCreationTime(), creationTime);
        Assert.assertEquals(version.getModificationTime(), modificationTime);
    }

    @Test
    public void testConvertToItemVersion() {
        VersionEntity versionEntity = new VersionEntity(VERSION_ID);
        versionEntity.setBaseId(BASE_VERSION_ID);
        versionEntity.setCreationTime(new Date(5000L));
        versionEntity.setModificationTime(new Date(6000L));
        ItemVersionData data = itemVersionData("item-version-data");

        ItemVersion itemVersion = ZusammenPluginUtil.convertToItemVersion(versionEntity, data);

        Assert.assertEquals(itemVersion.getId(), VERSION_ID);
        Assert.assertEquals(itemVersion.getBaseId(), BASE_VERSION_ID);
        Assert.assertEquals(itemVersion.getCreationTime(), new Date(5000L));
        Assert.assertEquals(itemVersion.getModificationTime(), new Date(6000L));
        Assert.assertSame(itemVersion.getData(), data);
    }

    @Test
    public void testConvertToElementEntity() {
        CollaborationElement element = fullyPopulatedCollaborationElement();

        ElementEntity elementEntity = ZusammenPluginUtil.convertToElementEntity(element);

        Assert.assertEquals(elementEntity.getId(), ELEMENT_ID);
        Assert.assertEquals(elementEntity.getParentId(), PARENT_ID);
        Assert.assertEquals(elementEntity.getNamespace(), namespace());
        Assert.assertSame(elementEntity.getInfo(), element.getInfo());
        Assert.assertSame(elementEntity.getRelations(), element.getRelations());
        Assert.assertEquals(asString(elementEntity.getData()), DATA);
        Assert.assertEquals(asString(elementEntity.getSearchableData()), SEARCHABLE_DATA);
        Assert.assertEquals(asString(elementEntity.getVisualization()), VISUALIZATION);
        Assert.assertEquals(elementEntity.getElementHash(),
                new Id(ZusammenPluginUtil.calculateElementHash(hashSourceOf(element))));
    }

    @Test
    public void testConvertToElementEntityOfRootElementSetsRootParentId() {
        CollaborationElement element =
                new CollaborationElement(ITEM_ID, VERSION_ID, namespace(), ELEMENT_ID);

        ElementEntity elementEntity = ZusammenPluginUtil.convertToElementEntity(element);

        Assert.assertEquals(elementEntity.getParentId(),
                ZusammenPluginConstants.ROOT_ELEMENTS_PARENT_ID);
    }

    @Test
    public void testConvertToElementEntityWithoutContent() {
        CollaborationElement element =
                new CollaborationElement(ITEM_ID, VERSION_ID, namespace(), ELEMENT_ID);
        element.setParentId(PARENT_ID);

        ElementEntity elementEntity = ZusammenPluginUtil.convertToElementEntity(element);

        Assert.assertNull(elementEntity.getData());
        Assert.assertNull(elementEntity.getSearchableData());
        Assert.assertNull(elementEntity.getVisualization());
        Assert.assertNull(elementEntity.getInfo());
        Assert.assertTrue(elementEntity.getElementHash().getValue().startsWith("0_0_0_0_"),
                "content-less element must hash the four missing parts as zeros, got "
                        + elementEntity.getElementHash().getValue());
    }

    @Test
    public void testConvertToElementDescriptorOfNullEntity() {
        Assert.assertNull(ZusammenPluginUtil
                .convertToElementDescriptor(new ElementContext(ITEM_ID, VERSION_ID), null));
    }

    @Test
    public void testConvertToElementDescriptor() {
        ElementEntity elementEntity = fullyPopulatedElementEntity();

        ElementDescriptor descriptor = ZusammenPluginUtil
                .convertToElementDescriptor(new ElementContext(ITEM_ID, VERSION_ID), elementEntity);

        Assert.assertEquals(descriptor.getItemId(), ITEM_ID);
        Assert.assertEquals(descriptor.getVersionId(), VERSION_ID);
        Assert.assertEquals(descriptor.getNamespace(), namespace());
        Assert.assertEquals(descriptor.getId(), ELEMENT_ID);
        Assert.assertEquals(descriptor.getParentId(), PARENT_ID);
        Assert.assertSame(descriptor.getInfo(), elementEntity.getInfo());
        Assert.assertSame(descriptor.getRelations(), elementEntity.getRelations());
        Assert.assertEquals(descriptor.getSubElements(), subElementIds());
    }

    @Test
    public void testConvertToElementDescriptorOfRootElementHasNoParentId() {
        ElementEntity elementEntity = new ElementEntity(ELEMENT_ID);
        elementEntity.setParentId(ZusammenPluginConstants.ROOT_ELEMENTS_PARENT_ID);

        ElementDescriptor descriptor = ZusammenPluginUtil
                .convertToElementDescriptor(new ElementContext(ITEM_ID, VERSION_ID), elementEntity);

        Assert.assertNull(descriptor.getParentId());
    }

    @Test
    public void testConvertToCollaborationElement() {
        ElementEntity elementEntity = fullyPopulatedElementEntity();

        CollaborationElement element = ZusammenPluginUtil.convertToCollaborationElement(
                new ElementContext(ITEM_ID, VERSION_ID), elementEntity);

        Assert.assertEquals(element.getItemId(), ITEM_ID);
        Assert.assertEquals(element.getVersionId(), VERSION_ID);
        Assert.assertEquals(element.getNamespace(), namespace());
        Assert.assertEquals(element.getId(), ELEMENT_ID);
        Assert.assertEquals(element.getParentId(), PARENT_ID);
        Assert.assertSame(element.getInfo(), elementEntity.getInfo());
        Assert.assertSame(element.getRelations(), elementEntity.getRelations());
        Assert.assertEquals(element.getSubElements(), subElementIds());
        Assert.assertEquals(asString(element.getData()), DATA);
        Assert.assertEquals(asString(element.getSearchableData()), SEARCHABLE_DATA);
        Assert.assertEquals(asString(element.getVisualization()), VISUALIZATION);
    }

    @Test
    public void testConvertToCollaborationElementWithoutContent() {
        ElementEntity elementEntity = new ElementEntity(ELEMENT_ID);
        elementEntity.setParentId(PARENT_ID);

        CollaborationElement element = ZusammenPluginUtil.convertToCollaborationElement(
                new ElementContext(ITEM_ID, VERSION_ID), elementEntity);

        Assert.assertNull(element.getData());
        Assert.assertNull(element.getSearchableData());
        Assert.assertNull(element.getVisualization());
    }

    @Test
    public void testConvertToElementChange() {
        ElementEntity elementEntity = fullyPopulatedElementEntity();

        CollaborationElementChange change = ZusammenPluginUtil.convertToElementChange(
                new ElementContext(ITEM_ID, VERSION_ID), elementEntity, Action.UPDATE);

        Assert.assertEquals(change.getAction(), Action.UPDATE);
        Assert.assertEquals(change.getElement().getId(), ELEMENT_ID);
        Assert.assertEquals(change.getElement().getItemId(), ITEM_ID);
        Assert.assertEquals(change.getElement().getVersionId(), VERSION_ID);
        Assert.assertEquals(asString(change.getElement().getData()), DATA);
    }

    @Test
    public void testConvertToVersionChange() {
        ElementEntity versionDataElement = new ElementEntity(Id.ZERO);
        Info info = TestUtils.createInfo("version-change");
        Collection<Relation> relations = relations("version-change-relation");
        versionDataElement.setInfo(info);
        versionDataElement.setRelations(relations);

        ItemVersionChange versionChange = ZusammenPluginUtil.convertToVersionChange(
                new ElementContext(ITEM_ID, VERSION_ID), versionDataElement, Action.CREATE);

        Assert.assertEquals(versionChange.getAction(), Action.CREATE);
        Assert.assertEquals(versionChange.getItemVersion().getId(), VERSION_ID);
        Assert.assertSame(versionChange.getItemVersion().getData().getInfo(), info);
        Assert.assertSame(versionChange.getItemVersion().getData().getRelations(), relations);
    }

    @Test
    public void testGetVersionConflict() {
        ElementEntity localVersionData = new ElementEntity(Id.ZERO);
        localVersionData.setInfo(TestUtils.createInfo("local-version"));
        ElementEntity remoteVersionData = new ElementEntity(Id.ZERO);
        remoteVersionData.setInfo(TestUtils.createInfo("remote-version"));

        ItemVersionDataConflict conflict =
                ZusammenPluginUtil.getVersionConflict(localVersionData, remoteVersionData);

        Assert.assertSame(conflict.getLocalData().getInfo(), localVersionData.getInfo());
        Assert.assertSame(conflict.getRemoteData().getInfo(), remoteVersionData.getInfo());
    }

    @Test
    public void testGetElementConflict() {
        ElementEntity localElement = new ElementEntity(ELEMENT_ID);
        localElement.setInfo(TestUtils.createInfo("local-element"));
        ElementEntity remoteElement = new ElementEntity(SUB_ELEMENT_ID_A);
        remoteElement.setInfo(TestUtils.createInfo("remote-element"));

        CollaborationElementConflict conflict = ZusammenPluginUtil
                .getElementConflict(new ElementContext(ITEM_ID, VERSION_ID), localElement,
                        remoteElement);

        Assert.assertEquals(conflict.getLocalElement().getId(), ELEMENT_ID);
        Assert.assertSame(conflict.getLocalElement().getInfo(), localElement.getInfo());
        Assert.assertEquals(conflict.getRemoteElement().getId(), SUB_ELEMENT_ID_A);
        Assert.assertSame(conflict.getRemoteElement().getInfo(), remoteElement.getInfo());
    }

    @Test
    public void testConvertToVersionData() {
        ElementEntity versionDataElement = new ElementEntity(Id.ZERO);
        versionDataElement.setInfo(TestUtils.createInfo("version-data"));
        versionDataElement.setRelations(relations("version-data-relation"));

        ItemVersionData versionData = ZusammenPluginUtil.convertToVersionData(versionDataElement);

        Assert.assertSame(versionData.getInfo(), versionDataElement.getInfo());
        Assert.assertSame(versionData.getRelations(), versionDataElement.getRelations());
    }

    @Test
    public void testCalculateElementHashOfEqualContentIsEqual() {
        Assert.assertEquals(ZusammenPluginUtil.calculateElementHash(fullyPopulatedElementEntity()),
                ZusammenPluginUtil.calculateElementHash(fullyPopulatedElementEntity()));
    }

    @Test
    public void testCalculateElementHashHasAPartPerHashedField() {
        String hash = ZusammenPluginUtil.calculateElementHash(fullyPopulatedElementEntity());

        Assert.assertEquals(hash.split("_").length, 5);
        for (String part : hash.split("_")) {
            Assert.assertNotEquals(part, "0", "every part of a fully populated element is hashed");
        }
    }

    @Test
    public void testCalculateElementHashDiffersWhenDataDiffers() {
        ElementEntity other = fullyPopulatedElementEntity();
        other.setData(ByteBuffer.wrap("other-data".getBytes(StandardCharsets.UTF_8)));

        Assert.assertNotEquals(ZusammenPluginUtil.calculateElementHash(other),
                ZusammenPluginUtil.calculateElementHash(fullyPopulatedElementEntity()));
    }

    @Test
    public void testCalculateElementHashDiffersWhenInfoDiffers() {
        ElementEntity other = fullyPopulatedElementEntity();
        other.setInfo(TestUtils.createInfo("other-info"));

        Assert.assertNotEquals(ZusammenPluginUtil.calculateElementHash(other),
                ZusammenPluginUtil.calculateElementHash(fullyPopulatedElementEntity()));
    }

    @Test
    public void testCalculateElementHashDiffersWhenVisualizationDiffers() {
        ElementEntity other = fullyPopulatedElementEntity();
        other.setVisualization(
                ByteBuffer.wrap("other-visualization".getBytes(StandardCharsets.UTF_8)));

        Assert.assertNotEquals(ZusammenPluginUtil.calculateElementHash(other),
                ZusammenPluginUtil.calculateElementHash(fullyPopulatedElementEntity()));
    }

    @Test
    public void testCalculateElementHashDiffersWhenSearchableDataDiffers() {
        ElementEntity other = fullyPopulatedElementEntity();
        other.setSearchableData(
                ByteBuffer.wrap("other-searchable-data".getBytes(StandardCharsets.UTF_8)));

        Assert.assertNotEquals(ZusammenPluginUtil.calculateElementHash(other),
                ZusammenPluginUtil.calculateElementHash(fullyPopulatedElementEntity()));
    }

    @Test
    public void testCalculateElementHashDiffersWhenRelationsDiffer() {
        ElementEntity other = fullyPopulatedElementEntity();
        other.setRelations(relations("other-relation"));

        Assert.assertNotEquals(ZusammenPluginUtil.calculateElementHash(other),
                ZusammenPluginUtil.calculateElementHash(fullyPopulatedElementEntity()));
    }

    @Test
    public void testCalculateElementHashOfEmptyElementHashesMissingFieldsAsZero() {
        ElementEntity empty = new ElementEntity(ELEMENT_ID);
        empty.setRelations(null);

        Assert.assertEquals(ZusammenPluginUtil.calculateElementHash(empty), "0_0_0_0_0");
    }

    @Test
    public void testGetStateElement() {
        ElementEntity elementEntity = fullyPopulatedElementEntity();

        StateElement stateElement = ZusammenPluginUtil
                .getStateElement(new ElementContext(ITEM_ID, VERSION_ID), elementEntity);

        Assert.assertEquals(stateElement.getItemId(), ITEM_ID);
        Assert.assertEquals(stateElement.getVersionId(), VERSION_ID);
        Assert.assertEquals(stateElement.getNamespace(), namespace());
        Assert.assertEquals(stateElement.getId(), ELEMENT_ID);
        Assert.assertEquals(stateElement.getParentId(), PARENT_ID);
        Assert.assertSame(stateElement.getInfo(), elementEntity.getInfo());
        Assert.assertSame(stateElement.getRelations(), elementEntity.getRelations());
        Assert.assertEquals(stateElement.getSubElements(), subElementIds());
        Assert.assertEquals(stateElement.getSpace(), Space.PRIVATE);
    }

    @Test
    public void testGetStateElementOfRootElementHasNoParentId() {
        ElementEntity elementEntity = new ElementEntity(ELEMENT_ID);
        elementEntity.setParentId(ZusammenPluginConstants.ROOT_ELEMENTS_PARENT_ID);

        StateElement stateElement = ZusammenPluginUtil
                .getStateElement(new ElementContext(ITEM_ID, VERSION_ID), elementEntity);

        Assert.assertNull(stateElement.getParentId());
    }

    private static SessionContext sessionContext() {
        return TestUtils.createSessionContext(new UserInfo(USER), TENANT);
    }

    private static Namespace namespace() {
        Namespace namespace = new Namespace();
        namespace.setValue("root-namespace/" + PARENT_ID.getValue());
        return namespace;
    }

    private static Set<Id> subElementIds() {
        return new HashSet<>(Arrays.asList(SUB_ELEMENT_ID_A, SUB_ELEMENT_ID_B));
    }

    private static Collection<Relation> relations(String type) {
        Relation relation = new Relation();
        relation.setType(type);
        return Arrays.asList(relation);
    }

    private static ItemVersionData itemVersionData(String name) {
        ItemVersionData data = new ItemVersionData();
        data.setInfo(TestUtils.createInfo(name));
        data.setRelations(relations(name + "-relation"));
        return data;
    }

    private static ElementEntity fullyPopulatedElementEntity() {
        ElementEntity elementEntity = new ElementEntity(ELEMENT_ID);
        elementEntity.setParentId(PARENT_ID);
        elementEntity.setNamespace(namespace());
        elementEntity.setElementHash(ELEMENT_HASH);
        elementEntity.setInfo(TestUtils.createInfo("element-info"));
        elementEntity.setRelations(relations("element-relation"));
        elementEntity.setData(ByteBuffer.wrap(DATA.getBytes(StandardCharsets.UTF_8)));
        elementEntity
                .setSearchableData(ByteBuffer.wrap(SEARCHABLE_DATA.getBytes(StandardCharsets.UTF_8)));
        elementEntity
                .setVisualization(ByteBuffer.wrap(VISUALIZATION.getBytes(StandardCharsets.UTF_8)));
        elementEntity.setSubElementIds(subElementIds());
        return elementEntity;
    }

    private static CollaborationElement fullyPopulatedCollaborationElement() {
        CollaborationElement element =
                new CollaborationElement(ITEM_ID, VERSION_ID, namespace(), ELEMENT_ID);
        element.setParentId(PARENT_ID);
        element.setInfo(TestUtils.createInfo("collaboration-element-info"));
        element.setRelations(relations("collaboration-element-relation"));
        element.setData(new ByteArrayInputStream(DATA.getBytes(StandardCharsets.UTF_8)));
        element.setSearchableData(
                new ByteArrayInputStream(SEARCHABLE_DATA.getBytes(StandardCharsets.UTF_8)));
        element.setVisualization(
                new ByteArrayInputStream(VISUALIZATION.getBytes(StandardCharsets.UTF_8)));
        return element;
    }

    private static ElementEntity hashSourceOf(CollaborationElement element) {
        ElementEntity hashSource = new ElementEntity(element.getId());
        hashSource.setInfo(element.getInfo());
        hashSource.setRelations(element.getRelations());
        hashSource.setData(ByteBuffer.wrap(FileUtils.toByteArray(element.getData())));
        hashSource
                .setSearchableData(ByteBuffer.wrap(FileUtils.toByteArray(element.getSearchableData())));
        hashSource
                .setVisualization(ByteBuffer.wrap(FileUtils.toByteArray(element.getVisualization())));
        return hashSource;
    }

    private static String asString(ByteBuffer buffer) {
        Assert.assertNotNull(buffer);
        return new String(buffer.array(), StandardCharsets.UTF_8);
    }

    private static String asString(InputStream input) {
        Assert.assertNotNull(input);
        return new String(FileUtils.toByteArray(input), StandardCharsets.UTF_8);
    }
}
