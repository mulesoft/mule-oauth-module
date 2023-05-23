/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.oauth2.internal.clientcredentials.functional;

import static java.lang.String.format;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.junit.Assert.assertThat;
import org.mule.runtime.extension.api.runtime.config.ConfigurationInstance;
import org.mule.runtime.extension.api.runtime.config.ConfigurationProvider;
import org.mule.tck.junit4.rule.SystemProperty;
import org.mule.test.oauth2.AbstractOAuthAuthorizationTestCase;

import org.junit.Rule;
import org.junit.Test;

public class DynamicClientCredentialsConfigTestCase extends AbstractOAuthAuthorizationTestCase {

  @Rule
  public SystemProperty tokenUrl =
      new SystemProperty("token.url", format("http://localhost:%d" + TOKEN_PATH, oauthServerPort.getNumber()));

  @Override
  protected String getConfigFile() {
    return "client-credentials/client-credentials-dynamic-config.xml";
  }

  @Override
  public void doSetUpBeforeMuleContextCreation() {
    configureWireMockToExpectTokenPathRequestForClientCredentialsGrantTypeWithMapResponse(ACCESS_TOKEN);
    configureProxyWireMock();
  }

  @Test
  public void sameInstanceForEquivalentValues() throws Exception {
    ConfigurationProvider configurationProvider = registry.<ConfigurationProvider>lookupByName("requestConfigWithOAuth").get();
    assertThat(configurationProvider, is(not(nullValue())));

    ConfigurationInstance config1 = configurationProvider.get(testEvent());
    ConfigurationInstance config2 = configurationProvider.get(testEvent());

    assertThat(config1.getValue(), is(sameInstance(config2.getValue())));
  }

}
