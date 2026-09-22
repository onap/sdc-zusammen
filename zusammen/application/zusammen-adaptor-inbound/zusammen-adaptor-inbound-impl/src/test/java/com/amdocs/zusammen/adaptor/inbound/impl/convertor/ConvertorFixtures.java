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

package com.amdocs.zusammen.adaptor.inbound.impl.convertor;

import com.amdocs.zusammen.datatypes.Id;
import com.amdocs.zusammen.datatypes.Namespace;
import com.amdocs.zusammen.datatypes.item.Action;
import com.amdocs.zusammen.datatypes.item.Info;
import com.amdocs.zusammen.datatypes.item.ItemVersion;
import com.amdocs.zusammen.datatypes.item.ItemVersionChange;
import com.amdocs.zusammen.datatypes.item.ItemVersionData;
import com.amdocs.zusammen.datatypes.item.ItemVersionDataConflict;
import com.amdocs.zusammen.datatypes.item.Relation;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

final class ConvertorFixtures {

    private ConvertorFixtures() {
    }

    static Info info(String name) {
        Info info = new Info();
        info.setName(name);
        info.setDescription(name + "-description");
        info.addProperty(name + "-property", name + "-value");
        return info;
    }

    static Relation relation(String type) {
        Relation relation = new Relation();
        relation.setType(type);
        relation.setInfo(info(type + "-info"));
        return relation;
    }

    static Namespace namespace(String value) {
        Namespace namespace = new Namespace();
        namespace.setValue(value);
        return namespace;
    }

    static ItemVersionChange itemVersionChange(String versionId, Action action) {
        ItemVersionData data = new ItemVersionData();
        data.setInfo(info(versionId + "-version-info"));
        ItemVersion itemVersion = new ItemVersion();
        itemVersion.setId(new Id(versionId));
        itemVersion.setData(data);
        ItemVersionChange change = new ItemVersionChange();
        change.setItemVersion(itemVersion);
        change.setAction(action);
        return change;
    }

    static ItemVersionDataConflict versionDataConflict(String localName, String remoteName) {
        ItemVersionData local = new ItemVersionData();
        local.setInfo(info(localName));
        ItemVersionData remote = new ItemVersionData();
        remote.setInfo(info(remoteName));
        ItemVersionDataConflict conflict = new ItemVersionDataConflict();
        conflict.setLocalData(local);
        conflict.setRemoteData(remote);
        return conflict;
    }

    static InputStream stream(String content) {
        return new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
    }

    static String text(InputStream input) {
        if (input == null) {
            return null;
        }
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[256];
        try {
            int read;
            while ((read = input.read(buffer)) != -1) {
                output.write(buffer, 0, read);
            }
        } catch (IOException e) {
            throw new IllegalStateException("could not read stream content", e);
        }
        return new String(output.toByteArray(), StandardCharsets.UTF_8);
    }
}
