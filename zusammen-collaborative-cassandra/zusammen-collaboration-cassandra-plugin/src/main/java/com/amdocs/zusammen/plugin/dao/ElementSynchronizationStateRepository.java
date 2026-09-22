package com.amdocs.zusammen.plugin.dao;

import com.amdocs.zusammen.datatypes.SessionContext;
import com.amdocs.zusammen.plugin.dao.types.SynchronizationStateEntity;
import com.amdocs.zusammen.plugin.statestore.cassandra.dao.types.ElementEntityContext;

import java.util.Collection;
import java.util.List;

public interface ElementSynchronizationStateRepository
    extends SynchronizationStateRepository<ElementEntityContext> {

  /**
   * Lists the synchronization state of every element of the version, keeping <b>one entry per
   * element</b>: {@link SynchronizationStateEntity#equals} compares the id alone, so the revisions
   * of an element collapse into whichever of them is read first. Use
   * {@link #listPerRevision(SessionContext, ElementEntityContext)} where the revision matters - in
   * the public space an element has a row per revision it was published in.
   */
  Collection<SynchronizationStateEntity> list(SessionContext context,
                                              ElementEntityContext elementContext);

  /**
   * Lists the synchronization state of every element revision of the version, one entry per stored
   * (element, revision) pair. Same single query as
   * {@link #list(SessionContext, ElementEntityContext)}, without its de-duplication.
   */
  List<SynchronizationStateEntity> listPerRevision(SessionContext context,
                                                   ElementEntityContext elementContext);

  void deleteAll(SessionContext context, ElementEntityContext elementContext);

  void update(SessionContext context, ElementEntityContext entityContext,
              SynchronizationStateEntity syncStateEntity);

  void markAsDirty(SessionContext context, ElementEntityContext entityContext,
                   SynchronizationStateEntity syncStateEntity);

}
