/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.oauth2.internal.clientcredentials.functional;

import static org.mule.extension.oauth2.internal.service.OAuthContextServiceAdapter.getAccessToken;
import static org.mule.runtime.oauth.api.state.ResourceOwnerOAuthContext.DEFAULT_RESOURCE_OWNER_ID;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import org.mule.extension.oauth2.api.tokenmanager.TokenManagerConfig;

import org.junit.Test;

public class ClientCredentialsNoTokenManagerConfigTestCase extends AbstractClientCredentialsBasicTestCase {

  @Test
  public void authenticationIsDoneOnStartup() throws Exception {
    verifyRequestDoneToTokenUrlForClientCredentials();

    Object oauthContext = registry.<TokenManagerConfig>lookupByName("tokenManagerConfig").get().getConfigOAuthContext()
        .getContextForResourceOwner(DEFAULT_RESOURCE_OWNER_ID);
    assertThat(getAccessToken(oauthContext), is(ACCESS_TOKEN));
  }

}
