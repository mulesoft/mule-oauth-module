/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.oauth2;

import org.mule.extension.http.api.request.authentication.HttpRequestAuthentication;
import org.mule.extension.http.api.request.proxy.HttpProxyConfig;
import org.mule.extension.oauth2.api.authorizationcode.DefaultAuthorizationCodeGrantType;
import org.mule.extension.oauth2.api.clientcredentials.ClientCredentialsGrantType;
import org.mule.extension.oauth2.api.exception.OAuthClientErrors;
import org.mule.extension.oauth2.internal.OAuthOperations;
import org.mule.runtime.extension.api.annotation.Extension;
import org.mule.runtime.extension.api.annotation.Import;
import org.mule.runtime.extension.api.annotation.Operations;
import org.mule.runtime.extension.api.annotation.SubTypeMapping;
import org.mule.runtime.extension.api.annotation.dsl.xml.Xml;
import org.mule.runtime.extension.api.annotation.error.ErrorTypes;

/**
 * An extension to hook oauth2 to http extension connectors.
 *
 * @since 1.0
 */
@Extension(name = "OAuth")
@Import(type = HttpRequestAuthentication.class)
@Import(type = HttpProxyConfig.class)
@Operations(OAuthOperations.class)
@SubTypeMapping(baseType = HttpRequestAuthentication.class,
    subTypes = {DefaultAuthorizationCodeGrantType.class, ClientCredentialsGrantType.class})
@ErrorTypes(OAuthClientErrors.class)
@Xml(prefix = "oauth")
public class OAuthExtension {

}
