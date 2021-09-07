// Copyright 2020 Goldman Sachs
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package org.finos.legend.engine.application.query.api;

import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.eclipse.collections.api.block.function.Function0;
import org.eclipse.collections.api.factory.Lists;
import org.finos.legend.engine.application.query.model.Query;
import org.finos.legend.engine.application.query.model.QueryEvent;
import org.finos.legend.engine.application.query.model.QueryProjectCoordinates;
import org.finos.legend.engine.application.query.utils.TestMongoClientProvider;
import org.finos.legend.engine.shared.core.vault.TestVaultImplementation;
import org.finos.legend.engine.shared.core.vault.Vault;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.time.Instant;
import java.util.List;

public class TestQueryStoreManager
{
    private static final ObjectMapper objectMapper = new ObjectMapper()
        .configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true)
        .configure(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, true);
    private TestMongoClientProvider testMongoClientProvider = new TestMongoClientProvider();
    private final QueryStoreManager queryStoreManager = new QueryStoreManager(testMongoClientProvider.mongoClient);
    private static final TestVaultImplementation testVaultImplementation = new TestVaultImplementation();

    @BeforeClass
    public static void setupClass()
    {
        testVaultImplementation.setValue("query.mongo.database", "test");
        testVaultImplementation.setValue("query.mongo.collection.query", "query");
        testVaultImplementation.setValue("query.mongo.collection.queryEvent", "query-event");
        Vault.INSTANCE.registerImplementation(testVaultImplementation);
    }

    @AfterClass
    public static void cleanUpClass()
    {
        Vault.INSTANCE.unregisterImplementation(testVaultImplementation);
    }

    @Before
    public void setup()
    {
        this.testMongoClientProvider = new TestMongoClientProvider();
    }

    @After
    public void cleanUp()
    {
        this.testMongoClientProvider.cleanUp();
    }

    private static Query createTestQuery(String id, String name, String owner)
    {
        Query query = new Query();
        query.id = id;
        query.name = name;
        query.owner = owner;
        query.groupId = "test.group";
        query.artifactId = "test-artifact";
        query.versionId = "0.0.0";
        query.mapping = "mapping";
        query.runtime = "runtime";
        query.content = "content";
        query.description = "description";
        return query;
    }

    private static Query createTestQueryWithProjectCoordinate(String id, String name, String owner, String groupId, String artifactId)
    {
        Query query = createTestQuery(id, name, owner);
        query.groupId = groupId;
        query.artifactId = artifactId;
        return query;
    }

    private static QueryProjectCoordinates createTestQueryProjectCoordinate(String groupId, String artifactId)
    {
        QueryProjectCoordinates coordinate = new QueryProjectCoordinates();
        coordinate.groupId = groupId;
        coordinate.artifactId = artifactId;
        return coordinate;
    }

    @Test
    public void testValidateQuery()
    {
        Function0<Query> _createTestQuery = () -> createTestQuery("1", "query1", "testUser");
        Query goodQuery = _createTestQuery.get();
        QueryStoreManager.validateQuery(goodQuery);

        // ID
        Query queryWithInvalidId = _createTestQuery.get();
        queryWithInvalidId.id = null;
        Assert.assertThrows(ApplicationQueryException.class, () -> QueryStoreManager.validateQuery(queryWithInvalidId));
        queryWithInvalidId.id = "";
        Assert.assertThrows(ApplicationQueryException.class, () -> QueryStoreManager.validateQuery(queryWithInvalidId));

        // Name
        Query queryWithInvalidName = _createTestQuery.get();
        queryWithInvalidName.name = null;
        Assert.assertThrows(ApplicationQueryException.class, () -> QueryStoreManager.validateQuery(queryWithInvalidName));
        queryWithInvalidId.name = "";
        Assert.assertThrows(ApplicationQueryException.class, () -> QueryStoreManager.validateQuery(queryWithInvalidName));

        // Group ID
        Query queryWithInvalidGroupId = _createTestQuery.get();
        queryWithInvalidGroupId.groupId = null;
        Assert.assertThrows(ApplicationQueryException.class, () -> QueryStoreManager.validateQuery(queryWithInvalidGroupId));
        queryWithInvalidGroupId.groupId = "";
        Assert.assertThrows(ApplicationQueryException.class, () -> QueryStoreManager.validateQuery(queryWithInvalidGroupId));
        queryWithInvalidGroupId.groupId = "group-test";
        Assert.assertThrows(ApplicationQueryException.class, () -> QueryStoreManager.validateQuery(queryWithInvalidGroupId));
        queryWithInvalidGroupId.groupId = "12314";
        Assert.assertThrows(ApplicationQueryException.class, () -> QueryStoreManager.validateQuery(queryWithInvalidGroupId));

        // Artifact ID
        Query queryWithInvalidArtifactId = _createTestQuery.get();
        queryWithInvalidArtifactId.artifactId = null;
        Assert.assertThrows(ApplicationQueryException.class, () -> QueryStoreManager.validateQuery(queryWithInvalidArtifactId));
        queryWithInvalidArtifactId.artifactId = "";
        Assert.assertThrows(ApplicationQueryException.class, () -> QueryStoreManager.validateQuery(queryWithInvalidArtifactId));
        queryWithInvalidArtifactId.artifactId = "Group";
        Assert.assertThrows(ApplicationQueryException.class, () -> QueryStoreManager.validateQuery(queryWithInvalidArtifactId));
        queryWithInvalidArtifactId.artifactId = "someArtifact";
        Assert.assertThrows(ApplicationQueryException.class, () -> QueryStoreManager.validateQuery(queryWithInvalidArtifactId));

        // Version
        Query queryWithInvalidVersionId = _createTestQuery.get();
        queryWithInvalidVersionId.versionId = null;
        Assert.assertThrows(ApplicationQueryException.class, () -> QueryStoreManager.validateQuery(queryWithInvalidVersionId));
        queryWithInvalidVersionId.versionId = "";
        Assert.assertThrows(ApplicationQueryException.class, () -> QueryStoreManager.validateQuery(queryWithInvalidVersionId));

        // Mapping
        Query queryWithInvalidMapping = _createTestQuery.get();
        queryWithInvalidMapping.mapping = null;
        Assert.assertThrows(ApplicationQueryException.class, () -> QueryStoreManager.validateQuery(queryWithInvalidMapping));
        queryWithInvalidMapping.mapping = "";
        Assert.assertThrows(ApplicationQueryException.class, () -> QueryStoreManager.validateQuery(queryWithInvalidMapping));

        // Runtime
        Query queryWithInvalidRuntime = _createTestQuery.get();
        queryWithInvalidRuntime.runtime = null;
        Assert.assertThrows(ApplicationQueryException.class, () -> QueryStoreManager.validateQuery(queryWithInvalidRuntime));
        queryWithInvalidRuntime.runtime = "";
        Assert.assertThrows(ApplicationQueryException.class, () -> QueryStoreManager.validateQuery(queryWithInvalidRuntime));

        // Content
        Query queryWithInvalidContent = _createTestQuery.get();
        queryWithInvalidContent.content = null;
        Assert.assertThrows(ApplicationQueryException.class, () -> QueryStoreManager.validateQuery(queryWithInvalidContent));
        queryWithInvalidContent.content = "";
        Assert.assertThrows(ApplicationQueryException.class, () -> QueryStoreManager.validateQuery(queryWithInvalidContent));
    }

    @Test
    public void testGetLightQueries() throws Exception
    {
        String currentUser = "testUser";
        Query newQuery = createTestQueryWithProjectCoordinate("1", "query1", currentUser, "test.group", "test-artifact");
        queryStoreManager.createQuery(newQuery, currentUser);
        List<Query> queries = queryStoreManager.getQueries(null, null, null, false, currentUser);
        Assert.assertEquals(1, queries.size());
        Assert.assertEquals("{" +
            "\"artifactId\":\"test-artifact\"," +
            "\"content\":null," +
            "\"description\":null," +
            "\"groupId\":\"test.group\"," +
            "\"id\":\"1\"," +
            "\"mapping\":null," +
            "\"name\":\"query1\"," +
            "\"owner\":\"testUser\"," +
            "\"runtime\":null," +
            "\"versionId\":\"0.0.0\"" +
            "}", objectMapper.writeValueAsString(queries.get(0)));
    }

    @Test
    public void testGetQueriesWithLimit() throws Exception
    {
        String currentUser = "testUser";
        queryStoreManager.createQuery(createTestQuery("1", "query1", currentUser), currentUser);
        queryStoreManager.createQuery(createTestQuery("2", "query2", currentUser), currentUser);
        Assert.assertEquals(1, queryStoreManager.getQueries(null, null, 1, false, currentUser).size());
        Assert.assertEquals(2, queryStoreManager.getQueries(null, null, null, false, currentUser).size());
        Assert.assertEquals(2, queryStoreManager.getQueries(null, null, 0, false, currentUser).size());
    }

    @Test
    public void testGetQueriesWithProjectCoordinates() throws Exception
    {
        String currentUser = "testUser";
        Query testQuery1 = createTestQueryWithProjectCoordinate("1", "query1", currentUser, "test", "test");
        Query testQuery2 = createTestQueryWithProjectCoordinate("2", "query2", currentUser, "test", "test");
        Query testQuery3 = createTestQueryWithProjectCoordinate("3", "query3", currentUser, "something", "something");
        queryStoreManager.createQuery(testQuery1, currentUser);
        queryStoreManager.createQuery(testQuery2, currentUser);
        queryStoreManager.createQuery(testQuery3, currentUser);

        // When no projects specified, return all queries
        Assert.assertEquals(3, queryStoreManager.getQueries(null, null, null, false, currentUser).size());

        QueryProjectCoordinates coordinate1 = createTestQueryProjectCoordinate("notfound", "notfound");
        Assert.assertEquals(0, queryStoreManager.getQueries(null, Lists.fixedSize.of(coordinate1), null, false, currentUser).size());

        QueryProjectCoordinates coordinate2 = createTestQueryProjectCoordinate("test", "test");
        Assert.assertEquals(2, queryStoreManager.getQueries(null, Lists.fixedSize.of(coordinate2), null, false, currentUser).size());
        Assert.assertEquals(2, queryStoreManager.getQueries(null, Lists.fixedSize.of(coordinate1, coordinate2), null, false, currentUser).size());

        QueryProjectCoordinates coordinate3 = createTestQueryProjectCoordinate("something", "something");
        Assert.assertEquals(1, queryStoreManager.getQueries(null, Lists.fixedSize.of(coordinate3), null, false, currentUser).size());
        Assert.assertEquals(3, queryStoreManager.getQueries(null, Lists.fixedSize.of(coordinate1, coordinate2, coordinate3), null, false, currentUser).size());
    }

    @Test
    public void testGetQueriesWithSearchText() throws Exception
    {
        String currentUser = "testUser";
        queryStoreManager.createQuery(createTestQuery("1", "query1", currentUser), currentUser);
        queryStoreManager.createQuery(createTestQuery("2", "query2", currentUser), currentUser);
        queryStoreManager.createQuery(createTestQuery("3", "query2", currentUser), currentUser);
        Assert.assertEquals(3, queryStoreManager.getQueries(null, null, null, false, currentUser).size());
        Assert.assertEquals(1, queryStoreManager.getQueries("query1", null, null, false, currentUser).size());
        Assert.assertEquals(2, queryStoreManager.getQueries("query2", null, null, false, currentUser).size());
        Assert.assertEquals(3, queryStoreManager.getQueries("query", null, null, false, currentUser).size());
    }

    @Test
    public void testGetQueriesForCurrentUser() throws Exception
    {
        String currentUser = "testUser";
        queryStoreManager.createQuery(createTestQuery("1", "query1", currentUser), "testUser1");
        queryStoreManager.createQuery(createTestQuery("2", "query2", currentUser), currentUser);
        Assert.assertEquals(2, queryStoreManager.getQueries(null, null, null, false, currentUser).size());
        Assert.assertEquals(1, queryStoreManager.getQueries(null, null, null, true, currentUser).size());
    }

    @Test
    public void testGetNotFoundQuery()
    {
        Assert.assertThrows(ApplicationQueryException.class, () -> queryStoreManager.getQuery("1"));
    }

    @Test
    public void testCreateSimpleQuery() throws Exception
    {
        String currentUser = "testUser";
        Query newQuery = createTestQuery("1", "query1", currentUser);
        queryStoreManager.createQuery(newQuery, currentUser);
        List<Query> queries = queryStoreManager.getQueries(null, null, null, false, currentUser);
        Assert.assertEquals(1, queries.size());
        Assert.assertEquals("{" +
            "\"artifactId\":\"test-artifact\"," +
            "\"content\":\"content\"," +
            "\"description\":\"description\"," +
            "\"groupId\":\"test.group\"," +
            "\"id\":\"1\"," +
            "\"mapping\":\"mapping\"," +
            "\"name\":\"query1\"," +
            "\"owner\":\"" + currentUser + "\"," +
            "\"runtime\":\"runtime\"," +
            "\"versionId\":\"0.0.0\"" +
            "}", objectMapper.writeValueAsString(queryStoreManager.getQuery("1")));
    }

    @Test
    public void testCreateInvalidQuery()
    {
        String currentUser = "testUser";
        Assert.assertThrows(ApplicationQueryException.class, () -> queryStoreManager.createQuery(createTestQuery("1", null, currentUser), currentUser));
    }

    @Test
    public void testCreateQueryWithSameId() throws Exception
    {
        String currentUser = "testUser";
        queryStoreManager.createQuery(createTestQuery("1", "query1", currentUser), currentUser);
        Assert.assertThrows(ApplicationQueryException.class, () -> queryStoreManager.createQuery(createTestQuery("1", "query1", currentUser), currentUser));
    }

    @Test
    public void testForceCurrentUserToBeOwnerWhenCreatingQuery() throws Exception
    {
        String currentUser = "testUser";
        queryStoreManager.createQuery(createTestQuery("1", "query1", null), currentUser);
        Assert.assertEquals(currentUser, queryStoreManager.getQuery("1").owner);
        queryStoreManager.createQuery(createTestQuery("2", "query2", "testUser2"), currentUser);
        Assert.assertEquals(currentUser, queryStoreManager.getQuery("2").owner);
        queryStoreManager.createQuery(createTestQuery("3", "query1", "testUser2"), null);
        Assert.assertNull(queryStoreManager.getQuery("3").owner);
    }

    @Test
    public void testUpdateQuery() throws Exception
    {
        String currentUser = "testUser";
        queryStoreManager.createQuery(createTestQuery("1", "query1", currentUser), currentUser);
        queryStoreManager.updateQuery("1", createTestQuery("1", "query2", currentUser), currentUser);
        Assert.assertEquals("query2", queryStoreManager.getQuery("1").name);
    }

    @Test
    public void testUpdateWithInvalidQuery()
    {
        String currentUser = "testUser";
        Assert.assertThrows(ApplicationQueryException.class, () -> queryStoreManager.updateQuery("1", createTestQuery("1", null, currentUser), currentUser));
    }

    @Test
    public void testPreventUpdateQueryId() throws Exception
    {
        String currentUser = "testUser";
        queryStoreManager.createQuery(createTestQuery("1", "query1", null), null);
        Assert.assertThrows(ApplicationQueryException.class, () -> queryStoreManager.updateQuery("1", createTestQuery("2", "query1", "testUser2"), currentUser));
    }

    @Test
    public void testUpdateNotFoundQuery()
    {
        String currentUser = "testUser";
        Assert.assertThrows(ApplicationQueryException.class, () -> queryStoreManager.updateQuery("1", createTestQuery("1", "query1", currentUser), currentUser));
    }

    @Test
    public void testAllowUpdateQueryWithoutOwner() throws Exception
    {
        String currentUser = "testUser";
        queryStoreManager.createQuery(createTestQuery("1", "query1", null), null);
        queryStoreManager.updateQuery("1", createTestQuery("1", "query2", null), currentUser);
        Assert.assertEquals(currentUser, queryStoreManager.getQuery("1").owner);
    }

    @Test
    public void testForbidUpdateQueryOfAnotherUser() throws Exception
    {
        String currentUser = "testUser";
        queryStoreManager.createQuery(createTestQuery("1", "query1", currentUser), currentUser);
        Assert.assertThrows(ApplicationQueryException.class, () -> queryStoreManager.updateQuery("1", createTestQuery("1", "query1", "testUser2"), "testUser2"));
    }

    @Test
    public void testDeleteQuery() throws Exception
    {
        String currentUser = "testUser";
        queryStoreManager.createQuery(createTestQuery("1", "query1", currentUser), currentUser);
        queryStoreManager.deleteQuery("1", currentUser);
        Assert.assertEquals(0, queryStoreManager.getQueries(null, null, null, false, currentUser).size());
    }

    @Test
    public void testDeleteNotFoundQuery()
    {
        String currentUser = "testUser";
        Assert.assertThrows(ApplicationQueryException.class, () -> queryStoreManager.deleteQuery("1", currentUser));
    }

    @Test
    public void testAllowDeleteQueryWithoutOwner() throws Exception
    {
        String currentUser = "testUser";
        queryStoreManager.createQuery(createTestQuery("1", "query1", null), null);
        queryStoreManager.deleteQuery("1", currentUser);
        Assert.assertEquals(0, queryStoreManager.getQueries(null, null, null, false, currentUser).size());
    }

    @Test
    public void testForbidDeleteQueryOfAnotherUser() throws Exception
    {
        String currentUser = "testUser";
        queryStoreManager.createQuery(createTestQuery("1", "query1", currentUser), currentUser);
        Assert.assertThrows(ApplicationQueryException.class, () -> queryStoreManager.deleteQuery("1", "testUser2"));
    }

    @Test
    public void testCreateQueryEvent() throws Exception
    {
        String currentUser = "testUser";
        queryStoreManager.createQuery(createTestQuery("1", "query1", currentUser), currentUser);
        List<QueryEvent> events = queryStoreManager.getQueryEvents(null, null, null, null, null);
        Assert.assertEquals(1, events.size());
        QueryEvent event = events.get(0);
        Assert.assertEquals("1", event.queryId);
        Assert.assertEquals(QueryEvent.QueryEventType.CREATED, event.eventType);
    }

    @Test
    public void testUpdateQueryEvent() throws Exception
    {
        String currentUser = "testUser";
        Query query = createTestQuery("1", "query1", currentUser);
        queryStoreManager.createQuery(query, currentUser);
        queryStoreManager.updateQuery(query.id, query, currentUser);
        List<QueryEvent> events = queryStoreManager.getQueryEvents(null, null, null, null, null);
        Assert.assertEquals(2, events.size());
        QueryEvent event = events.get(1);
        Assert.assertEquals("1", event.queryId);
        Assert.assertEquals(QueryEvent.QueryEventType.UPDATED, event.eventType);
    }

    @Test
    public void testDeleteQueryEvent() throws Exception
    {
        String currentUser = "testUser";
        Query query = createTestQuery("1", "query1", currentUser);
        queryStoreManager.createQuery(query, currentUser);
        queryStoreManager.deleteQuery(query.id, currentUser);
        List<QueryEvent> events = queryStoreManager.getQueryEvents(null, null, null, null, null);
        Assert.assertEquals(2, events.size());
        QueryEvent event = events.get(1);
        Assert.assertEquals("1", event.queryId);
        Assert.assertEquals(QueryEvent.QueryEventType.DELETED, event.eventType);
    }

    @Test
    public void testGetQueryEvents() throws Exception
    {
        String currentUser = "testUser";
        Query query1 = createTestQuery("1", "query1", currentUser);

        // NOTE: for this test to work well, we need leave a tiny window of time (10 ms) between each operation
        // so the test for filter using timestamp can be correct
        queryStoreManager.createQuery(query1, currentUser);
        Thread.sleep(10);
        queryStoreManager.updateQuery(query1.id, query1, currentUser);
        Thread.sleep(10);
        queryStoreManager.deleteQuery(query1.id, currentUser);
        Thread.sleep(10);
        Query query2 = createTestQuery("2", "query2", currentUser);
        queryStoreManager.createQuery(query2, currentUser);
        Thread.sleep(10);
        queryStoreManager.updateQuery(query2.id, query2, currentUser);
        Thread.sleep(10);
        queryStoreManager.deleteQuery(query2.id, currentUser);
        Thread.sleep(10);

        Assert.assertEquals(6, queryStoreManager.getQueryEvents(null, null, null, null, null).size());

        // Query ID
        Assert.assertEquals(3, queryStoreManager.getQueryEvents("1", null, null, null, null).size());
        Assert.assertEquals(3, queryStoreManager.getQueryEvents("2", null, null, null, null).size());

        // Event Type
        Assert.assertEquals(2, queryStoreManager.getQueryEvents(null, QueryEvent.QueryEventType.CREATED, null, null, null).size());
        Assert.assertEquals(2, queryStoreManager.getQueryEvents(null, QueryEvent.QueryEventType.UPDATED, null, null, null).size());
        Assert.assertEquals(2, queryStoreManager.getQueryEvents(null, QueryEvent.QueryEventType.DELETED, null, null, null).size());

        // Limit
        Assert.assertEquals(1, queryStoreManager.getQueryEvents(null, null, null, null, 1).size());
        Assert.assertEquals(5, queryStoreManager.getQueryEvents(null, null, null, null, 5).size());

        Long now = Instant.now().toEpochMilli();
        Assert.assertEquals(0, queryStoreManager.getQueryEvents(null, null, now, null, null).size());
        Assert.assertEquals(6, queryStoreManager.getQueryEvents(null, null, null, now, null).size());

        QueryEvent event1 = queryStoreManager.getQueryEvents("1", QueryEvent.QueryEventType.DELETED, null, null, null).get(0);
        Assert.assertEquals(4, queryStoreManager.getQueryEvents(null, null, event1.timestamp, null, null).size());
        Assert.assertEquals(3, queryStoreManager.getQueryEvents(null, null, null, event1.timestamp, null).size());

        QueryEvent event2 = queryStoreManager.getQueryEvents("2", QueryEvent.QueryEventType.CREATED, null, null, null).get(0);
        Assert.assertEquals(3, queryStoreManager.getQueryEvents(null, null, event2.timestamp, null, null).size());
        Assert.assertEquals(4, queryStoreManager.getQueryEvents(null, null, null, event2.timestamp, null).size());
    }
}
