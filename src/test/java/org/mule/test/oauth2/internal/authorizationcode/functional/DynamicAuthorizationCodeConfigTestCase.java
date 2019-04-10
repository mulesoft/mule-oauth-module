/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.oauth2.internal.authorizationcode.functional;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.junit.Assert.assertThat;
import org.mule.runtime.extension.api.runtime.config.ConfigurationInstance;
import org.mule.runtime.extension.api.runtime.config.ConfigurationProvider;
import org.mule.tck.junit4.rule.SystemProperty;

import org.junit.Rule;
import org.junit.Test;

public class DynamicAuthorizationCodeConfigTestCase extends AbstractAuthorizationCodeBasicTestCase {

  @Rule
  public SystemProperty externalUrl = new SystemProperty("external.redirect.url", "http://app.cloudhub.com:1234/callback");

  @Override
  protected String getConfigFile() {
    return "authorization-code/authorization-code-dynamic-config.xml";
  }

  @Test
  public void sameInstanceForEquivalentValues() throws Exception {
    ConfigurationProvider configurationProvider = registry.<ConfigurationProvider>lookupByName("requestConfig").get();
    assertThat(configurationProvider, is(not(nullValue())));

    ConfigurationInstance config1 = configurationProvider.get(testEvent());
    ConfigurationInstance config2 = configurationProvider.get(testEvent());

    assertThat(config1.getValue(), is(sameInstance(config2.getValue())));
  }

  @Test
  @Override
  public void localAuthorizationUrlRedirectsToOAuthAuthorizationUrl() throws Exception {
    // this inherited test doesn't apply here
  }
}
