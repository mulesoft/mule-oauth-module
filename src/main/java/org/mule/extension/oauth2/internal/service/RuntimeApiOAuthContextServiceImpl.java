/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.oauth2.internal.service;

import static org.mule.extension.oauth2.internal.service.OAuthContextServiceAdapter.classForNameOrNull;

import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.lock.LockFactory;
import org.mule.runtime.oauth.api.builder.OAuthDancerBuilder;
import org.mule.runtime.oauth.api.state.ResourceOwnerOAuthContext;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.locks.Lock;

class RuntimeApiOAuthContextServiceImpl implements OAuthContextService {

  private static final Constructor<?> resourceOwnerOAuthContextWithRefreshStateConstructor;
  private static final Method getRefreshOAuthContextLockMethod;
  private static final Method createRefreshOAuthContextLockMethod;
  private static final Method getResourceOwnerIdMethod;
  private static final Method getTokenResponseParametersMethod;
  private static final Method getAccessTokenMethod;
  private static final Method getRefreshTokenMethod;
  private static final Method getExpiresInMethod;
  private static final Method getStateMethod;

  private static final Constructor<?> runtimeApi_ctxWithStateCopyConstructor;

  static {
    try {
      Class<?> resourceOwnerOAuthContextWithRefreshStateClass =
          Class.forName("org.mule.runtime.oauth.api.state.ResourceOwnerOAuthContextWithRefreshState");
      Class<?> runtimeApiCtxClass = classForNameOrNull("org.mule.runtime.oauth.api.state.ResourceOwnerOAuthContext");
      Class<?> runtimeApiCtxWithStateClass =
          classForNameOrNull("org.mule.runtime.oauth.api.state.ResourceOwnerOAuthContextWithRefreshState");

      runtimeApi_ctxWithStateCopyConstructor = runtimeApiCtxWithStateClass.getConstructor(runtimeApiCtxClass);
      resourceOwnerOAuthContextWithRefreshStateConstructor =
          resourceOwnerOAuthContextWithRefreshStateClass.getConstructor(String.class);
      getRefreshOAuthContextLockMethod =
          resourceOwnerOAuthContextWithRefreshStateClass.getMethod("getRefreshOAuthContextLock", String.class, LockFactory.class);
      createRefreshOAuthContextLockMethod = resourceOwnerOAuthContextWithRefreshStateClass
          .getMethod("createRefreshOAuthContextLock", String.class, LockFactory.class, String.class);
      getResourceOwnerIdMethod = resourceOwnerOAuthContextWithRefreshStateClass.getMethod("getResourceOwnerId");
      getTokenResponseParametersMethod = resourceOwnerOAuthContextWithRefreshStateClass.getMethod("getTokenResponseParameters");
      getAccessTokenMethod = resourceOwnerOAuthContextWithRefreshStateClass.getMethod("getAccessToken");
      getRefreshTokenMethod = resourceOwnerOAuthContextWithRefreshStateClass.getMethod("getRefreshToken");
      getExpiresInMethod = resourceOwnerOAuthContextWithRefreshStateClass.getMethod("getExpiresIn");
      getStateMethod = resourceOwnerOAuthContextWithRefreshStateClass.getMethod("getState");
    } catch (ClassNotFoundException | NoSuchMethodException e) {
      throw new MuleRuntimeException(e);
    }
  }

  @Override
  public Object createResourceOwnerOAuthContext(String resourceOwnerId, String name, LockFactory lockFactory) {
    try {
      return resourceOwnerOAuthContextWithRefreshStateConstructor.newInstance(resourceOwnerId);
    } catch (InvocationTargetException | InstantiationException | IllegalAccessException e) {
      throw new MuleRuntimeException(e);
    }
  }

  @Override
  public Object migrateContextIfNeeded(Object resourceOwnerOAuthContext, String name, LockFactory lockFactory) {
    if (resourceOwnerOAuthContext instanceof ResourceOwnerOAuthContext) {
      try {
        return runtimeApi_ctxWithStateCopyConstructor.newInstance(resourceOwnerOAuthContext);
      } catch (Exception e) {
        throw new MuleRuntimeException(e);
      }
    }
    return resourceOwnerOAuthContext;
  }

  @Override
  public Lock getRefreshUserOAuthContextLock(Object resourceOwnerOAuthContext, String name, LockFactory lockFactory) {
    try {
      return (Lock) getRefreshOAuthContextLockMethod.invoke(resourceOwnerOAuthContext, name, lockFactory);
    } catch (Exception e) {
      throw new MuleRuntimeException(e);
    }
  }

  @Override
  public Lock createRefreshUserOAuthContextLock(String lockNamePrefix, LockFactory lockProvider, String resourceOwnerId) {
    try {
      return (Lock) createRefreshOAuthContextLockMethod.invoke(null, lockNamePrefix, lockProvider, resourceOwnerId);
    } catch (InvocationTargetException | IllegalAccessException e) {
      throw new MuleRuntimeException(e);
    }
  }

  @Override
  public <T> OAuthDancerBuilder<T> dancerName(OAuthDancerBuilder<T> dancerBuilder, String name) {
    try {
      Method dancerName = OAuthDancerBuilder.class.getDeclaredMethod("name", String.class);
      return (OAuthDancerBuilder<T>) dancerName.invoke(dancerBuilder, name);
    } catch (IllegalAccessException | IllegalArgumentException | NoSuchMethodException | InvocationTargetException e) {
      throw new MuleRuntimeException(e);
    }
  }

  @Override
  public String getResourceOwnerId(Object resourceOwnerOAuthContext) {
    try {
      return (String) getResourceOwnerIdMethod.invoke(resourceOwnerOAuthContext);
    } catch (InvocationTargetException | IllegalAccessException e) {
      throw new MuleRuntimeException(e);
    }
  }

  @Override
  public Map<String, Object> getTokenResponseParameters(Object contextForResourceOwner) {
    try {
      Object invoke = getTokenResponseParametersMethod.invoke(contextForResourceOwner);
      return (Map<String, Object>) invoke;
    } catch (InvocationTargetException | IllegalAccessException e) {
      throw new MuleRuntimeException(e);
    }
  }

  @Override
  public String getAccessToken(Object contextForResourceOwner) {
    try {
      return (String) getAccessTokenMethod.invoke(contextForResourceOwner);
    } catch (InvocationTargetException | IllegalAccessException e) {
      throw new MuleRuntimeException(e);
    }
  }

  @Override
  public String getRefreshToken(Object contextForResourceOwner) {
    try {
      return (String) getRefreshTokenMethod.invoke(contextForResourceOwner);
    } catch (InvocationTargetException | IllegalAccessException e) {
      throw new MuleRuntimeException(e);
    }
  }

  @Override
  public String getExpiresIn(Object contextForResourceOwner) {
    try {
      return (String) getExpiresInMethod.invoke(contextForResourceOwner);
    } catch (InvocationTargetException | IllegalAccessException e) {
      throw new MuleRuntimeException(e);
    }
  }

  @Override
  public String getState(Object contextForResourceOwner) {
    try {
      return (String) getStateMethod.invoke(contextForResourceOwner);
    } catch (Exception e) {
      throw new MuleRuntimeException(e);
    }
  }
}
