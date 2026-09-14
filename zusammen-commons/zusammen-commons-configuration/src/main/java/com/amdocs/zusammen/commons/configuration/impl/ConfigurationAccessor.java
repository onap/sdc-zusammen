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

package com.amdocs.zusammen.commons.configuration.impl;

import com.amdocs.zusammen.commons.configuration.ConfigurationManagerFactory;

import java.util.Optional;


public class ConfigurationAccessor {

    private static final String MISSING_PROPERTY_MSG = "property %s does not exist";
    private static final String MISSING_PLUGIN_PROPERTY_MSG =
            "property %s does not exist for plugin %s";

    public static <T> T getProperty(String propertyName, T defaultValue) {
        try {
            return getProperty(propertyName);
        } catch (RuntimeException re) {
            return defaultValue;
        }

    }

    public static <T> T getProperty(String propertyName) {
        Optional<T> property = getOptionalProperty(propertyName);
        return property.orElseThrow(() ->
                new RuntimeException(String.format(MISSING_PROPERTY_MSG, propertyName)));
    }

    public static <T> Optional<T> getOptionalProperty(String propertyName) {
        Object value = System.getProperty(propertyName);
        return value == null ?
                ConfigurationManagerFactory.getInstance().createInterface().getProperty(propertyName) :
                Optional.of((T) value);
    }

    public static <T> T getPluginProperty(String pluginName, String propertyName) {
        Optional<T> property = getOptionalPluginProperty(pluginName, propertyName);
        return property.orElseThrow(() ->
                new RuntimeException(String.format(MISSING_PLUGIN_PROPERTY_MSG, propertyName, pluginName)));
    }

    public static <T> Optional<T> getOptionalPluginProperty(String pluginName, String propertyName) {
        Object value = System.getProperty(propertyName);
        if (value == null) {
            value = ConfigurationManagerFactory.getInstance().createInterface()
                    .getPluginInfo(pluginName).getProperties().get(propertyName);

            if (value == null) {
                return Optional.empty();
            }
        }
        return Optional.of((T) value);
    }
}
