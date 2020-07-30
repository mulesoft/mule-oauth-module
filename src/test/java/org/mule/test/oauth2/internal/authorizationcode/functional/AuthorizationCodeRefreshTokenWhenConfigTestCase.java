/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.oauth2.internal.authorizationcode.functional;

import static org.mule.runtime.oauth.api.state.ResourceOwnerOAuthContext.DEFAULT_RESOURCE_OWNER_ID;

import org.junit.Rule;
import org.junit.Test;
import org.mule.extension.oauth2.api.tokenmanager.TokenManagerConfig;
import org.mule.functional.api.exception.ExpectedError;

import javax.inject.Inject;

public class AuthorizationCodeRefreshTokenWhenConfigTestCase extends AbstractAuthorizationCodeRefreshTokenWhenConfigTestCase {

  @Inject
  TokenManagerConfig tokenManagerConfig;

  @Rule
  public ExpectedError expectedError = ExpectedError.none();

  @Override
  protected String getConfigFile() {
    return "authorization-code/authorization-code-refresh-token-when-config.xml";
  }

  @Test
  public void afterFailureDoRefreshWhenTokenWithDefaultValueNoResourceOwnerId() throws Exception {
    executeRefreshTokenWhen("testFlowRefreshTokenWhen", DEFAULT_RESOURCE_OWNER_ID, 403);
  }

  @Override
  protected TokenManagerConfig getTokenManagerConfig() {
    return tokenManagerConfig;
  }

}
