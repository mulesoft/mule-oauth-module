/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.oauth2.internal.tokenmanager;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.disposeIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.startIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.stopIfNeeded;

import org.mule.extension.oauth2.api.tokenmanager.TokenManagerConfig;
import org.mule.runtime.oauth.api.state.ResourceOwnerOAuthContext;
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

  @Test
  @Issue("OAMOD-6")
  public void resetTokenManagerWithoutObjectStoreConfigShouldRetainValues() throws Exception {
    // creates a token manager config with a default object store
    TokenManagerConfig<? extends ResourceOwnerOAuthContext> tokenManagerConfig = new TokenManagerConfig<>();
    tokenManagerConfig.setName("tokenManagerConfig");
    initialiseIfNeeded(tokenManagerConfig, muleContext);
    startIfNeeded(tokenManagerConfig);

    // tries to use the object store of the token manager config
    // (since there is no context for the given resource owner ID yet, a new one should be added to the store)
    tokenManagerConfig.getConfigOAuthContext().getContextForResourceOwner("resourceOwnerId");

    // control test, to see that our assumptions are correct
    assertThat(tokenManagerConfig.getResolvedObjectStore().contains("resourceOwnerId"), is(true));

    // disposes the token manager config
    stopIfNeeded(tokenManagerConfig);
    disposeIfNeeded(tokenManagerConfig, LOGGER);

    // creates a new token manager config, also using the default object store
    tokenManagerConfig = new TokenManagerConfig<>();
    tokenManagerConfig.setName("tokenManagerConfig");
    initialiseIfNeeded(tokenManagerConfig, muleContext);
    startIfNeeded(tokenManagerConfig);

    // the object store, being persistent, should still contain the context for the resource owner ID
    assertThat(tokenManagerConfig.getResolvedObjectStore().contains("resourceOwnerId"), is(true));

    // disposes the second token manager config
    stopIfNeeded(tokenManagerConfig);
    disposeIfNeeded(tokenManagerConfig, LOGGER);
  }

}
