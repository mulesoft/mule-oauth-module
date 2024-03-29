/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.oauth2.api.clientcredentials;

import static org.mule.extension.oauth2.api.exception.OAuthClientErrors.TOKEN_URL_FAIL;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static org.mule.runtime.http.api.HttpHeaders.Names.AUTHORIZATION;
import static java.lang.Thread.currentThread;
import static java.util.Objects.hash;

import org.mule.api.annotation.NoExtend;
import org.mule.api.annotation.NoInstantiate;
import org.mule.extension.http.api.HttpResponseAttributes;
import org.mule.extension.oauth2.internal.AbstractGrantType;
import org.mule.extension.oauth2.internal.store.SimpleObjectStoreToMapAdapter;
import org.mule.runtime.api.exception.DefaultMuleException;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.exception.ModuleException;
import org.mule.runtime.extension.api.runtime.operation.Result;
import org.mule.runtime.http.api.domain.message.request.HttpRequestBuilder;
import org.mule.runtime.oauth.api.ClientCredentialsOAuthDancer;
import org.mule.runtime.oauth.api.builder.OAuthClientCredentialsDancerBuilder;

import java.util.concurrent.ExecutionException;

/**
 * Authorization element for client credentials oauth grant type
 */
@NoExtend
@NoInstantiate
public class ClientCredentialsGrantType extends AbstractGrantType {

  /**
   * If true, the client id and client secret will be sent in the request body. Otherwise, they will be sent as basic
   * authentication.
   */
  @Parameter
  @Optional(defaultValue = "false")
  private boolean encodeClientCredentialsInBody;

  private ClientCredentialsOAuthDancer dancer;

  @Override
  public final void doInitialize() throws InitialisationException {
    initTokenManager();

    OAuthClientCredentialsDancerBuilder dancerBuilder =
        oAuthService.clientCredentialsGrantTypeDancerBuilder(lockFactory,
                                                             new SimpleObjectStoreToMapAdapter(tokenManager
                                                                 .getResolvedObjectStore()),
                                                             expressionEvaluator);
    dancerBuilder.encodeClientCredentialsInBody(isEncodeClientCredentialsInBody());
    dancerBuilder.clientCredentials(getClientId(), getClientSecret());

    configureBaseDancer(dancerBuilder);
    dancer = dancerBuilder.build();
    initialiseIfNeeded(getDancer());
  }

  @Override
  public void authenticate(HttpRequestBuilder builder) throws MuleException {
    try {
      builder.addHeader(AUTHORIZATION, buildAuthorizationHeaderContent(dancer.accessToken().get()));
    } catch (InterruptedException e) {
      currentThread().interrupt();
      throw new DefaultMuleException(e);
    } catch (ExecutionException e) {
      throw new DefaultMuleException(e.getCause());
    }
  }

  @Override
  public boolean shouldRetry(final Result<Object, HttpResponseAttributes> firstAttemptResult) throws MuleException {
    final Boolean shouldRetryRequest = resolver.resolveExpression(getRefreshTokenWhen(), firstAttemptResult);
    if (shouldRetryRequest) {
      try {
        dancer.refreshToken().get();
      } catch (InterruptedException e) {
        currentThread().interrupt();
        throw new DefaultMuleException(e);
      } catch (ExecutionException e) {
        throw new DefaultMuleException(e.getCause());
      }
    }
    return shouldRetryRequest;
  }

  @Override
  public void retryIfShould(Result<Object, HttpResponseAttributes> firstAttemptResult, Runnable retryCallback,
                            Runnable notRetryCallback)
      throws ModuleException {

    Boolean isUnauthorized = firstAttemptResult.getAttributes().get().getStatusCode() == 401;
    Boolean shouldRetryRequest = resolver.resolveExpression(getRefreshTokenWhen(), firstAttemptResult);

    if (shouldRetryRequest) {
      try {
        dancer.refreshToken().get();
        retryCallback.run();
      } catch (Exception ex) {
        throw new ModuleException(TOKEN_URL_FAIL, ex.getCause());
      }
    } else if (isUnauthorized) {
      try {
        dancer.refreshToken().get();
      } catch (Exception ex) {
        throw new ModuleException(TOKEN_URL_FAIL, ex.getCause());
      }
    } else {
      notRetryCallback.run();
    }
  }


  @Override
  public boolean equals(Object obj) {
    if (obj instanceof ClientCredentialsGrantType) {
      ClientCredentialsGrantType other = (ClientCredentialsGrantType) obj;
      return encodeClientCredentialsInBody == other.encodeClientCredentialsInBody && super.equals(obj);
    }

    return false;
  }

  @Override
  public int hashCode() {
    return hash(super.hashCode(), encodeClientCredentialsInBody);
  }

  @Override
  public ClientCredentialsOAuthDancer getDancer() {
    return dancer;
  }

  @Override
  public boolean isEncodeClientCredentialsInBody() {
    return encodeClientCredentialsInBody;
  }

  public void setEncodeClientCredentialsInBody(boolean encodeClientCredentialsInBody) {
    this.encodeClientCredentialsInBody = encodeClientCredentialsInBody;
  }

  public ClientCredentialsGrantType() {}
}
