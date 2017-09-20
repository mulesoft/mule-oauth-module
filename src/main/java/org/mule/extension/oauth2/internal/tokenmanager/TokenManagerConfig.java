/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.oauth2.internal.tokenmanager;

import org.mule.extension.oauth2.internal.authorizationcode.state.ConfigOAuthContext;
import org.mule.runtime.api.lifecycle.Disposable;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.store.ObjectStore;
import org.mule.runtime.api.store.ObjectStoreSettings;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.context.MuleContextAware;
import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.dsl.xml.TypeDsl;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.RefName;
import org.mule.runtime.extension.api.annotation.param.reference.ObjectStoreReference;
import org.mule.runtime.oauth.api.state.DefaultResourceOwnerOAuthContext;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Token manager stores all the OAuth State (access token, refresh token).
 *
 * It can be referenced to access the state inside a flow for custom processing of oauth dance content.
 */
@Alias("token-manager-config")
@TypeDsl(allowTopLevelDefinition = true)
public class TokenManagerConfig implements Initialisable, Disposable, MuleContextAware {

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
  private ObjectStore<DefaultResourceOwnerOAuthContext> objectStore;

  private MuleContext muleContext;

  private ConfigOAuthContext configOAuthContext;

  private boolean initialised;

  public ObjectStore<DefaultResourceOwnerOAuthContext> getObjectStore() {
    return objectStore;
  }

  public void setObjectStore(ObjectStore<DefaultResourceOwnerOAuthContext> objectStore) {
    this.objectStore = objectStore;
  }

  public void setName(String name) {
    this.name = name;
  }

  @Override
  public synchronized void initialise() throws InitialisationException {
    if (initialised) {
      return;
    }
    if (objectStore == null) {
      objectStore =
          muleContext.getObjectStoreManager().createObjectStore("token-manager-store-" + name,
                                                                ObjectStoreSettings.builder().persistent(true).build());
    }
    configOAuthContext = new ConfigOAuthContext(muleContext.getLockFactory(), objectStore, name);
    initialised = true;
  }

  @Override
  public void dispose() {
    activeConfigs.remove(name);
  }

  public static TokenManagerConfig createDefault(final MuleContext context) throws InitialisationException {
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
  public void setMuleContext(MuleContext muleContext) {
    this.muleContext = muleContext;
  }

}
