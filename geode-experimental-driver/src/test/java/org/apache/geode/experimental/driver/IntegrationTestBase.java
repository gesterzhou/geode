/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional information regarding
 * copyright ownership. The ASF licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License. You may obtain a
 * copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package org.apache.geode.experimental.driver;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.contrib.java.lang.system.RestoreSystemProperties;

import org.apache.geode.cache.Cache;
import org.apache.geode.cache.CacheFactory;
import org.apache.geode.cache.Region;
import org.apache.geode.cache.RegionShortcut;
import org.apache.geode.cache.execute.Function;
import org.apache.geode.cache.execute.FunctionContext;
import org.apache.geode.cache.execute.RegionFunctionContext;
import org.apache.geode.cache.execute.ResultSender;
import org.apache.geode.cache.server.CacheServer;
import org.apache.geode.distributed.ConfigurationProperties;
import org.apache.geode.distributed.Locator;

/**
 * Created by dan on 2/23/18.
 */
public class IntegrationTestBase {
  protected static final String NAME = "name";
  protected static final String GROUP = "group";
  protected static final String REGION = "region";
  protected static final String FUNCTION_ID = "function";

  @Rule
  public RestoreSystemProperties restoreSystemProperties = new RestoreSystemProperties();
  protected Driver driver;
  protected org.apache.geode.cache.Region<Object, Object> serverRegion;
  private Locator locator;
  private Cache cache;

  @Before
  public void createServerAndDriver() throws Exception {
    System.setProperty("geode.feature-protobuf-protocol", "true");

    // Create a cache
    CacheFactory cf = new CacheFactory();
    cf.set(ConfigurationProperties.MCAST_PORT, "0");
    cf.set(ConfigurationProperties.NAME, NAME);
    cf.set(ConfigurationProperties.GROUPS, GROUP);
    cache = cf.create();

    // Start a locator
    locator = Locator.startLocatorAndDS(0, null, new Properties());
    int locatorPort = locator.getPort();

    // Start a server
    CacheServer server = cache.addCacheServer();
    server.setPort(0);
    server.start();

    // Create a region
    serverRegion = cache.createRegionFactory(RegionShortcut.REPLICATE).create(REGION);

    // Register a function
    org.apache.geode.cache.execute.FunctionService.registerFunction(new TestFunction());

    // Create a driver connected to the server
    driver = new DriverFactory().addLocator("localhost", locatorPort).create();
  }

  @After
  public void cleanup() {
    locator.stop();
    cache.close();
  }

  class TestFunction implements org.apache.geode.cache.execute.Function {
    @Override
    public String getId() {
      return FUNCTION_ID;
    }

    @Override
    public void execute(FunctionContext context) {
      final ResultSender resultSender = context.getResultSender();
      resultSender.sendResult("first result");
      resultSender.sendResult("next result");
      resultSender.lastResult("last result");
    }
  }
}
