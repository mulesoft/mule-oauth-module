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

import org.mule.tck.junit4.rule.SystemProperty;
import org.mule.test.oauth2.AbstractOAuthAuthorizationTestCase;

import java.io.UnsupportedEncodingException;


public class ClientCredentialsProxyTestCase extends AbstractOAuthAuthorizationTestCase {

  @Rule
  public SystemProperty tokenUrl =
      new SystemProperty("token.url", format("http://localhost:%d" + TOKEN_PATH, oauthServerPort.getNumber()));

  @Override
  protected String[] getConfigFiles() {
    return new String[] {"client-credentials/client-credentials-through-proxy-config.xml"};
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
