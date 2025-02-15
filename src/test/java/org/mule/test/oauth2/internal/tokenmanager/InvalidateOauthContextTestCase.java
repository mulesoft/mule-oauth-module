/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.oauth2.internal.tokenmanager;

import static org.mule.extension.oauth2.internal.service.OAuthContextServiceAdapter.getAccessToken;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static org.mule.runtime.oauth.api.state.ResourceOwnerOAuthContext.DEFAULT_RESOURCE_OWNER_ID;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;

import org.mule.extension.oauth2.api.tokenmanager.TokenManagerConfig;
import org.mule.extension.oauth2.internal.authorizationcode.state.ConfigOAuthContext;
import org.mule.test.oauth2.AbstractOAuthAuthorizationTestCase;

import org.junit.Test;

public class InvalidateOauthContextTestCase extends AbstractOAuthAuthorizationTestCase {

  public static final String ACCESS_TOKEN = "Access_token";
  public static final String RESOURCE_OWNER_JOHN = "john";
  public static final String RESOURCE_OWNER_TONY = "tony";

  @Override
  protected String getConfigFile() {
    return "tokenmanager/invalidate-oauth-context-config.xml";
  }

  @Test
  public void invalidateTokenManagerGeneralOauthContext() throws Exception {
    TokenManagerConfig tokenManagerConfig = registry.<TokenManagerConfig>lookupByName("tokenManagerConfig").get();
    initialiseIfNeeded(tokenManagerConfig, muleContext);
    final ConfigOAuthContext configOAuthContext = tokenManagerConfig.getConfigOAuthContext();
    loadResourceOwnerWithAccessToken(configOAuthContext, DEFAULT_RESOURCE_OWNER_ID);
    flowRunner("invalidateOauthContext").withPayload(TEST_MESSAGE).run();
    assertThatOAuthContextWasCleanForUser(configOAuthContext, DEFAULT_RESOURCE_OWNER_ID);
  }

  @Test
  public void invalidateTokenManagerGeneralOauthContextForResourceOwnerId() throws Exception {
    TokenManagerConfig tokenManagerConfig = registry.<TokenManagerConfig>lookupByName("tokenManagerConfig").get();
    initialiseIfNeeded(tokenManagerConfig, muleContext);
    final ConfigOAuthContext configOAuthContext = tokenManagerConfig.getConfigOAuthContext();
    loadResourceOwnerWithAccessToken(configOAuthContext, RESOURCE_OWNER_JOHN);
    loadResourceOwnerWithAccessToken(configOAuthContext, RESOURCE_OWNER_TONY);

    flowRunner("invalidateOauthContextWithResourceOwnerId").withPayload(TEST_MESSAGE)
        .withVariable("resourceOwnerId", RESOURCE_OWNER_TONY).run();
    assertThatOAuthContextWasCleanForUser(configOAuthContext, RESOURCE_OWNER_TONY);
    assertThat(getAccessToken(configOAuthContext.getContextForResourceOwner(RESOURCE_OWNER_JOHN)), is(ACCESS_TOKEN));
  }

  @Test
  public void invalidateTokenManagerForNonExistentResourceOwnerId() throws Exception {
    flowRunner("invalidateOauthContextWithResourceOwnerId").withPayload(TEST_MESSAGE).runExpectingException();
  }

  private void assertThatOAuthContextWasCleanForUser(ConfigOAuthContext configOAuthContext, String resourceOwnerId) {
    assertThat(getAccessToken(configOAuthContext.getContextForResourceOwner(resourceOwnerId)), nullValue());
  }

  private void loadResourceOwnerWithAccessToken(ConfigOAuthContext configOAuthContext, String resourceOwnerId) {
    final Object resourceOwnerContext = configOAuthContext.getContextForResourceOwner(resourceOwnerId);
    setTokens(resourceOwnerContext, ACCESS_TOKEN, null);
    configOAuthContext.updateResourceOwnerOAuthContext(resourceOwnerContext);
  }
}
