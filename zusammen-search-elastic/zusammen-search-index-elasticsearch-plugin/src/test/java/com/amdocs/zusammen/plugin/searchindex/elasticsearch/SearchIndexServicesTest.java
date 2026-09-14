/*
 * Copyright © 2016-2017 European Support Limited
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

package com.amdocs.zusammen.plugin.searchindex.elasticsearch;

import com.amdocs.zusammen.plugin.searchindex.elasticsearch.dao.ElasticSearchDao;
import com.amdocs.zusammen.plugin.searchindex.elasticsearch.datatypes.EsSearchCriteria;
import com.amdocs.zusammen.plugin.searchindex.elasticsearch.datatypes.EsSearchableData;
import com.amdocs.zusammen.datatypes.Id;
import com.amdocs.zusammen.datatypes.Namespace;
import com.amdocs.zusammen.datatypes.SessionContext;
import com.amdocs.zusammen.datatypes.Space;
import com.amdocs.zusammen.datatypes.item.Info;
import com.amdocs.zusammen.datatypes.item.Relation;
import com.amdocs.zusammen.sdk.searchindex.types.SearchIndexElement;
import com.amdocs.zusammen.utils.fileutils.json.JsonUtil;
import org.elasticsearch.action.search.SearchResponse;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;


public class SearchIndexServicesTest {

  @Mock
  private ElasticSearchDao elasticSearchDaoMock;
  @InjectMocks
  private SearchIndexServices searchIndexServices;

  @BeforeMethod(alwaysRun = true)
  public void injectDoubles() {
    MockitoAnnotations.initMocks(this);
    Mockito.when(elasticSearchDaoMock.search(anyObject(), anyString(), anyObject()))
        .thenReturn(new SearchResponse());

  }

  @Test
  public void testSearch() throws Exception {
    SessionContext sessionContext = EsTestUtils.createSessionContext("tenant", "user");
    Optional<String> json = EsTestUtils.getJson("myname", null, null);
    Assert.assertEquals(json.isPresent(), true);
    if (json.isPresent()) {
      String jsonQuery = EsTestUtils.wrapperTermQuery(json.get().toLowerCase());

      List<String> types = new ArrayList<>();
      types.add("type1");
      EsSearchCriteria searchCriteria = EsTestUtils.createSearchCriteria(types, 0, 1, jsonQuery);
      searchIndexServices.search(sessionContext, searchCriteria);
    }
  }

  @Test
  public void testGetElasticSearchDao() throws Exception {
    SessionContext sessionContext = EsTestUtils.createSessionContext("tenant", "myUser");
    ElasticSearchDao elasticSearchDao =
        new SearchIndexServices().getElasticSearchDao(sessionContext);
    Assert.assertNotNull(elasticSearchDao);
  }

  @Test
  public void testGetEsSearchableData() throws Exception {
    EsSearchableData searchableData = EsTestUtils.createSearchableData("myType", "myname", null,
        null);
    SearchIndexElement element =
        EsTestUtils.createSearchIndexElement(searchableData,
            Space.PUBLIC, new Id(), new Id(), Namespace.ROOT_NAMESPACE, new Id());
    EsSearchableData data = new SearchIndexServices().getEsSearchableData(element);
    Assert.assertNotNull(data);
    Assert.assertNotNull(data.getType());
    Assert.assertNotNull(data.getData());
  }

  @Test
  public void testGetEsSearchableDataWithNoData() throws Exception {
    SearchIndexElement element =
        EsTestUtils.createSearchIndexElement(null,
            Space.PUBLIC, new Id(), new Id(), Namespace.ROOT_NAMESPACE, new Id());
    EsSearchableData data = new SearchIndexServices().getEsSearchableData(element);
    Assert.assertNotNull(data);
    Assert.assertNotNull(data.getType());
    Assert.assertNull(data.getData());
  }

  @Test
  public void testGetEsSource() throws Exception {
    EsSearchableData searchableData = EsTestUtils.createSearchableData("nyType", "nmyname",
        "msg", null);
    String source = new SearchIndexServices().getEsSource(searchableData);
    Assert.assertNotNull(source);
  }

  @Test
  public void testCreateEnrichedElasticSearchableData() throws Exception {
    String type = "type";
    String name = "createName";
    String message = "message";
    EsSearchableData searchableData = EsTestUtils.createSearchableData(type, name, message, null);

    Map map = testCreateEnrichElasticSearchableData(searchableData);
    Assert.assertEquals(map.size(), 10);
    Assert.assertEquals(map.get("name"), name);
    Assert.assertEquals(map.get("message"), message);
  }

  @Test
  public void testCreateOnlyEnrichedElasticSearchableData() throws Exception {
    Map map = testCreateEnrichElasticSearchableData(null);
    Assert.assertEquals(map.size(), 8);
  }

  private Map testCreateEnrichElasticSearchableData(EsSearchableData searchableData) {
    String user = "myUser";
    SessionContext sessionContext = EsTestUtils.createSessionContext("tenant", user);
    Id itemId = new Id();
    Id versionId = new Id();
    Id elementId = new Id();
    Id parentId = new Id();
    Namespace namespace = new Namespace(Namespace.ROOT_NAMESPACE, parentId);
    Info info = new Info();
    info.setName("elementName");
    info.setDescription("elementDesc");
    List<Relation> relations = Arrays.asList(new Relation(), new Relation());

    SearchIndexElement element = EsTestUtils.createSearchIndexElement(
        searchableData, Space.PRIVATE, itemId, versionId, namespace, elementId);
    element.setParentId(parentId);
    element.setInfo(info);
    element.setRelations(relations);

    EsSearchableData enrichedElasticSearchableData = new SearchIndexServices()
        .createEnrichedElasticSearchableData(sessionContext, element);
    Map map = JsonUtil.json2Object(enrichedElasticSearchableData.getData(), Map.class);

    Assert.assertEquals(map.get("spaceName"), user.toLowerCase());
    Assert.assertEquals(((Map) map.get("itemId")).get("value").toString(), itemId.toString());
    Assert.assertEquals(((Map) map.get("versionId")).get("value").toString(), versionId.toString());
    Assert.assertEquals(((Map) map.get("elementId")).get("value").toString(), elementId.toString());
    Assert.assertEquals(map.get("namespace"), namespace.getValue());
    Assert.assertEquals(((Map) map.get("parentId")).get("value").toString(), parentId.toString());
    Assert.assertEquals(JsonUtil.object2Json(map.get("info")), JsonUtil.object2Json(info));
    Assert.assertEquals(
        JsonUtil.object2Json(map.get("relations")), JsonUtil.object2Json(relations));
    return map;
  }

  @Test
  public void testCreateSearchableDataId() throws Exception {
    SessionContext sessionContext = EsTestUtils.createSessionContext("tenant", "myUser");
    EsSearchableData searchableData = EsTestUtils.createSearchableData("myType", "nmyname",
        "msg", null);
    Id itemId = new Id();
    Id versionId = new Id();
    Id elementId = new Id();
    SearchIndexElement element = EsTestUtils
        .createSearchIndexElement(searchableData, Space.PUBLIC, itemId, versionId,
            Namespace.ROOT_NAMESPACE, elementId
        );
    String id = new SearchIndexServices().createSearchableDataId(sessionContext, element);
    Assert.assertNotNull(id);
    Assert.assertEquals(id,
        Space.PUBLIC.toString().toLowerCase() + "_" + itemId.toString() + "_" +
            versionId.toString() + "_" + elementId.toString());
  }

  @Test
  public void testGetElasticSpaceForCreAndUpdPrivate() throws Exception {
    SessionContext sessionContext = EsTestUtils.createSessionContext("tenant", "My user");
    String elasticSpaceForCreAndUpd =
        new SearchIndexServices().getElasticSpaceForCreAndUpd(sessionContext, Space.PRIVATE);
    Assert.assertEquals(elasticSpaceForCreAndUpd, "myuser");
  }

  @Test
  public void testGetElasticSpaceForCreAndUpdPublic() throws Exception {
    SessionContext sessionContext = EsTestUtils.createSessionContext("tenant", "My user");
    String elasticSpaceForCreAndUpd =
        new SearchIndexServices().getElasticSpaceForCreAndUpd(sessionContext, Space.PUBLIC);
    Assert.assertEquals(elasticSpaceForCreAndUpd, "public");
  }

  @Test(expectedExceptions = {RuntimeException.class},
      expectedExceptionsMessageRegExp = "BOTH is invalid Space value for Elastic create and " +
          "update actions")
  public void testGetElasticSpaceForCreAndUpdBoth() throws Exception {
    SessionContext sessionContext = EsTestUtils.createSessionContext("tenant", "My user");
    new SearchIndexServices().getElasticSpaceForCreAndUpd(sessionContext, Space.BOTH);
  }

  @Test
  public void testGetIndex() {
    SessionContext sessionContext = EsTestUtils.createSessionContext(" one \" OO * * ee * \"\" "
            + "** sDs \\\\ ww \\ w < P<A tt << EE | s|sd ,w ,q >> ksjk> d / // ww /p ?? q? s    ",
        "myUser");

    String esIndex = new SearchIndexServices().getEsIndex(sessionContext);
    Assert.assertEquals(esIndex, "oneooeesdswwwpatteessdwqksjkdwwpqs");
  }

  @Test(expectedExceptions = {RuntimeException.class},
      expectedExceptionsMessageRegExp = "Mandatory Data is missing - Tenant value in session context")
  public void testGetIndexWithNoTenant() {
    SessionContext sessionContext = EsTestUtils.createSessionContext(null, "myUser");
    new SearchIndexServices().getEsIndex(sessionContext);
  }

  @Test
  public void testValidationNoError() throws Exception {
    EsSearchableData searchableData = EsTestUtils.createSearchableData("myType", "nmyname",
        "msg", null);
    SearchIndexElement element = EsTestUtils
        .createSearchIndexElement(searchableData, Space.PUBLIC, new Id(), new Id(),
            Namespace.ROOT_NAMESPACE, new Id()
        );
    new SearchIndexServices().validation(element);
  }

  @Test(expectedExceptions = {RuntimeException.class},
      expectedExceptionsMessageRegExp = "Mandatory Data is missing - Element searchable data")
  public void testValidationMissingSearchIndexElement() throws Exception {
    new SearchIndexServices().validation(null);
  }

  @Test
  public void testValidationMissingSearchableData() throws Exception {
    SearchIndexElement element = EsTestUtils
        .createSearchIndexElement(null, Space.PUBLIC, new Id(), new Id(), Namespace.ROOT_NAMESPACE,
            new Id());
    new SearchIndexServices().validation(element);
  }

  @Test(expectedExceptions = {RuntimeException.class},
      expectedExceptionsMessageRegExp = "Invalid searchableData, EsSearchableData object is expected.*")
  public void testValidationInvalidSearchableData() throws Exception {
    SearchIndexElement element =
        new SearchIndexElement(new Id(), new Id(), Namespace.ROOT_NAMESPACE, new Id());
    element.setSearchableData(new ByteArrayInputStream("kfkf".getBytes()));
    new SearchIndexServices().validation(element);
  }

  @Test
  public void testValidationSearchableDataNoRequired() throws Exception {
    SearchIndexElement element = EsTestUtils
        .createSearchIndexElement(null, Space.PUBLIC, new Id(), new Id(), Namespace.ROOT_NAMESPACE,
            new Id());
    new SearchIndexServices().validation(element);
  }

  @Test(expectedExceptions = {RuntimeException.class},
      expectedExceptionsMessageRegExp = "Mandatory Data is missing - Space.*")
  public void testValidationMissingSpace() throws Exception {
    SearchIndexElement element = EsTestUtils
        .createSearchIndexElement(null, null, new Id(), new Id(), Namespace.ROOT_NAMESPACE,
            new Id());
    new SearchIndexServices().validation(element);
  }

  @Test(expectedExceptions = {RuntimeException.class},
      expectedExceptionsMessageRegExp = ".*Mandatory Data is missing - Item Id.*")
  public void testValidationMissingItemId() throws Exception {
    SearchIndexElement element = EsTestUtils
        .createSearchIndexElement(null, null, null, new Id(), Namespace.ROOT_NAMESPACE, new Id());
    new SearchIndexServices().validation(element);
  }

  @Test(expectedExceptions = {RuntimeException.class},
      expectedExceptionsMessageRegExp = "Mandatory Data is missing - Version Id.*")
  public void testValidationMissingVersionId() throws Exception {
    SearchIndexElement element = EsTestUtils
        .createSearchIndexElement(null, Space.PRIVATE, new Id(), null, Namespace.ROOT_NAMESPACE,
            new Id());
    new SearchIndexServices().validation(element);
  }

  @Test(expectedExceptions = {RuntimeException.class},
      expectedExceptionsMessageRegExp = "Mandatory Data is missing - Element Id.*")
  public void testValidationMissingElementId() throws Exception {
    SearchIndexElement element = EsTestUtils
        .createSearchIndexElement(null, Space.PRIVATE, new Id(), new Id(), Namespace.ROOT_NAMESPACE,
            null);
    new SearchIndexServices().validation(element);
  }

  @Test
  public void testCheckSearchCriteriaInstance() throws Exception {
    new SearchIndexServices().checkSearchCriteriaInstance(new EsSearchCriteria());
  }

  @Test(expectedExceptions = {RuntimeException.class},
      expectedExceptionsMessageRegExp = "Invalid SearchCriteria, EsSearchCriteria object is expected")
  public void testCheckSearchCriteriaInstanceInvalid() throws Exception {
    new SearchIndexServices().checkSearchCriteriaInstance(null);
  }

}