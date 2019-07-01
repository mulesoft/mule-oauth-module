/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.oauth2.internal.service;

import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.lock.LockFactory;
import org.mule.runtime.oauth.api.builder.OAuthDancerBuilder;
import org.mule.runtime.oauth.api.state.DefaultResourceOwnerOAuthContext;
import org.mule.runtime.oauth.api.state.ResourceOwnerOAuthContext;
import org.mule.runtime.oauth.api.state.ResourceOwnerOAuthContextWithRefreshState;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.locks.Lock;

/**
 * This provides the adapter layer required for newer versions of the module to work on older versions of the runtime/OAuth
 * service.
 */
public final class OAuthContextServiceAdapter {

  private static Method createRefreshUserOAuthContextLock = null;
  private static Method getRefreshUserOAuthContextLock = null;
  private static Method dancerName = null;

  static {
    try {
      getRefreshUserOAuthContextLock = ResourceOwnerOAuthContextWithRefreshState.class
          .getDeclaredMethod("getRefreshOAuthContextLock", String.class, LockFactory.class);

      createRefreshUserOAuthContextLock = ResourceOwnerOAuthContextWithRefreshState.class
          .getDeclaredMethod("createRefreshOAuthContextLock", String.class, LockFactory.class, String.class);

      dancerName = OAuthDancerBuilder.class.getDeclaredMethod("name", String.class);
    } catch (NoSuchMethodException e) {
      // Nothing to do, this is just using an older version of the api
    } catch (SecurityException e) {
      throw e;
    }
  }

  private OAuthContextServiceAdapter() {
    // Nothing to do
  }

  public static Lock getRefreshUserOAuthContextLock(ResourceOwnerOAuthContext resourceOwnerOAuthContext, String name,
                                                    LockFactory lockFactory) {
    if (getRefreshUserOAuthContextLock != null) {
      try {
        return (Lock) getRefreshUserOAuthContextLock.invoke(resourceOwnerOAuthContext, name, lockFactory);
      } catch (IllegalAccessException | IllegalArgumentException e) {
        throw new MuleRuntimeException(e);
      } catch (InvocationTargetException e) {
        throw new MuleRuntimeException(e.getCause());
      }
    } else {
      return ((DefaultResourceOwnerOAuthContext) resourceOwnerOAuthContext).getRefreshUserOAuthContextLock();
    }
  }

  public static Lock createRefreshUserOAuthContextLock(String lockNamePrefix, LockFactory lockProvider, String resourceOwnerId) {
    if (createRefreshUserOAuthContextLock != null) {
      try {
        return (Lock) createRefreshUserOAuthContextLock.invoke(null, lockNamePrefix, lockProvider, resourceOwnerId);
      } catch (IllegalAccessException | IllegalArgumentException e) {
        throw new MuleRuntimeException(e);
      } catch (InvocationTargetException e) {
        throw new MuleRuntimeException(e.getCause());
      }
    } else {
      return lockProvider.createLock(lockNamePrefix + "-config-oauth-context");
    }
  }

  public static <T> OAuthDancerBuilder<T> dancerName(OAuthDancerBuilder<T> dancerBuilder, String name) {
    if (dancerName != null) {
      try {
        return (OAuthDancerBuilder<T>) dancerName.invoke(dancerBuilder, name);
      } catch (IllegalAccessException | IllegalArgumentException e) {
        throw new MuleRuntimeException(e);
      } catch (InvocationTargetException e) {
        throw new MuleRuntimeException(e.getCause());
      }
    } else {
      return dancerBuilder;
    }
  }
}
