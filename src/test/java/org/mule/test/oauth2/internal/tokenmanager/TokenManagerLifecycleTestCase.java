/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.oauth2.internal.tokenmanager;

import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.disposeIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.startIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.stopIfNeeded;

import org.mule.extension.oauth2.api.tokenmanager.TokenManagerConfig;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.qameta.allure.Issue;

public class TokenManagerLifecycleTestCase extends AbstractMuleContextTestCase {

  private static final Logger LOGGER = LoggerFactory.getLogger(TokenManagerLifecycleTestCase.class);

  @Test
  @Issue("OAMOD-2")
  public void resetTokenManagerWithoutObjectStoreConfig() throws Exception {
    TokenManagerConfig tokenManagerConfig = new TokenManagerConfig<>();
    tokenManagerConfig.setName("tokenManagerConfig");

    initialiseIfNeeded(tokenManagerConfig, muleContext);
    startIfNeeded(tokenManagerConfig);
    stopIfNeeded(tokenManagerConfig);
    disposeIfNeeded(tokenManagerConfig, LOGGER);

    tokenManagerConfig = new TokenManagerConfig<>();
    tokenManagerConfig.setName("tokenManagerConfig");

    initialiseIfNeeded(tokenManagerConfig, muleContext);
    startIfNeeded(tokenManagerConfig);
    stopIfNeeded(tokenManagerConfig);
    disposeIfNeeded(tokenManagerConfig, LOGGER);
  }

}
