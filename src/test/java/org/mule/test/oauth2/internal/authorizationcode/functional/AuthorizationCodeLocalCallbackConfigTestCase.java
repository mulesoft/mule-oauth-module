/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.oauth2.internal.authorizationcode.functional;

import static org.mule.service.oauth.internal.OAuthConstants.CODE_PARAMETER;

import org.mule.test.runner.RunnerDelegateTo;

import org.apache.http.client.fluent.Request;
import org.junit.Test;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunnerDelegateTo(Parameterized.class)
public class AuthorizationCodeLocalCallbackConfigTestCase extends AbstractAuthorizationCodeBasicTestCase {

  private String baseConfig;

  public AuthorizationCodeLocalCallbackConfigTestCase(String baseConfig) {
    this.baseConfig = baseConfig;
  }

  @Parameters
  public static String[] params() {
    return new String[] {
        "authorization-code/authorization-code-localcallbackref-config.xml",
        // TODO MULE-14827 Uncomment this scenario once min mule version is above 4.1.2
        // "authorization-code/authorization-code-localcallbackref-inverse-order-config.xml"
    };
  }

  @Override
  protected String[] getConfigFiles() {
    return new String[] {baseConfig, "operations/operations-config.xml"};
  }

  @Test
  public void hitRedirectUrlAndGetToken() throws Exception {
    configureWireMockToExpectTokenPathRequestForAuthorizationCodeGrantType();

    Request.Get(localCallbackUrl.getValue() + "?" + CODE_PARAMETER + "=" + AUTHENTICATION_CODE)
        .connectTimeout(REQUEST_TIMEOUT).socketTimeout(REQUEST_TIMEOUT).execute();

    verifyRequestDoneToTokenUrlForAuthorizationCode();
    verifyTokenManagerAccessToken();
    verifyTokenManagerRefreshToken();
  }
}
