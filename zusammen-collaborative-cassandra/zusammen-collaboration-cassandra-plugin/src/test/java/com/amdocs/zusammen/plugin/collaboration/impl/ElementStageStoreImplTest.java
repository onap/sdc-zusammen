package com.amdocs.zusammen.plugin.collaboration.impl;

import com.amdocs.zusammen.datatypes.Id;
import com.amdocs.zusammen.datatypes.SessionContext;
import com.amdocs.zusammen.datatypes.UserInfo;
import com.amdocs.zusammen.datatypes.item.Action;
import com.amdocs.zusammen.datatypes.item.ElementContext;
import com.amdocs.zusammen.datatypes.item.Resolution;
import com.amdocs.zusammen.plugin.statestore.cassandra.dao.types.ElementEntityContext;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import com.amdocs.zusammen.plugin.collaboration.TestUtils;
import com.amdocs.zusammen.plugin.dao.ElementStageRepository;
import com.amdocs.zusammen.plugin.dao.types.ElementEntity;
import com.amdocs.zusammen.plugin.dao.types.StageEntity;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

public class ElementStageStoreImplTest {
  private static final UserInfo USER = new UserInfo("user");
  private static final SessionContext context = TestUtils.createSessionContext(USER, "test");
  private static final ElementContext elementContext =
      TestUtils.createElementContext(new Id(), new Id());

  private AutoCloseable mocks;

  @Mock
  private ElementStageRepository elementStageRepositoryMock;
  @Spy
  private ElementStageStoreImpl elementStageStore;

  @BeforeMethod
  public void setUp() throws Exception {
    mocks = MockitoAnnotations.openMocks(this);
    // doReturn, not when(spy...): the latter runs the real getter, which resolves the repository
    // through the factory registry and can pull a Cassandra-configured class into the JVM.
    doReturn(elementStageRepositoryMock).when(elementStageStore).getElementStageRepository(any());
  }

  @AfterMethod
  public void tearDown() throws Exception {
    mocks.close();
  }

  @Test
  public void testListIds() throws Exception {
    Collection<ElementEntity> stagedIds =
        Arrays.asList(new ElementEntity(new Id()), new ElementEntity(new Id()));
    doReturn(stagedIds).when(elementStageRepositoryMock).listIds(any(), any());

    Assert.assertEquals(elementStageStore.listIds(context, elementContext), stagedIds);

    ArgumentCaptor<ElementEntityContext> contextCaptor =
        ArgumentCaptor.forClass(ElementEntityContext.class);
    verify(elementStageRepositoryMock).listIds(same(context), contextCaptor.capture());
    assertPrivateStageContext(contextCaptor.getValue());
  }

  @Test
  public void testGet() throws Exception {
    ElementEntity element = new ElementEntity(new Id());
    StageEntity<ElementEntity> stage = new StageEntity<>(element, new Date(1000L));
    doReturn(Optional.of(stage)).when(elementStageRepositoryMock).get(any(), any(), any());

    Optional<StageEntity<ElementEntity>> retrieved =
        elementStageStore.get(context, elementContext, element);

    Assert.assertTrue(retrieved.isPresent());
    Assert.assertSame(retrieved.get(), stage);

    ArgumentCaptor<ElementEntityContext> contextCaptor =
        ArgumentCaptor.forClass(ElementEntityContext.class);
    verify(elementStageRepositoryMock).get(same(context), contextCaptor.capture(), same(element));
    assertPrivateStageContext(contextCaptor.getValue());
  }

  @Test
  public void testGetWhenNotStaged() throws Exception {
    doReturn(Optional.empty()).when(elementStageRepositoryMock).get(any(), any(), any());

    Assert.assertFalse(elementStageStore
        .get(context, elementContext, new ElementEntity(new Id())).isPresent());
  }

  @Test
  public void testGetConflicted() throws Exception {
    ElementEntity element = new ElementEntity(new Id());
    StageEntity<ElementEntity> stage =
        new StageEntity<>(element, new Date(1000L), Action.UPDATE, true);
    doReturn(Optional.of(stage)).when(elementStageRepositoryMock).get(any(), any(), any());

    Optional<StageEntity<ElementEntity>> retrieved =
        elementStageStore.getConflicted(context, elementContext, element);

    Assert.assertTrue(retrieved.isPresent());
    Assert.assertSame(retrieved.get(), stage);
  }

  @Test
  public void testGetConflictedWhenStagedWithoutConflict() throws Exception {
    ElementEntity element = new ElementEntity(new Id());
    doReturn(Optional.of(new StageEntity<>(element, new Date(1000L), Action.UPDATE, false)))
        .when(elementStageRepositoryMock).get(any(), any(), any());

    Assert.assertFalse(
        elementStageStore.getConflicted(context, elementContext, element).isPresent());
  }

  @Test
  public void testGetConflictedWhenNotStaged() throws Exception {
    doReturn(Optional.empty()).when(elementStageRepositoryMock).get(any(), any(), any());

    Assert.assertFalse(elementStageStore
        .getConflicted(context, elementContext, new ElementEntity(new Id())).isPresent());
  }

  @Test
  public void testHasConflicts() throws Exception {
    doReturn(Collections.singletonList(new ElementEntity(new Id())))
        .when(elementStageRepositoryMock).listConflictedIds(any(), any());

    Assert.assertTrue(elementStageStore.hasConflicts(context, elementContext));

    ArgumentCaptor<ElementEntityContext> contextCaptor =
        ArgumentCaptor.forClass(ElementEntityContext.class);
    verify(elementStageRepositoryMock).listConflictedIds(same(context), contextCaptor.capture());
    assertPrivateStageContext(contextCaptor.getValue());
  }

  @Test
  public void testHasConflictsWhenNone() throws Exception {
    doReturn(Collections.emptyList())
        .when(elementStageRepositoryMock).listConflictedIds(any(), any());

    Assert.assertFalse(elementStageStore.hasConflicts(context, elementContext));
  }

  @Test
  public void testListConflictedDescriptors() throws Exception {
    ElementEntity firstId = new ElementEntity(new Id("first"));
    ElementEntity secondId = new ElementEntity(new Id("second"));
    StageEntity<ElementEntity> firstDescriptor = new StageEntity<>(firstId, new Date(1000L));
    StageEntity<ElementEntity> secondDescriptor = new StageEntity<>(secondId, new Date(2000L));
    doReturn(Arrays.asList(firstId, secondId))
        .when(elementStageRepositoryMock).listConflictedIds(any(), any());
    doReturn(Optional.of(firstDescriptor))
        .when(elementStageRepositoryMock).getDescriptor(any(), any(), same(firstId));
    doReturn(Optional.of(secondDescriptor))
        .when(elementStageRepositoryMock).getDescriptor(any(), any(), same(secondId));

    Collection<StageEntity<ElementEntity>> descriptors =
        elementStageStore.listConflictedDescriptors(context, elementContext);

    Assert.assertEquals(new ArrayList<>(descriptors),
        Arrays.asList(firstDescriptor, secondDescriptor));
  }

  @Test(expectedExceptions = IllegalStateException.class)
  public void testListConflictedDescriptorsWhenDescriptorMissing() throws Exception {
    doReturn(Collections.singletonList(new ElementEntity(new Id())))
        .when(elementStageRepositoryMock).listConflictedIds(any(), any());
    doReturn(Optional.empty()).when(elementStageRepositoryMock).getDescriptor(any(), any(), any());

    elementStageStore.listConflictedDescriptors(context, elementContext);
  }

  @Test
  public void testDeleteAll() throws Exception {
    ElementEntity firstId = new ElementEntity(new Id("first"));
    ElementEntity secondId = new ElementEntity(new Id("second"));
    ElementEntity firstStaged = new ElementEntity(new Id("first"));
    ElementEntity secondStaged = new ElementEntity(new Id("second"));
    doReturn(Arrays.asList(firstId, secondId))
        .when(elementStageRepositoryMock).listIds(any(), any());
    doReturn(Optional.of(new StageEntity<>(firstStaged, new Date(1000L))))
        .when(elementStageRepositoryMock).get(any(), any(), same(firstId));
    doReturn(Optional.of(new StageEntity<>(secondStaged, new Date(2000L))))
        .when(elementStageRepositoryMock).get(any(), any(), same(secondId));

    elementStageStore.deleteAll(context, elementContext);

    verify(elementStageRepositoryMock).delete(same(context), any(), same(firstStaged));
    verify(elementStageRepositoryMock).delete(same(context), any(), same(secondStaged));
  }

  @Test(expectedExceptions = IllegalStateException.class)
  public void testDeleteAllWhenStagedElementMissing() throws Exception {
    doReturn(Collections.singletonList(new ElementEntity(new Id())))
        .when(elementStageRepositoryMock).listIds(any(), any());
    doReturn(Optional.empty()).when(elementStageRepositoryMock).get(any(), any(), any());

    elementStageStore.deleteAll(context, elementContext);
  }

  @Test
  public void testCreate() throws Exception {
    StageEntity<ElementEntity> stage =
        new StageEntity<>(new ElementEntity(new Id()), new Date(1000L), Action.CREATE, false);

    elementStageStore.create(context, elementContext, stage);

    ArgumentCaptor<ElementEntityContext> contextCaptor =
        ArgumentCaptor.forClass(ElementEntityContext.class);
    verify(elementStageRepositoryMock).create(same(context), contextCaptor.capture(), same(stage));
    assertPrivateStageContext(contextCaptor.getValue());
  }

  @Test
  public void testDelete() throws Exception {
    ElementEntity element = new ElementEntity(new Id());

    elementStageStore.delete(context, elementContext, element);

    ArgumentCaptor<ElementEntityContext> contextCaptor =
        ArgumentCaptor.forClass(ElementEntityContext.class);
    verify(elementStageRepositoryMock)
        .delete(same(context), contextCaptor.capture(), same(element));
    assertPrivateStageContext(contextCaptor.getValue());
  }


  @Test
  public void testResolveConflictWhenNotStaged() throws Exception {
    doReturn(Optional.empty())
        .when(elementStageRepositoryMock).get(any(), any(), any());
    elementStageStore
        .resolveConflict(context, elementContext, new ElementEntity(new Id()), Resolution.YOURS);

    verify(elementStageRepositoryMock, never()).markAsNotConflicted(any(), any(), any(), any());
    verify(elementStageRepositoryMock, never()).markAsNotConflicted(any(), any(), any());
  }

  @Test
  public void testResolveConflictWhenNotConflicted() throws Exception {
    Id elementId = new Id();
    StageEntity<ElementEntity> stagedElement =
        new StageEntity<>(new ElementEntity(elementId), new Date());
    doReturn(Optional.of(stagedElement))
        .when(elementStageRepositoryMock).get(any(), any(), any());
    elementStageStore
        .resolveConflict(context, elementContext, new ElementEntity(elementId), Resolution.YOURS);

    verify(elementStageRepositoryMock, never()).markAsNotConflicted(any(), any(), any(), any());
    verify(elementStageRepositoryMock, never()).markAsNotConflicted(any(), any(), any());
  }

  @Test
  public void testResolveConflictByYours() throws Exception {
    Id elementId = new Id();
    StageEntity<ElementEntity> stagedElement =
        new StageEntity<>(new ElementEntity(elementId), new Date());
    stagedElement.setAction(Action.UPDATE);
    stagedElement.setConflicted(true);

    doReturn(Optional.of(stagedElement))
        .when(elementStageRepositoryMock).get(any(), any(), any());

    elementStageStore
        .resolveConflict(context, elementContext, new ElementEntity(elementId), Resolution.YOURS);

    verify(elementStageRepositoryMock).markAsNotConflicted(same(context),
        eq(new ElementEntityContext(USER.getUserName(), elementContext)),
        same(stagedElement.getEntity()), same(Action.IGNORE));
  }

  @Test
  public void testResolveConflictByYoursWithRelated() throws Exception {
    Id elementId = new Id();
    StageEntity<ElementEntity> stagedElement =
        new StageEntity<>(new ElementEntity(elementId), new Date());
    stagedElement.setAction(Action.UPDATE);
    stagedElement.setConflicted(true);
    ElementEntity relatedElement1 = new ElementEntity(new Id());
    ElementEntity relatedElement2 = new ElementEntity(new Id());
    ElementEntity relatedElement3 = new ElementEntity(new Id());
    Set<ElementEntity> relatedElements = new HashSet<>();
    relatedElements.add(relatedElement1);
    relatedElements.add(relatedElement2);
    relatedElements.add(relatedElement3);
    stagedElement.setConflictDependents(relatedElements);

    doReturn(Optional.of(stagedElement))
        .when(elementStageRepositoryMock).get(any(), any(), any());

    elementStageStore
        .resolveConflict(context, elementContext, new ElementEntity(elementId), Resolution.YOURS);

    ElementEntityContext elementEntityContext =
        new ElementEntityContext(USER.getUserName(), elementContext);
    verify(elementStageRepositoryMock).markAsNotConflicted(same(context), eq(elementEntityContext),
        same(stagedElement.getEntity()), same(Action.IGNORE));
    verify(elementStageRepositoryMock).markAsNotConflicted(same(context), eq(elementEntityContext),
        same(relatedElement1), same(Action.IGNORE));
    verify(elementStageRepositoryMock).markAsNotConflicted(same(context), eq(elementEntityContext),
        same(relatedElement2), same(Action.IGNORE));
    verify(elementStageRepositoryMock).markAsNotConflicted(same(context), eq(elementEntityContext),
        same(relatedElement3), same(Action.IGNORE));
  }

  @Test
  public void testResolveConflictByTheirs() throws Exception {
    Id elementId = new Id();
    StageEntity<ElementEntity> stagedElement =
        new StageEntity<>(new ElementEntity(elementId), new Date(), Action.UPDATE, true);

    doReturn(Optional.of(stagedElement))
        .when(elementStageRepositoryMock).get(any(), any(), any());

    elementStageStore
        .resolveConflict(context, elementContext, new ElementEntity(elementId), Resolution.THEIRS);

    ArgumentCaptor<ElementEntityContext> contextCaptor =
        ArgumentCaptor.forClass(ElementEntityContext.class);
    verify(elementStageRepositoryMock).markAsNotConflicted(same(context), contextCaptor.capture(),
        same(stagedElement.getEntity()));
    assertPrivateStageContext(contextCaptor.getValue());
    verify(elementStageRepositoryMock, never()).markAsNotConflicted(any(), any(), any(), any());
  }

  @Test
  public void testResolveConflictByTheirsWithRelated() throws Exception {
    Id elementId = new Id();
    StageEntity<ElementEntity> stagedElement =
        new StageEntity<>(new ElementEntity(elementId), new Date(), Action.UPDATE, true);
    ElementEntity relatedElement = new ElementEntity(new Id());
    stagedElement.setConflictDependents(Collections.singleton(relatedElement));

    doReturn(Optional.of(stagedElement))
        .when(elementStageRepositoryMock).get(any(), any(), any());

    elementStageStore
        .resolveConflict(context, elementContext, new ElementEntity(elementId), Resolution.THEIRS);

    ArgumentCaptor<ElementEntityContext> contextCaptor =
        ArgumentCaptor.forClass(ElementEntityContext.class);
    verify(elementStageRepositoryMock).markAsNotConflicted(same(context), contextCaptor.capture(),
        same(stagedElement.getEntity()));
    assertPrivateStageContext(contextCaptor.getValue());
  }

  @Test(expectedExceptions = UnsupportedOperationException.class)
  public void testResolveConflictByOtherIsNotSupported() throws Exception {
    Id elementId = new Id();
    doReturn(Optional.of(new StageEntity<>(new ElementEntity(elementId), new Date(), Action.UPDATE,
        true))).when(elementStageRepositoryMock).get(any(), any(), any());

    elementStageStore
        .resolveConflict(context, elementContext, new ElementEntity(elementId), Resolution.OTHER);
  }

  private void assertPrivateStageContext(ElementEntityContext captured) {
    Assert.assertEquals(captured.getSpace(), USER.getUserName());
    Assert.assertEquals(captured.getItemId(), elementContext.getItemId());
    Assert.assertEquals(captured.getVersionId(), elementContext.getVersionId());
    Assert.assertEquals(captured.getRevisionId(), Id.ZERO);
  }
}
