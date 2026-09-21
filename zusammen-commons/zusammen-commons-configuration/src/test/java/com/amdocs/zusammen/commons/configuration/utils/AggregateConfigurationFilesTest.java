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
package com.amdocs.zusammen.commons.configuration.utils;

import com.amdocs.zusammen.commons.configuration.datatypes.Configuration;
import com.amdocs.zusammen.commons.configuration.datatypes.PluginInfo;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class AggregateConfigurationFilesTest {

    private static final String STATE_STORE = "zusammen_state_store";
    private static final String SEARCH_INDEX = "zusammen_search_index";

    private static final String LEVEL_1_BASE =
            "{"
                    + "  \"level\": 1,"
                    + "  \"configuration\": {"
                    + "    \"properties\": {"
                    + "      \"cassandra.keyspace\": \"zusammen_base\","
                    + "      \"cassandra.port\": \"9042\""
                    + "    },"
                    + "    \"plugins\": {"
                    + "      \"zusammen_state_store\": {"
                    + "        \"implementationClass\": \"com.example.BaseStateStore\","
                    + "        \"properties\": { \"cassandra.nodes\": \"base-node\" }"
                    + "      }"
                    + "    }"
                    + "  }"
                    + "}";

    private static final String LEVEL_5_OVERRIDE =
            "{"
                    + "  \"level\": 5,"
                    + "  \"configuration\": {"
                    + "    \"properties\": {"
                    + "      \"cassandra.keyspace\": \"zusammen_override\""
                    + "    },"
                    + "    \"plugins\": {"
                    + "      \"zusammen_state_store\": {"
                    + "        \"implementationClass\": \"com.example.OverrideStateStore\","
                    + "        \"properties\": {"
                    + "          \"cassandra.nodes\": \"override-node\","
                    + "          \"cassandra.ssl\": \"true\""
                    + "        }"
                    + "      }"
                    + "    }"
                    + "  }"
                    + "}";

    @Test
    public void testAggregateOfNoFilesYieldsEmptyConfiguration() {
        Configuration configuration =
                AggregateConfigurationFiles.aggregate(new ArrayList<InputStream>());

        Assert.assertTrue(configuration.getProperties().isEmpty());
        Assert.assertTrue(configuration.getPlugins().isEmpty());
    }

    @Test
    public void testAggregateOfSingleFileCopiesPropertiesAndPlugins() {
        Configuration configuration = aggregate(LEVEL_1_BASE);

        Assert.assertEquals(configuration.getProperties().size(), 2);
        Assert.assertEquals(configuration.getProperties().get("cassandra.keyspace"), "zusammen_base");
        Assert.assertEquals(configuration.getProperties().get("cassandra.port"), "9042");

        PluginInfo stateStore = configuration.getPlugins().get(STATE_STORE);
        Assert.assertEquals(stateStore.getImplementationClass(), "com.example.BaseStateStore");
        Assert.assertEquals(stateStore.getProperties().get("cassandra.nodes"), "base-node");
    }

    @Test
    public void testAggregateAppliesHigherLevelLastRegardlessOfInputOrder() {
        Configuration configuration = aggregate(LEVEL_5_OVERRIDE, LEVEL_1_BASE);

        Assert.assertEquals(configuration.getProperties().get("cassandra.keyspace"),
                "zusammen_override");
        Assert.assertEquals(configuration.getProperties().get("cassandra.port"), "9042");

        PluginInfo stateStore = configuration.getPlugins().get(STATE_STORE);
        Assert.assertEquals(stateStore.getImplementationClass(), "com.example.OverrideStateStore");
        Assert.assertEquals(stateStore.getProperties().get("cassandra.nodes"), "override-node");
        Assert.assertEquals(stateStore.getProperties().get("cassandra.ssl"), "true");
    }

    /**
     * level is a member of ConfigurationInfo and only orders a descriptor when it is written as a
     * sibling of configuration. Nested inside configuration it has no member to bind to, so the
     * parser drops it and the descriptor is ordered at the default 0 - losing even to level 1.
     */
    @Test
    public void testAggregateIgnoresLevelNestedInsideConfiguration() {
        String nestedLevel10 =
                "{"
                        + "  \"configuration\": {"
                        + "    \"level\": 10,"
                        + "    \"plugins\": {"
                        + "      \"zusammen_state_store\": {"
                        + "        \"implementationClass\": \"com.example.NestedLevelStateStore\""
                        + "      }"
                        + "    }"
                        + "  }"
                        + "}";

        Assert.assertEquals(aggregate(nestedLevel10, LEVEL_1_BASE).getPlugins().get(STATE_STORE)
                .getImplementationClass(), "com.example.BaseStateStore");
        Assert.assertEquals(aggregate(LEVEL_1_BASE, nestedLevel10).getPlugins().get(STATE_STORE)
                .getImplementationClass(), "com.example.BaseStateStore");
    }

    @Test
    public void testAggregateAddsPropertyThatOnlyTheHigherLevelDeclares() {
        String base = descriptor(1, "\"cassandra.keyspace\": \"zusammen\"");
        String override = descriptor(2, "\"cassandra.datacenter\": \"dc1\"");

        Configuration configuration = aggregate(base, override);

        Assert.assertEquals(configuration.getProperties().size(), 2);
        Assert.assertEquals(configuration.getProperties().get("cassandra.keyspace"), "zusammen");
        Assert.assertEquals(configuration.getProperties().get("cassandra.datacenter"), "dc1");
    }

    @Test
    public void testAggregateDropsBasePropertyWhenOverrideValueIsNull() {
        Configuration configuration =
                aggregate(LEVEL_1_BASE, descriptor(5, "\"cassandra.port\": null"));

        Assert.assertEquals(configuration.getProperties().get("cassandra.keyspace"),
                "zusammen_base");
        // the key survives: overrideProperties removes it and then puts the whole override map back
        // with putAll, so only the resolved value can be asserted on, not containsKey
        Assert.assertNull(configuration.getProperties().get("cassandra.port"));
    }

    @Test
    public void testAggregateAddsPluginThatOnlyTheHigherLevelDeclares() {
        String override =
                "{"
                        + "  \"level\": 9,"
                        + "  \"configuration\": {"
                        + "    \"plugins\": {"
                        + "      \"zusammen_search_index\": {"
                        + "        \"implementationClass\": \"com.example.EmptySearchIndex\""
                        + "      }"
                        + "    }"
                        + "  }"
                        + "}";

        Configuration configuration = aggregate(LEVEL_1_BASE, override);

        Assert.assertEquals(configuration.getPlugins().size(), 2);
        Assert.assertEquals(configuration.getPlugins().get(STATE_STORE).getImplementationClass(),
                "com.example.BaseStateStore");
        Assert.assertEquals(configuration.getPlugins().get(SEARCH_INDEX).getImplementationClass(),
                "com.example.EmptySearchIndex");
    }

    @Test
    public void testAggregateKeepsLowerLevelImplementationClassWhenOverrideOmitsIt() {
        String override =
                "{"
                        + "  \"level\": 9,"
                        + "  \"configuration\": {"
                        + "    \"plugins\": {"
                        + "      \"zusammen_state_store\": {"
                        + "        \"properties\": { \"cassandra.consistency.level\": \"QUORUM\" }"
                        + "      }"
                        + "    }"
                        + "  }"
                        + "}";

        Configuration configuration = aggregate(LEVEL_1_BASE, override);

        PluginInfo stateStore = configuration.getPlugins().get(STATE_STORE);
        Assert.assertEquals(stateStore.getImplementationClass(), "com.example.BaseStateStore");
        Assert.assertEquals(stateStore.getProperties().get("cassandra.nodes"), "base-node");
        Assert.assertEquals(stateStore.getProperties().get("cassandra.consistency.level"), "QUORUM");
    }

    @Test
    public void testAggregateKeepsLowerLevelPluginsWhenHigherLevelDeclaresNone() {
        String override = descriptor(9, "\"cassandra.keyspace\": \"zusammen_override\"");

        Configuration configuration = aggregate(LEVEL_1_BASE, override);

        Assert.assertEquals(configuration.getPlugins().size(), 1);
        Assert.assertEquals(configuration.getPlugins().get(STATE_STORE).getImplementationClass(),
                "com.example.BaseStateStore");
        Assert.assertEquals(configuration.getProperties().get("cassandra.keyspace"),
                "zusammen_override");
    }

    @Test
    public void testAggregateReadsUnquotedNumberAsDouble() {
        Configuration configuration = aggregate(descriptor(1, "\"cassandra.port\": 9042"));

        Assert.assertEquals(configuration.getProperties().get("cassandra.port"),
                Double.valueOf(9042));
    }

    @Test(expectedExceptions = RuntimeException.class)
    public void testAggregateFailsOnMalformedJson() {
        aggregate("{ \"level\": 1, \"configuration\": ");
    }

    @Test(expectedExceptions = RuntimeException.class)
    public void testAggregateFailsOnDescriptorWithoutConfigurationSection() {
        aggregate("{ \"level\": 1 }");
    }

    @Test(expectedExceptions = RuntimeException.class)
    public void testAggregateFailsOnEmptyDescriptor() {
        aggregate("");
    }

    private static Configuration aggregate(String... descriptors) {
        List<InputStream> streams = new ArrayList<>();
        for (String descriptor : descriptors) {
            streams.add(new ByteArrayInputStream(descriptor.getBytes(StandardCharsets.UTF_8)));
        }
        return AggregateConfigurationFiles.aggregate(streams);
    }

    private static String descriptor(int level, String propertyEntry) {
        return "{ \"level\": " + level + ", \"configuration\": { \"properties\": { "
                + propertyEntry + " } } }";
    }
}
