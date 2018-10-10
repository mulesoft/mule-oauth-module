/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.oauth2.internal;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mule.extension.oauth2.internal.DeferredExpressionResolver;
import org.mule.runtime.api.el.MuleExpressionLanguage;
import org.mule.runtime.extension.api.runtime.parameter.Literal;
import org.mule.tck.size.SmallTest;

import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SmallTest
public class DeferredExpressionResolverTestCase {

  @Rule
  public ExpectedException exception = ExpectedException.none();

  private MuleExpressionLanguage evaluator = evaluatorMock();

  @Test
  public void testNonExpressionLiteralValue() {
    DeferredExpressionResolver resolver = new DeferredExpressionResolver(evaluator);
    Literal<Boolean> trueLiteral = literalMock("tRuE");
    Boolean shouldBeTrue = resolver.resolveExpression(trueLiteral, null);
    Literal<Boolean> falseLiteral = literalMock("false");
    Boolean shouldBeFalse = resolver.resolveExpression(falseLiteral, null);

    assertThat(shouldBeTrue, is(true));
    assertThat(shouldBeFalse, is(false));
  }

  @Test
  public void invalidNonExpressionLiteralValue() {
    exception.expect(IllegalArgumentException.class);
    exception.expectMessage("Invalid value [nonBoolean] can't be converted to Boolean");

    DeferredExpressionResolver resolver = new DeferredExpressionResolver(evaluator);
    Literal<Boolean> literal = literalMock("nonBoolean");
    resolver.resolveExpression(literal, null);
  }

  private Literal<Boolean> literalMock(String value) {
    Literal literal = mock(Literal.class);
    when(literal.getLiteralValue()).thenReturn(Optional.of(value));
    when(literal.getType()).thenReturn(Boolean.class);
    return literal;
  }

  private MuleExpressionLanguage evaluatorMock() {
    MuleExpressionLanguage expressionLanguage = mock(MuleExpressionLanguage.class);
    when(expressionLanguage.isExpression(anyString())).thenReturn(false);
    return expressionLanguage;
  }
}
