/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.oauth2.asserter;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mule.runtime.http.api.HttpConstants.Method.GET;
import static org.mule.runtime.http.api.utils.HttpEncoderDecoderUtils.decodeQueryString;
import static org.mule.runtime.http.api.utils.HttpEncoderDecoderUtils.extractQueryParams;
import static org.mule.service.oauth.internal.OAuthConstants.CODE_PARAMETER;

import org.mule.runtime.api.util.MultiMap;

import com.github.tomakehurst.wiremock.verification.LoggedRequest;

public class AuthorizationRequestAsserter {

  private final LoggedRequest loggedRequest;
  private final MultiMap<String, String> queryParameters;

  public static AuthorizationRequestAsserter create(LoggedRequest loggedRequest) {
    return new AuthorizationRequestAsserter(loggedRequest);
  }

  private AuthorizationRequestAsserter(LoggedRequest loggedRequest) {
    this.loggedRequest = loggedRequest;
    queryParameters = decodeQueryString(extractQueryParams(loggedRequest.getUrl()));
  }

  public AuthorizationRequestAsserter assertMethodIsGet() {
    assertThat(loggedRequest.getMethod().value(), is(GET.name()));
    return this;
  }

  public AuthorizationRequestAsserter assertClientIdIs(String expectedClientId) {
    assertThat(queryParameters.get("client_id"), is(expectedClientId));
    return this;
  }

  public AuthorizationRequestAsserter assertRedirectUriIs(String redirectUri) {
    assertThat(queryParameters.get("redirect_uri"), is(redirectUri));
    return this;
  }

  public AuthorizationRequestAsserter assertResponseTypeIsCode() {
    assertThat(queryParameters.get("response_type"), is(CODE_PARAMETER));
    return this;
  }

  public AuthorizationRequestAsserter assertScopeIs(String expectedScope) {
    assertThat(queryParameters.get("scope"), is(expectedScope));
    return this;
  }

  public AuthorizationRequestAsserter assertStateIs(String expectedState) {
    assertThat(queryParameters.get("state"), is(expectedState));
    return this;
  }

  public AuthorizationRequestAsserter assertContainsCustomParameter(String paramName, String paramValue) {
    assertThat(queryParameters.get(paramName), is(paramValue));
    return this;
  }
}
