/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.oauth2.internal.authorizationcode.state;

import static org.mule.extension.oauth2.internal.service.OAuthContextServiceAdapter.createRefreshUserOAuthContextLock;
import static org.mule.extension.oauth2.internal.service.OAuthContextServiceAdapter.createResourceOwnerOAuthContext;
import static org.mule.extension.oauth2.internal.service.OAuthContextServiceAdapter.getRefreshUserOAuthContextLock;
import static org.mule.extension.oauth2.internal.service.OAuthContextServiceAdapter.getResourceOwnerId;
import static org.mule.extension.oauth2.internal.service.OAuthContextServiceAdapter.migrateContextIfNeeded;

import org.mule.runtime.api.lock.LockFactory;

import java.util.Map;
import java.util.concurrent.locks.Lock;

/**
 * Provides the OAuth context for a particular config
 */
public class ConfigOAuthContext {

  static {
    System.out.println("initializing ConfigOAuthContext class");
  }

  private final LockFactory lockFactory;
  private final String configName;
  private final Map<String, Object> oauthContextStore;

  public ConfigOAuthContext(final LockFactory lockFactory, Map<String, Object> objectStore,
                            final String configName) {
    this.lockFactory = lockFactory;
    this.oauthContextStore = objectStore;
    this.configName = configName;
  }

  /**
   * Retrieves the oauth context for a particular user. If there's no state for that user a new state is retrieve so never returns
   * null.
   *
   * @param resourceOwnerId id of the user.
   * @return oauth state
   */
  public Object getContextForResourceOwner(final String resourceOwnerId) {
    Object resourceOwnerOAuthContext = null;
    if (!oauthContextStore.containsKey(resourceOwnerId)) {
      final Lock lock = createRefreshUserOAuthContextLock(configName, lockFactory, resourceOwnerId);
      lock.lock();
      try {
        if (!oauthContextStore.containsKey(resourceOwnerId)) {
          resourceOwnerOAuthContext = createResourceOwnerOAuthContext(resourceOwnerId, configName, lockFactory);
          oauthContextStore.put(resourceOwnerId, resourceOwnerOAuthContext);
        }
      } finally {
        lock.unlock();
      }
    }
    if (resourceOwnerOAuthContext == null) {
      resourceOwnerOAuthContext = migrateContextIfNeeded(oauthContextStore.get(resourceOwnerId), configName, lockFactory);
    }
    return resourceOwnerOAuthContext;
  }

  /**
   * Updates the resource owner oauth context information
   *
   * @param resourceOwnerOAuthContext
   */
  public void updateResourceOwnerOAuthContext(Object resourceOwnerOAuthContext) {
    final Lock resourceOwnerContextLock =
        getRefreshUserOAuthContextLock(resourceOwnerOAuthContext, configName, lockFactory);
    resourceOwnerContextLock.lock();
    try {
      oauthContextStore.put(getResourceOwnerId(resourceOwnerOAuthContext), resourceOwnerOAuthContext);
    } finally {
      resourceOwnerContextLock.unlock();
    }
  }

  public void clearContextForResourceOwner(String resourceOwnerId) {
    final Object resourceOwnerOAuthContext = getContextForResourceOwner(resourceOwnerId);

    if (resourceOwnerOAuthContext != null) {
      final Lock resourceOwnerContextLock = getRefreshUserOAuthContextLock(resourceOwnerOAuthContext, configName, lockFactory);
      resourceOwnerContextLock.lock();
      try {
        oauthContextStore.remove(resourceOwnerId);
      } finally {
        resourceOwnerContextLock.unlock();
      }
    }
  }
}
