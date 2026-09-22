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

package com.amdocs.zusammen.adaptor.inbound.impl.item;

import com.amdocs.zusammen.datatypes.SessionContext;
import com.amdocs.zusammen.datatypes.UserInfo;
import com.amdocs.zusammen.datatypes.item.Info;
import com.amdocs.zusammen.datatypes.item.Relation;
import com.amdocs.zusammen.datatypes.response.Module;
import com.amdocs.zusammen.datatypes.response.ReturnCode;
import com.amdocs.zusammen.datatypes.response.ZusammenException;
import com.amdocs.zusammen.utils.facade.impl.AbstractFactoryBase;

import org.testng.Assert;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

final class AdaptorTestSupport {

    private AdaptorTestSupport() {
    }

    static SessionContext sessionContext(String userName) {
        SessionContext context = new SessionContext();
        context.setUser(new UserInfo(userName));
        context.setTenant("zusammen-test-tenant");
        return context;
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

    static ZusammenException failure(int errorCode, Module module, String message) {
        return new ZusammenException(new ReturnCode(errorCode, module, message, null));
    }

    /**
     * {@code ReturnCode} exposes neither its {@code ErrorCode} nor the {@code Module} through a
     * getter, so the pair a failure branch picked can only be read back off the first line of
     * {@code toString()}, which is {@code "<MODULE>-<code>"} plus {@code "-<message>"} when set.
     */
    static void assertErrorCode(ReturnCode returnCode, Module module, int errorCode) {
        Assert.assertNotNull(returnCode);
        String expected = module.name() + "-" + errorCode;
        if (returnCode.getMessage() != null) {
            expected = expected + "-" + returnCode.getMessage();
        }
        Assert.assertEquals(returnCode.toString().split("\\R", 2)[0], expected);
    }

    /**
     * There is no API to drop a registry entry, so a test class restores the production mapping by
     * re-registering it. The impl is named by string because {@code zusammen-core-impl} is a runtime
     * scoped dependency of this module - it is on the test classpath but not the compile classpath.
     */
    static <F extends AbstractFactoryBase> void restoreProductionFactory(Class<F> factory,
                                                                        String implName) {
        try {
            AbstractFactoryBase.registerFactory(factory,
                    Class.forName(implName, false, factory.getClassLoader()).asSubclass(factory));
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException(implName + " is not on the test classpath", e);
        }
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
