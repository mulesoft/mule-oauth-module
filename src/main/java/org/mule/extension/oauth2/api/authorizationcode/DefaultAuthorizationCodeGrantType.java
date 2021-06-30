/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.oauth2.api.authorizationcode;

import static java.lang.Thread.currentThread;
import static java.util.Objects.hash;
import static org.mule.extension.oauth2.internal.OAuthUtils.literalEquals;
import static org.mule.extension.oauth2.internal.OAuthUtils.literalHashCodes;
import static org.mule.extension.oauth2.internal.OAuthUtils.resolverEquals;
import static org.mule.extension.oauth2.internal.OAuthUtils.resolverHashCode;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static org.mule.runtime.http.api.HttpHeaders.Names.AUTHORIZATION;
import static org.mule.runtime.oauth.api.state.ResourceOwnerOAuthContext.DEFAULT_RESOURCE_OWNER_ID;
import static org.slf4j.LoggerFactory.getLogger;

import org.mule.api.annotation.NoExtend;
import org.mule.api.annotation.NoInstantiate;
import org.mule.extension.http.api.HttpResponseAttributes;
import org.mule.extension.oauth2.internal.AbstractGrantType;
import org.mule.extension.oauth2.internal.authorizationcode.state.ConfigOAuthContext;
import org.mule.extension.oauth2.internal.store.SimpleObjectStoreToMapAdapter;
import org.mule.runtime.api.exception.DefaultMuleException;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.reference.ConfigReference;
import org.mule.runtime.extension.api.runtime.operation.Result;
import org.mule.runtime.extension.api.runtime.parameter.Literal;
import org.mule.runtime.extension.api.runtime.parameter.ParameterResolver;
import org.mule.runtime.http.api.HttpService;
import org.mule.runtime.http.api.domain.message.request.HttpRequestBuilder;
import org.mule.runtime.http.api.server.HttpServer;
import org.mule.runtime.http.api.server.ServerNotFoundException;
import org.mule.runtime.oauth.api.AuthorizationCodeOAuthDancer;
import org.mule.runtime.oauth.api.OAuthService;
import org.mule.runtime.oauth.api.builder.OAuthAuthorizationCodeDancerBuilder;

import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutionException;

import javax.inject.Inject;

import org.slf4j.Logger;

/**
 * Represents the config element for {@code oauth:authentication-code-config}.
 * <p>
 * This config will: - If the authorization-request is defined then it will create a flow listening for an user call to begin the
 * oauth login. - If the token-request is defined then it will create a flow for listening in the redirect uri so we can get the
 * authentication code and retrieve the access token
 */
@Alias("authorization-code-grant-type")
@NoExtend
@NoInstantiate
public class DefaultAuthorizationCodeGrantType extends AbstractGrantType {

  private static final Logger LOGGER = getLogger(DefaultAuthorizationCodeGrantType.class);

  /**
   * Listener configuration to be used instead of localCallbackUrl. Note that if using this you must also provide a
   * localCallbackConfigPath separately.
   */
  @Parameter
  @Optional
  @ConfigReference(namespace = "HTTP", name = "LISTENER_CONFIG")
  private String localCallbackConfig;

  /**
   * Local path for the listener that will be created according to localCallbackConfig, not required if using localCallbackUrl.
   */
  @Parameter
  @Optional
  private String localCallbackConfigPath;

  /**
   * If this attribute is provided mule will automatically create an endpoint in this url to be able to store the authentication
   * code unless there's already an endpoint registered to manually extract the authorization code.
   */
  @Parameter
  @Optional
  private String localCallbackUrl;

  /**
   * The oauth authentication server will use this url to provide the authentication code to the Mule server so the mule server
   * can retrieve the access token.
   * <p>
   * Note that this must be the externally visible address of the callback, not the local one.
   */
  @Parameter
  private String externalCallbackUrl;

  /**
   * State parameter for holding state between the authentication request and the callback done by the oauth authorization server
   * to the redirectUrl.
   */
  @Parameter
  @Optional
  private Literal<String> state;

  /**
   * Identifier under which the oauth authentication attributes are stored (accessToken, refreshToken, etc).
   * <p>
   * This attribute is only required when the applications needs to access resources from more than one user in the OAuth
   * authentication server.
   */
  @Parameter
  @Optional
  private Literal<String> localAuthorizationUrlResourceOwnerId;

  /**
   * If this attribute is provided mule will automatically create and endpoint in the host server that the user can hit to
   * authenticate and grant access to the application for his account.
   */
  @Parameter
  private String localAuthorizationUrl;

  /**
   * The oauth authentication server url to authorize the app for a certain user.
   */
  @Parameter
  private String authorizationUrl;

  /**
   * Custom parameters to send to the authorization request url or the oauth authorization sever.
   */
  @Parameter
  @Optional
  @Alias("custom-parameters")
  private final Map<String, String> customParameters = new HashMap<>();

  /**
   * Identifier under which the oauth authentication attributes are stored (accessToken, refreshToken, etc).
   * <p>
   * This attribute is only required when the applications needs to access resources from more than one user in the OAuth
   * authentication server.
   */
  @Parameter
  @Optional(defaultValue = DEFAULT_RESOURCE_OWNER_ID)
  private ParameterResolver<String> resourceOwnerId;

  /**
   * If true, the client id and client secret will be sent in the request body. Otherwise, they will be sent as basic
   * authentication.
   */
  @Parameter
  @Optional(defaultValue = "true")
  private boolean encodeClientCredentialsInBody;

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof DefaultAuthorizationCodeGrantType) {
      DefaultAuthorizationCodeGrantType other = (DefaultAuthorizationCodeGrantType) obj;
      return super.equals(other) &&
          Objects.equals(localCallbackConfig, other.localCallbackConfig) &&
          Objects.equals(localCallbackConfigPath, other.localCallbackConfigPath) &&
          Objects.equals(localCallbackUrl, other.localCallbackUrl) &&
          Objects.equals(externalCallbackUrl, other.externalCallbackUrl) &&
          literalEquals(state, other.state) &&
          literalEquals(localAuthorizationUrlResourceOwnerId, other.localAuthorizationUrlResourceOwnerId) &&
          Objects.equals(localAuthorizationUrl, other.localAuthorizationUrl) &&
          Objects.equals(authorizationUrl, other.authorizationUrl) &&
          Objects.equals(customParameters, other.customParameters) &&
          resolverEquals(resourceOwnerId, other.resourceOwnerId) &&
          encodeClientCredentialsInBody == other.encodeClientCredentialsInBody;
    }

    return false;
  }

  @Override
  public int hashCode() {
    return hash(localCallbackConfig, localCallbackConfigPath, localCallbackUrl, externalCallbackUrl, localAuthorizationUrl,
                authorizationUrl, customParameters, encodeClientCredentialsInBody,
                literalHashCodes(state, localAuthorizationUrlResourceOwnerId), resolverHashCode(resourceOwnerId),
                super.hashCode());
  }

  @Inject
  private HttpService httpService;

  private AuthorizationCodeOAuthDancer dancer;

  public String getLocalCallbackConfig() {
    return localCallbackConfig;
  }

  public String getLocalCallbackConfigPath() {
    return localCallbackConfigPath;
  }

  public String getLocalCallbackUrl() {
    return localCallbackUrl;
  }

  public String getExternalCallbackUrl() {
    return externalCallbackUrl;
  }

  public ConfigOAuthContext getUserOAuthContext() {
    return tokenManager.getConfigOAuthContext();
  }

  @Override
  public final void doInitialize() throws InitialisationException {
    initTokenManager();

    OAuthAuthorizationCodeDancerBuilder dancerBuilder = configDancer(oAuthService);

    if (!isEncodeClientCredentialsInBody()) {
      // This dirty reflection hack is in place so that the minMuleVersion of the plugin doesn't have to be increased.
      // If at some point, the minMuleVersion of this OAuth module is increased to 4.2.0 or 4.1.4, this may be removed and the
      // encodeClientCredentialsInBody called directly.

      try {
        OAuthAuthorizationCodeDancerBuilder.class.getDeclaredMethod("encodeClientCredentialsInBody", boolean.class)
            .invoke(dancerBuilder, isEncodeClientCredentialsInBody());
      } catch (NoSuchMethodException e) {
        LOGGER.warn("`encodeClientCredentialsInBody` method not found in dancer builder but configured in authenticator."
            + " The configured value will be ignored. Check the version of the Mule Runtime.");
      } catch (SecurityException | IllegalAccessException | IllegalArgumentException
          | InvocationTargetException e) {
        throw new InitialisationException(e, this);
      }
    }

    dancerBuilder.clientCredentials(getClientId(), getClientSecret());

    configureBaseDancer(dancerBuilder);
    dancer = dancerBuilder.build();
    initialiseIfNeeded(getDancer());
  }

  private OAuthAuthorizationCodeDancerBuilder configDancer(OAuthService oauthService) throws InitialisationException {
    OAuthAuthorizationCodeDancerBuilder dancerBuilder =
        oauthService.authorizationCodeGrantTypeDancerBuilder(lockFactory,
                                                             new SimpleObjectStoreToMapAdapter(tokenManager
                                                                 .getResolvedObjectStore()),
                                                             expressionEvaluator);
    try {
      if (localCallbackConfig != null && localCallbackUrl != null) {
        throw new IllegalArgumentException("Attributes localCallbackConfig and localCallbackUrl are mutually exclusive");
      }
      if ((localCallbackConfig == null) != (localCallbackConfigPath == null)) {
        throw new IllegalArgumentException(
                                           "Attributes localCallbackConfig and localCallbackConfigPath must be both present or absent");
      }
    } catch (Exception e) {
      throw new InitialisationException(e, this);
    }

    try {
      if (localCallbackUrl != null) {
        if (!localCallbackUrl.startsWith("urn:")) {
          if (getTlsContextFactory() != null) {
            dancerBuilder = dancerBuilder.localCallback(new URL(localCallbackUrl), getTlsContextFactory());
          } else {
            dancerBuilder = dancerBuilder.localCallback(new URL(localCallbackUrl));
          }
        }
      } else if (localCallbackConfig != null) {
        HttpServer server = httpService.getServerFactory().lookup(localCallbackConfig);

        dancerBuilder = dancerBuilder.localCallback(server, localCallbackConfigPath);
      }
      dancerBuilder = dancerBuilder
          .localAuthorizationUrlPath(new URL(localAuthorizationUrl).getPath())
          .localAuthorizationUrlResourceOwnerId(resolver.getExpression(localAuthorizationUrlResourceOwnerId))
          .customParameters(customParameters)
          .state(resolver.getExpression(state))
          .authorizationUrl(authorizationUrl)
          .externalCallbackUrl(externalCallbackUrl);
      return dancerBuilder;
    } catch (MalformedURLException | ServerNotFoundException e) {
      throw new InitialisationException(e, this);
    }
  }

  @Override
  public void authenticate(HttpRequestBuilder builder) throws MuleException {
    try {
      builder.addHeader(AUTHORIZATION, buildAuthorizationHeaderContent(dancer.accessToken(resourceOwnerId.resolve()).get()));
    } catch (InterruptedException e) {
      currentThread().interrupt();
      throw new DefaultMuleException(e);
    } catch (ExecutionException e) {
      throw new DefaultMuleException(e.getCause());
    }
  }

  @Override
  public boolean shouldRetry(final Result<Object, HttpResponseAttributes> firstAttemptResult) throws MuleException {
    Boolean shouldRetryRequest = resolver.resolveExpression(getRefreshTokenWhen(), firstAttemptResult);
    if (shouldRetryRequest) {
      try {
        dancer.refreshToken(resourceOwnerId.resolve()).get();
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
                            Runnable notRetryCallback) {
    Boolean shouldRetryRequest = resolver.resolveExpression(getRefreshTokenWhen(), firstAttemptResult);
    if (shouldRetryRequest) {
      dancer.refreshToken(resourceOwnerId.resolve()).thenRun(retryCallback);
    } else {
      notRetryCallback.run();
    }
  }

  @Override
  public AuthorizationCodeOAuthDancer getDancer() {
    return dancer;
  }

  @Override
  public boolean isEncodeClientCredentialsInBody() {
    return encodeClientCredentialsInBody;
  }
}
