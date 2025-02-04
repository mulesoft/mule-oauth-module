/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.oauth2.internal.service;

import static java.lang.Class.forName;

import org.mule.runtime.api.lock.LockFactory;
import org.mule.runtime.oauth.api.builder.OAuthDancerBuilder;

import java.util.Map;
import java.util.concurrent.locks.Lock;

/**
 * This provides the adapter layer required for newer versions of the module to work on older versions of the runtime/OAuth
 * service.
 * <p>
 * This must be removed once the {@code minMuleVersion} of this extension is upgraded to 4.5+
 *
 * @since 1.2.0, 1.1.9
 *
 * @deprecated to be removed when {@code minMuleVersion} is upgraded to 4.5.0 or newer.
 */
@Deprecated
public final class OAuthContextServiceAdapter {

  private static final OAuthContextService oAuthContextService;

  // This code uses reflection to detect what version of the OAuth service API is in the runtime.
  // In case the new methods are detected, those are called via reflection. Can't use them directly because this code has to
  // compile against the older version of the service API.
  // In case the new methods are not found, the original logic is executed.
  // TODO: Coverage
  static {
    Class<?> clientApiCtxClass = classForNameOrNull("org.mule.oauth.client.api.state.ResourceOwnerOAuthContext");
    Class<?> runtimeApiCtxClass = classForNameOrNull("org.mule.runtime.oauth.api.state.ResourceOwnerOAuthContext");

    Class<?> clientApiCtxWithStateClass =
        classForNameOrNull("org.mule.oauth.client.api.state.ResourceOwnerOAuthContextWithRefreshState");
    Class<?> runtimeApiCtxWithStateClass =
        classForNameOrNull("org.mule.runtime.oauth.api.state.ResourceOwnerOAuthContextWithRefreshState");

    if (clientApiCtxClass != null && clientApiCtxWithStateClass != null) {
      oAuthContextService = new ClientApiOAuthContextServiceImpl();
    } else if (runtimeApiCtxClass != null && runtimeApiCtxWithStateClass != null) {
      oAuthContextService = new RuntimeApiOAuthContextServiceImpl();
    } else {
      oAuthContextService = new DefaultOAuthContextServiceImpl();
    }
  }

  static Class<?> classForNameOrNull(String className) {
    try {
      return forName(className);
    } catch (ClassNotFoundException e) {
      return null;
    }
  }

  private OAuthContextServiceAdapter() {
    // Nothing to do
  }

  public static Object createResourceOwnerOAuthContext(String resourceOwnerId, String name, LockFactory lockFactory) {
    return oAuthContextService.createResourceOwnerOAuthContext(resourceOwnerId, name, lockFactory);
  }

  static Lock createLockForResourceOwner(String resourceOwnerId, String configName, LockFactory lockFactory) {
    return lockFactory.createLock(configName + "-" + resourceOwnerId);
  }

  public static Object migrateContextIfNeeded(Object resourceOwnerOAuthContext, String name, LockFactory lockFactory) {
    return oAuthContextService.migrateContextIfNeeded(resourceOwnerOAuthContext, name, lockFactory);
  }

  public static Lock getRefreshUserOAuthContextLock(Object resourceOwnerOAuthContext, String name,
                                                    LockFactory lockFactory) {
    return oAuthContextService.getRefreshUserOAuthContextLock(resourceOwnerOAuthContext, name, lockFactory);
  }

  public static Lock createRefreshUserOAuthContextLock(String lockNamePrefix, LockFactory lockProvider, String resourceOwnerId) {
    return oAuthContextService.createRefreshUserOAuthContextLock(lockNamePrefix, lockProvider, resourceOwnerId);
  }

  public static <T> OAuthDancerBuilder<T> dancerName(OAuthDancerBuilder<T> dancerBuilder, String name) {
    return oAuthContextService.dancerName(dancerBuilder, name);
  }

  public static String getResourceOwnerId(Object resourceOwnerOAuthContext) {
    return oAuthContextService.getResourceOwnerId(resourceOwnerOAuthContext);
  }

  public static Map<String, Object> getTokenResponseParameters(Object contextForResourceOwner) {
    return oAuthContextService.getTokenResponseParameters(contextForResourceOwner);
  }

  public static String getAccessToken(Object contextForResourceOwner) {
    return oAuthContextService.getAccessToken(contextForResourceOwner);
  }

  public static String getRefreshToken(Object contextForResourceOwner) {
    return oAuthContextService.getRefreshToken(contextForResourceOwner);
  }

  public static String getExpiresIn(Object contextForResourceOwner) {
    return oAuthContextService.getExpiresIn(contextForResourceOwner);
  }

  public static String getState(Object contextForResourceOwner) {
    return oAuthContextService.getState(contextForResourceOwner);
  }
}
