/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.oauth2.internal;

import static java.util.Collections.emptyList;
import static java.util.Objects.hash;
import static java.util.stream.Collectors.toMap;
import static org.mule.extension.http.internal.HttpConnectorConstants.TLS_CONFIGURATION;
import static org.mule.extension.oauth2.internal.OAuthUtils.literalEquals;
import static org.mule.extension.oauth2.internal.OAuthUtils.literalHashCodes;
import static org.mule.extension.oauth2.internal.service.OAuthContextServiceAdapter.dancerName;
import static org.mule.runtime.api.meta.ExpressionSupport.NOT_SUPPORTED;
import static org.mule.runtime.core.api.el.ExpressionManager.DEFAULT_EXPRESSION_PREFIX;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.disposeIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.startIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.stopIfNeeded;
import static org.mule.runtime.extension.api.annotation.param.display.Placement.SECURITY_TAB;
import static org.slf4j.LoggerFactory.getLogger;

import org.mule.extension.http.api.request.authentication.HttpRequestAuthentication;
import org.mule.extension.http.api.request.proxy.HttpProxyConfig;
import org.mule.extension.oauth2.api.tokenmanager.TokenManagerConfig;
import org.mule.runtime.api.el.MuleExpressionLanguage;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.lifecycle.Lifecycle;
import org.mule.runtime.api.lock.LockFactory;
import org.mule.runtime.api.tls.TlsContextFactory;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.extension.api.annotation.Expression;
import org.mule.runtime.extension.api.annotation.param.DefaultEncoding;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.display.DisplayName;
import org.mule.runtime.extension.api.annotation.param.display.Placement;
import org.mule.runtime.extension.api.runtime.parameter.Literal;
import org.mule.runtime.oauth.api.OAuthService;
import org.mule.runtime.oauth.api.builder.OAuthDancerBuilder;

import java.nio.charset.Charset;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

import javax.inject.Inject;

import org.slf4j.Logger;

/**
 * Common interface for all grant types must extend this interface.
 *
 * @since 1.0
 */
public abstract class AbstractGrantType implements HttpRequestAuthentication, Lifecycle {

  private final AtomicInteger initializations = new AtomicInteger();
  private final AtomicInteger starts = new AtomicInteger();

  private static final Logger LOGGER = getLogger(AbstractGrantType.class);

  // Expressions to extract parameters from standard token url response.
  private static final String ACCESS_TOKEN_EXPRESSION = "#[payload.access_token]";
  private static final String REFRESH_TOKEN_EXPRESSION = "#[payload.refresh_token]";
  private static final String EXPIRATION_TIME_EXPRESSION = "#[payload.expires_in]";
  private static final String PAYLOAD = "payload";

  @Inject
  protected MuleContext muleContext;

  @Inject
  protected LockFactory lockFactory;

  @Inject
  protected MuleExpressionLanguage expressionEvaluator;

  protected DeferredExpressionResolver resolver;

  /**
   * Application identifier as defined in the oauth authentication server.
   */
  @Parameter
  private String clientId;

  /**
   * Application secret as defined in the oauth authentication server.
   */
  @Parameter
  private String clientSecret;

  /**
   * Scope required by this application to execute. Scopes define permissions over resources.
   */
  @Parameter
  @Optional
  private String scopes;

  /**
   * The token manager configuration to use for this grant type.
   */
  @Parameter
  @Optional
  @Expression(value = NOT_SUPPORTED)
  protected TokenManagerConfig tokenManager;

  /**
   * The oauth authentication server url to get access to the token. Mule, after receiving the authentication code from the oauth
   * server (through the redirectUrl) will call this url to get the access token.
   */
  @Parameter
  private String tokenUrl;

  /**
   * Expression to extract the access token parameter from the response of the call to tokenUrl.
   */
  @Parameter
  @Optional(defaultValue = ACCESS_TOKEN_EXPRESSION)
  protected Literal<String> responseAccessToken;

  @Parameter
  @Optional(defaultValue = REFRESH_TOKEN_EXPRESSION)
  protected Literal<String> responseRefreshToken;

  /**
   * Expression to extract the expiresIn parameter from the response of the call to tokenUrl.
   */
  @Parameter
  @Optional(defaultValue = EXPIRATION_TIME_EXPRESSION)
  protected Literal<String> responseExpiresIn;

  @Parameter
  @Optional
  protected List<ParameterExtractor> customParameterExtractors;

  /**
   * After executing an API call authenticated with OAuth it may be that the access token used was expired, so this attribute
   * allows for an expressions that will be evaluated against the http response of the API callback to determine if the request
   * failed because it was done using an expired token. In case the evaluation returns true (access token expired) then mule will
   * automatically trigger a refresh token flow and retry the API callback using a new access token. Default value evaluates if
   * the response status code was 401 or 403.
   */
  @Parameter
  @Optional(defaultValue = "#[attributes.statusCode == 401 or attributes.statusCode == 403]")
  private Literal<Boolean> refreshTokenWhen;

  /**
   * References a TLS config that will be used to receive incoming HTTP request and do HTTP request during the OAuth dance.
   */
  @Parameter
  @Optional
  @Expression(NOT_SUPPORTED)
  @DisplayName(TLS_CONFIGURATION)
  @Placement(tab = SECURITY_TAB)
  private TlsContextFactory tlsContext;

  @Parameter
  @Optional
  @Expression(NOT_SUPPORTED)
  private HttpProxyConfig proxyConfig;

  @DefaultEncoding
  private String encoding;

  @Inject
  protected OAuthService oAuthService;

  // This is used to detect that two different grant types with
  // the default token manager are recognized as equals even if
  // not initialized.
  private boolean defaultTokenManager;
  private boolean readsResponseBody;

  protected void initTokenManager() throws InitialisationException {
    if (tokenManager == null) {
      this.tokenManager = TokenManagerConfig.createDefault();
      this.defaultTokenManager = true;
    }
    initialiseIfNeeded(tokenManager, muleContext);
  }

  protected OAuthDancerBuilder configureBaseDancer(OAuthDancerBuilder dancerBuilder) throws InitialisationException {
    TlsContextFactory contextFactory = getTlsContextFactory();
    if (contextFactory != null) {
      initialiseIfNeeded(getTlsContextFactory());
    }
    dancerBuilder.tokenUrl(tokenUrl, contextFactory, proxyConfig);
    dancerBuilder = dancerName(dancerBuilder, tokenManager.getName());
    dancerBuilder
        .scopes(getScopes())
        .encoding(Charset.forName(encoding))
        .responseAccessTokenExpr(resolver.getExpression(getResponseAccessToken()))
        .responseRefreshTokenExpr(resolver.getExpression(getResponseRefreshToken()))
        .responseExpiresInExpr(resolver.getExpression(getResponseExpiresIn()))
        .customParametersExtractorsExprs(getCustomParameterExtractors().stream()
            .collect(toMap(extractor -> extractor.getParamName(),
                           extractor -> resolver.getExpression(extractor.getValue()))));

    return dancerBuilder;
  }

  public abstract Object getDancer();

  @Override
  public final void initialise() throws InitialisationException {
    if (initializations.getAndIncrement() > 0) {
      return;
    }

    this.resolver = new DeferredExpressionResolver(expressionEvaluator);
    readsResponseBody = refreshTokenWhen.getLiteralValue()
        .map(expression -> expression.startsWith(DEFAULT_EXPRESSION_PREFIX) && expression.contains(PAYLOAD))
        .orElse(Boolean.FALSE);
    doInitialize();
  }

  protected void doInitialize() throws InitialisationException {

  }

  @Override
  public final void start() throws MuleException {
    if (starts.getAndIncrement() > 0) {
      return;
    }

    startIfNeeded(tokenManager);
    startIfNeeded(getDancer());
  }

  @Override
  public final void stop() throws MuleException {
    if (starts.decrementAndGet() > 0) {
      return;
    }

    stopIfNeeded(getDancer());
  }

  @Override
  public final void dispose() {
    if (initializations.decrementAndGet() > 0) {
      return;
    }

    disposeIfNeeded(getDancer(), LOGGER);
  }

  /**
   * @param accessToken an oauth access token
   * @return the content of the HTTP authentication header.
   */
  protected String buildAuthorizationHeaderContent(String accessToken) {
    return "Bearer " + accessToken;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof AbstractGrantType) {
      AbstractGrantType other = (AbstractGrantType) obj;
      return Objects.equals(clientId, other.clientId) &&
          Objects.equals(clientSecret, other.clientSecret) &&
          Objects.equals(scopes, other.scopes) &&
          (Objects.equals(tokenManager, other.tokenManager) || (this.isDefaultTokenManager() && other.isDefaultTokenManager())) &&
          Objects.equals(tokenUrl, other.tokenUrl) &&
          literalEquals(responseAccessToken, other.responseAccessToken) &&
          literalEquals(responseRefreshToken, other.responseRefreshToken) &&
          literalEquals(responseExpiresIn, other.responseExpiresIn) &&
          Objects.equals(customParameterExtractors, other.customParameterExtractors) &&
          literalEquals(refreshTokenWhen, other.refreshTokenWhen) &&
          Objects.equals(tlsContext, other.tlsContext) &&
          Objects.equals(proxyConfig, other.proxyConfig);
    }

    return false;
  }

  @Override
  public int hashCode() {
    return 31 * hash(
                     clientId, clientSecret, scopes, tokenManager, tokenUrl, customParameterExtractors, tlsContext, proxyConfig)
        *
        literalHashCodes(responseAccessToken, responseRefreshToken, responseExpiresIn, refreshTokenWhen);
  }

  public String getClientSecret() {
    return clientSecret;
  }

  public String getClientId() {
    return clientId;
  }

  public abstract boolean isEncodeClientCredentialsInBody();

  public String getScopes() {
    return scopes;
  }

  public String getTokenUrl() {
    return tokenUrl;
  }

  public Literal<Boolean> getRefreshTokenWhen() {
    return refreshTokenWhen;
  }

  public Literal<String> getResponseAccessToken() {
    return responseAccessToken;
  }


  public Literal<String> getResponseRefreshToken() {
    return responseRefreshToken;
  }


  public Literal<String> getResponseExpiresIn() {
    return responseExpiresIn;
  }

  public List<ParameterExtractor> getCustomParameterExtractors() {
    return customParameterExtractors != null ? customParameterExtractors : emptyList();
  }

  public TlsContextFactory getTlsContextFactory() {
    return tlsContext;
  }

  public boolean isDefaultTokenManager() {
    return defaultTokenManager || tokenManager == null;
  }

  @Override
  public boolean readsAuthenticatedResponseBody() {
    return readsResponseBody;
  }

}
