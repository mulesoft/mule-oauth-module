/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.oauth2.internal.authorizationcode.state;

import static org.mule.extension.oauth2.internal.service.OAuthContextServiceAdapter.createRefreshUserOAuthContextLock;
import static org.mule.extension.oauth2.internal.service.OAuthContextServiceAdapter.createResourceOwnerOAuthContext;
import static org.mule.extension.oauth2.internal.service.OAuthContextServiceAdapter.getRefreshUserOAuthContextLock;
import static org.mule.extension.oauth2.internal.service.OAuthContextServiceAdapter.migrateContextIfNeeded;

import org.mule.runtime.api.lock.LockFactory;
import org.mule.runtime.oauth.api.state.ResourceOwnerOAuthContext;

import java.util.Map;
import java.util.concurrent.locks.Lock;

/**
 * Provides the OAuth context for a particular config
 */
public class ConfigOAuthContext {

  private final LockFactory lockFactory;
  private final String configName;
  private final Map<String, ResourceOwnerOAuthContext> oauthContextStore;

  public ConfigOAuthContext(final LockFactory lockFactory, Map<String, ResourceOwnerOAuthContext> objectStore,
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
  public ResourceOwnerOAuthContext getContextForResourceOwner(final String resourceOwnerId) {
    ResourceOwnerOAuthContext resourceOwnerOAuthContext = null;
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
  public void updateResourceOwnerOAuthContext(ResourceOwnerOAuthContext resourceOwnerOAuthContext) {
    final Lock resourceOwnerContextLock =
        getRefreshUserOAuthContextLock(resourceOwnerOAuthContext, configName, lockFactory);
    resourceOwnerContextLock.lock();
    try {
      oauthContextStore.put(resourceOwnerOAuthContext.getResourceOwnerId(), resourceOwnerOAuthContext);
    } finally {
      resourceOwnerContextLock.unlock();
    }
  }

  public void clearContextForResourceOwner(String resourceOwnerId) {
    final ResourceOwnerOAuthContext resourceOwnerOAuthContext = getContextForResourceOwner(resourceOwnerId);

    if (resourceOwnerOAuthContext != null) {
      final Lock resourceOwnerContextLock =
          getRefreshUserOAuthContextLock(resourceOwnerOAuthContext, configName, lockFactory);
      resourceOwnerContextLock.lock();
      try {
        oauthContextStore.remove(resourceOwnerId);
      } finally {
        resourceOwnerContextLock.unlock();
      }
    }
  }
}
