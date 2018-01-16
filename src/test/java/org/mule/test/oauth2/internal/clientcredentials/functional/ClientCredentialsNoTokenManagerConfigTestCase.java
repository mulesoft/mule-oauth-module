/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.oauth2.internal.clientcredentials.functional;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mule.runtime.oauth.api.state.ResourceOwnerOAuthContext.DEFAULT_RESOURCE_OWNER_ID;

import org.mule.extension.oauth2.api.tokenmanager.TokenManagerConfig;
import org.mule.runtime.oauth.api.state.ResourceOwnerOAuthContext;

import org.junit.Test;

public class ClientCredentialsNoTokenManagerConfigTestCase extends AbstractClientCredentialsBasicTestCase {

  @Test
  public void authenticationIsDoneOnStartup() throws Exception {
    verifyRequestDoneToTokenUrlForClientCredentials();

    final ResourceOwnerOAuthContext oauthContext =
        registry.<TokenManagerConfig>lookupByName("tokenManagerConfig").get().getConfigOAuthContext()
            .getContextForResourceOwner(DEFAULT_RESOURCE_OWNER_ID);
    assertThat(oauthContext.getAccessToken(), is(ACCESS_TOKEN));
  }

}
