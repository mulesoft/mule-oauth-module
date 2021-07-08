/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.oauth2.api.tokenmanager;

import static java.util.Objects.hash;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.startIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.stopIfNeeded;

import org.mule.api.annotation.NoExtend;
import org.mule.api.annotation.NoInstantiate;
import org.mule.extension.oauth2.internal.authorizationcode.state.ConfigOAuthContext;
import org.mule.extension.oauth2.internal.store.SimpleObjectStoreToMapAdapter;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.lifecycle.Lifecycle;
import org.mule.runtime.api.lock.LockFactory;
import org.mule.runtime.api.store.ObjectStore;
import org.mule.runtime.api.store.ObjectStoreManager;
import org.mule.runtime.api.store.ObjectStoreSettings;
import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.dsl.xml.TypeDsl;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.RefName;
import org.mule.runtime.extension.api.annotation.param.reference.ObjectStoreReference;
import org.mule.runtime.oauth.api.state.ResourceOwnerOAuthContext;

import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import javax.inject.Inject;

/**
 * Token manager stores all the OAuth State (access token, refresh token).
 *
 * It can be referenced to access the state inside a flow for custom processing of oauth dance content.
 */
@Alias("token-manager-config")
@TypeDsl(allowTopLevelDefinition = true)
@NoExtend
@NoInstantiate
public class TokenManagerConfig<CTX extends ResourceOwnerOAuthContext & Serializable> implements Lifecycle {

  public static AtomicInteger defaultTokenManagerConfigIndex = new AtomicInteger(0);

  private static final Map<String, TokenManagerConfig> activeConfigs = new ConcurrentHashMap<>();

  /**
   * Identifier for the token manager configuration.
   */
  // TODO MULE-11424 Move to OAuthExtension
  @RefName
  private String name;

  /**
   * References an object store to use for storing oauth context data
   */
  // TODO MULE-11424 Move to OAuthExtension
  @Parameter
  @Optional
  @ObjectStoreReference
  private ObjectStore<CTX> objectStore;

  private ObjectStore<CTX> resolvedObjectStore;

  @Inject
  private LockFactory lockFactory;

  @Inject
  private ObjectStoreManager objectStoreManager;

  private ConfigOAuthContext configOAuthContext;

  private boolean initialised;
  private boolean started;

  public ObjectStore<CTX> getObjectStore() {
    return objectStore;
  }

  public void setObjectStore(ObjectStore<CTX> objectStore) {
    this.objectStore = objectStore;
  }

  public ObjectStore<CTX> getResolvedObjectStore() {
    return resolvedObjectStore;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getName() {
    return name;
  }

  @Override
  public synchronized void initialise() throws InitialisationException {
    if (initialised) {
      return;
    }
    if (objectStore == null) {
      resolvedObjectStore = objectStoreManager.getOrCreateObjectStore("token-manager-store-" + name,
                                                                      ObjectStoreSettings.builder().persistent(true).build());
    } else {
      resolvedObjectStore = objectStore;
    }
    configOAuthContext =
        new ConfigOAuthContext(lockFactory, new SimpleObjectStoreToMapAdapter(resolvedObjectStore), name);
    initialised = true;
  }

  @Override
  public void start() throws MuleException {
    if (!started) {
      startIfNeeded(resolvedObjectStore);
      started = true;
    }
  }

  @Override
  public void stop() throws MuleException {
    if (started) {
      stopIfNeeded(resolvedObjectStore);
      started = false;
    }
  }

  @Override
  public void dispose() {
    if (!initialised) {
      return;
    }

    configOAuthContext = null;

    activeConfigs.remove(name);

    initialised = false;
  }

  public static TokenManagerConfig createDefault() {
    final TokenManagerConfig tokenManagerConfig = new TokenManagerConfig();
    final String tokenManagerConfigName = "default-token-manager-config-" + defaultTokenManagerConfigIndex.getAndIncrement();
    tokenManagerConfig.setName(tokenManagerConfigName);
    activeConfigs.put(tokenManagerConfigName, tokenManagerConfig);
    return tokenManagerConfig;
  }

  /**
   * @param name the name of the {@link TokenManagerConfig} to get.
   * @return the config with the given name
   */
  public static TokenManagerConfig getTokenManagerConfigByName(String name) {
    return activeConfigs.get(name);
  }

  public ConfigOAuthContext getConfigOAuthContext() {
    return configOAuthContext;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof TokenManagerConfig) {
      TokenManagerConfig other = (TokenManagerConfig) obj;
      return name.equals(other.name) && resolvedObjectStore == other.resolvedObjectStore;
    }

    return false;
  }

  @Override
  public int hashCode() {
    return hash(name, resolvedObjectStore);
  }
}
