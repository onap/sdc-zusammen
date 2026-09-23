package com.amdocs.zusammen.plugin.dao;

import com.amdocs.zusammen.datatypes.Id;
import com.amdocs.zusammen.datatypes.SessionContext;
import com.amdocs.zusammen.plugin.statestore.cassandra.dao.types.ElementEntityContext;
import com.amdocs.zusammen.plugin.dao.types.ElementEntity;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;

public interface ElementRepository {

  Map<Id,Id> listIds(SessionContext context, ElementEntityContext elementContext);

  void create(SessionContext context, ElementEntityContext elementContext, ElementEntity element);

  void update(SessionContext context, ElementEntityContext elementContext, ElementEntity element);

  void delete(SessionContext context, ElementEntityContext elementContext, ElementEntity element);

  /**
   * Deletes the entire revisions of an element.
   * As apposed to delete (which deletes specific element revision) this API does not deletes the
   * element from the version elements list.
   * @param context
   * @param elementContext
   * @param element
   */
  void cleanAllRevisions(SessionContext context, ElementEntityContext elementContext, ElementEntity element);

  Optional<ElementEntity> get(SessionContext context, ElementEntityContext elementContext,
                              ElementEntity element);

  /**
   * Reads several elements of one space and version at once. Ids without a row are left out of the
   * result rather than failing it; the map iterates in the order the ids were given.
   */
  Map<Id, ElementEntity> getAll(SessionContext context, ElementEntityContext elementContext,
                                Collection<Id> elementIds);

  Optional<ElementEntity> getDescriptor(SessionContext context, ElementEntityContext elementContext,
                                        ElementEntity element);

  void createNamespace(SessionContext context, ElementEntityContext elementContext,
                       ElementEntity element);

  Optional<Id> getHash(SessionContext context, ElementEntityContext elementEntityContext,
                       ElementEntity element);
}
