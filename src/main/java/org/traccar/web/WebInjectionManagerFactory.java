/*
 * Copyright 2022 Anton Tananaev (anton@traccar.org)
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
package org.traccar.web;

import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.jersey.inject.hk2.Hk2InjectionManagerFactory;
import org.glassfish.jersey.internal.inject.InjectionManager;
import org.glassfish.jersey.internal.inject.InjectionManagerFactory;
import org.jvnet.hk2.guice.bridge.api.GuiceBridge;
import org.jvnet.hk2.guice.bridge.api.GuiceIntoHK2Bridge;
import org.traccar.Main;

import jakarta.annotation.Priority;

@Priority(20)
public class WebInjectionManagerFactory implements InjectionManagerFactory {

    private final InjectionManagerFactory originalFactory = new Hk2InjectionManagerFactory();

    private InjectionManager injectGuiceBridge(InjectionManager injectionManager) {
        var serviceLocator = injectionManager.getInstance(ServiceLocator.class);
        GuiceBridge.getGuiceBridge().initializeGuiceBridge(serviceLocator);
        var guiceBridge = serviceLocator.getService(GuiceIntoHK2Bridge.class);
        guiceBridge.bridgeGuiceInjector(Main.getInjector());
        return injectionManager;
    }

    @Override
    public InjectionManager create() {
        return injectGuiceBridge(originalFactory.create());
    }

    @Override
    public InjectionManager create(Object parent) {
        return injectGuiceBridge(originalFactory.create(parent));
    }
}
