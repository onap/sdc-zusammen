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
import org.testng.annotations.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;

public class AggregateConfigurationFilesTest {

  private static final String PLUGIN_TYPE = "zusammen_state_store";
  private static final String LOW_LEVEL_IMPL = "com.example.LowLevelStateStore";
  private static final String HIGH_LEVEL_IMPL = "com.example.HighLevelStateStore";

  private static final String LEVEL_1_DESCRIPTOR = descriptor("\"level\": 1,", LOW_LEVEL_IMPL);
  private static final String LEVEL_10_DESCRIPTOR = descriptor("\"level\": 10,", HIGH_LEVEL_IMPL);

  @Test
  public void testHigherLevelOverridesLowerLevel() {
    Configuration configuration = aggregate(LEVEL_1_DESCRIPTOR, LEVEL_10_DESCRIPTOR);

    assertEquals(configuration.getPlugins().get(PLUGIN_TYPE).getImplementationClass(),
        HIGH_LEVEL_IMPL);
  }

  @Test
  public void testHigherLevelOverridesLowerLevelRegardlessOfInputOrder() {
    Configuration configuration = aggregate(LEVEL_10_DESCRIPTOR, LEVEL_1_DESCRIPTOR);

    assertEquals(configuration.getPlugins().get(PLUGIN_TYPE).getImplementationClass(),
        HIGH_LEVEL_IMPL);
  }

  /**
   * level is a member of ConfigurationInfo, so it only orders a descriptor when it is written as a
   * sibling of configuration. Nested inside configuration it is dropped by the json parser and the
   * descriptor is applied at level 0, losing to every descriptor that declares a level correctly -
   * even to a lower number such as 1.
   */
  @Test
  public void testLevelNestedInsideConfigurationIsIgnored() {
    String nestedLevel10 = "{\"configuration\": {\"level\": 10, \"plugins\": {\"" + PLUGIN_TYPE
        + "\": {\"implementationClass\": \"" + HIGH_LEVEL_IMPL + "\"}}}}";

    assertEquals(aggregate(nestedLevel10, LEVEL_1_DESCRIPTOR).getPlugins().get(PLUGIN_TYPE)
        .getImplementationClass(), LOW_LEVEL_IMPL);
    assertEquals(aggregate(LEVEL_1_DESCRIPTOR, nestedLevel10).getPlugins().get(PLUGIN_TYPE)
        .getImplementationClass(), LOW_LEVEL_IMPL);
  }

  @Test
  public void testNullOverrideValueDropsBaseProperty() {
    String base = "{\"level\": 1, \"configuration\": {\"properties\": "
        + "{\"kept.property\": \"base\", \"dropped.property\": \"base\"}}}";
    String override = "{\"level\": 10, \"configuration\": {\"properties\": "
        + "{\"dropped.property\": null}}}";

    Configuration configuration = aggregate(base, override);

    assertEquals(configuration.getProperties().get("kept.property"), "base");
    // the key itself survives - overrideProperties removes it and then puts the whole override map
    // back with putAll - so only the resolved value can be asserted on, not containsKey
    assertNull(configuration.getProperties().get("dropped.property"));
  }

  private static Configuration aggregate(String... descriptors) {
    List<InputStream> streams = Arrays.stream(descriptors)
        .map(descriptor -> (InputStream) new ByteArrayInputStream(
            descriptor.getBytes(StandardCharsets.UTF_8)))
        .collect(Collectors.toList());
    return AggregateConfigurationFiles.aggregate(streams);
  }

  private static String descriptor(String level, String implementationClass) {
    return "{" + level + " \"configuration\": {\"plugins\": {\"" + PLUGIN_TYPE
        + "\": {\"implementationClass\": \"" + implementationClass + "\"}}}}";
  }
}
