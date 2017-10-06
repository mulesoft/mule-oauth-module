/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.oauth2.internal.authorizationcode.functional;

import static org.mule.service.oauth.internal.OAuthConstants.CODE_PARAMETER;

import org.mule.runtime.http.api.HttpService;
import org.mule.runtime.http.api.server.HttpServerConfiguration;
import org.mule.runtime.http.api.server.ServerCreationException;

import org.apache.http.client.fluent.Request;
import org.junit.Ignore;
import org.junit.Test;

import javax.inject.Inject;

import io.qameta.allure.Issue;

public class AuthorizationCodeMinimalConfigTestCase extends AbstractAuthorizationCodeBasicTestCase {

  @Inject
  private HttpService httpService;

  @Override
  protected String[] getConfigFiles() {
    return new String[] {"authorization-code/authorization-code-minimal-config.xml", "operations/operations-config.xml"};
  }

  @Ignore("MULE-6926: flaky test")
  @Issue("MULE-6926")
  @Test
  public void hitRedirectUrlAndGetToken() throws Exception {
    configureWireMockToExpectTokenPathRequestForAuthorizationCodeGrantType();

    Request.Get(localCallbackUrl.getValue() + "?" + CODE_PARAMETER + "=" + AUTHENTICATION_CODE)
        .connectTimeout(REQUEST_TIMEOUT).socketTimeout(REQUEST_TIMEOUT).execute();

    verifyRequestDoneToTokenUrlForAuthorizationCode();

    verifyTokenManagerAccessToken();
    verifyTokenManagerRefreshToken();
  }

  /**
   * Expect a failure since the application created a listener in the port
   */
  @Test(expected = ServerCreationException.class)
  public void listenerCreated() throws ServerCreationException {
    httpService.getServerFactory()
        .create(new HttpServerConfiguration.Builder().setHost("localhost").setPort(localHostPort.getNumber()).setName("test")
            .build());
  }
}
