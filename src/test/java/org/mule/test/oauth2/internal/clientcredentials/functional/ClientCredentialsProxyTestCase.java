/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.oauth2.internal.clientcredentials.functional;

import static java.lang.String.format;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;

import org.mule.tck.junit4.rule.SystemProperty;
import org.mule.test.oauth2.AbstractOAuthAuthorizationTestCase;
import org.mule.test.runner.RunnerDelegateTo;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.Collection;

@RunnerDelegateTo(Parameterized.class)
public class ClientCredentialsProxyTestCase extends AbstractOAuthAuthorizationTestCase {

  @Parameter
  public String configFile;

  @Rule
  public SystemProperty tokenUrl =
      new SystemProperty("token.url", format("http://localhost:%d" + TOKEN_PATH, oauthServerPort.getNumber()));

  @Parameterized.Parameters
  public static Collection<Object> data() {
    String inlineProxyConfig = "client-credentials/client-credentials-through-proxy-inline-config.xml";
    String globalProxyConfig = "client-credentials/client-credentials-through-proxy-config.xml";
    return Arrays.asList(new Object[] {inlineProxyConfig, globalProxyConfig});
  }

  @Override
  protected String getConfigFile() {
    return configFile;
  }

  @Override
  public void doSetUpBeforeMuleContextCreation() {
    configureWireMockToExpectTokenPathRequestForClientCredentialsGrantTypeWithMapResponse(ACCESS_TOKEN);
    configureProxyWireMock();
  }

  @Test
  public void tokenRequestThroughProxy() throws UnsupportedEncodingException {
    verifyRequestDoneToTokenUrlForClientCredentialsThroughProxy();
  }

}
