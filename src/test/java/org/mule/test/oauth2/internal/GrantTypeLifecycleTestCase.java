/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.oauth2.internal;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;

import org.mule.extension.http.api.HttpResponseAttributes;
import org.mule.extension.oauth2.internal.AbstractGrantType;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.Disposable;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.lifecycle.Startable;
import org.mule.runtime.api.lifecycle.Stoppable;
import org.mule.runtime.extension.api.runtime.operation.Result;
import org.mule.runtime.http.api.domain.message.request.HttpRequestBuilder;
import org.mule.runtime.module.extension.internal.runtime.resolver.ImmutableLiteral;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import io.qameta.allure.Description;
import io.qameta.allure.Issue;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runner.RunWith;
import org.junit.Test;

@Issue("W-10781856")
@RunWith(Parameterized.class)
public class GrantTypeLifecycleTestCase {

  public static final String GRANT_TYPE_LIFECYCLE_EXCEPTION = "Error when applying lifecycle phase";

  @Parameter
  public Consumer<TestGrantType> grantTypeBasedLifecycleAction;

  @Parameter(1)
  public boolean expectedInitialisedDancer;

  @Parameter(2)
  public boolean expectedStartedDancer;

  @Parameter(3)
  public boolean expectedStoppedDancer;

  @Parameter(4)
  public boolean expectedDisposedDancer;

  // Actions as parameters
  // The consumers should be wrapped in an unchecked exception
  private final static Consumer<TestGrantType> initialiseGrantTypeAction = grantType -> {
    try {
      grantType.initialise();
    } catch (MuleException e) {
      throw new RuntimeException(e);
    }
  };

  private static final Consumer<TestGrantType> startGrantTypeAction = grantType -> {
    try {
      grantType.start();
    } catch (MuleException e) {
      throw new RuntimeException(e);
    }
  };

  private static final Consumer<TestGrantType> stopGrantTypeAction = grantType -> {
    try {
      grantType.stop();
    } catch (MuleException e) {
      throw new RuntimeException(e);
    }
  };

  private static final Consumer<TestGrantType> disposeGrantTypeAction = TestGrantType::dispose;

  @Parameterized.Parameters
  public static Object[][] params() {
    return new Object[][] {
        {initialiseGrantTypeAction, true, false, false, false},
        {startGrantTypeAction, false, true, false, false},
        {stopGrantTypeAction, false, false, true, false},
        {disposeGrantTypeAction, false, false, false, true}
    };
  }

  @Test
  @Description("When an error applying a lifecycle phase, the gran type lifecycle phase can be applied")
  public void grantTypeLifecyclePhaseCanBeAppliedAfterException() {
    TestGrantType grantType = new TestGrantType();

    try {
      grantTypeBasedLifecycleAction.accept(grantType);
    } catch (Exception e) {
      assertThat(e, instanceOf(IllegalStateException.class));
      assertThat(grantType, not(grantType.isInitialised()));
    }

    grantTypeBasedLifecycleAction.accept(grantType);
    assertThat(grantType.isInitialised(), equalTo(expectedInitialisedDancer));
    assertThat(((TestGrantType.TestDancer) grantType.getDancer()).isStarted(), equalTo(expectedStartedDancer));
    assertThat(((TestGrantType.TestDancer) grantType.getDancer()).isStopped(), equalTo(expectedStoppedDancer));
    assertThat(((TestGrantType.TestDancer) grantType.getDancer()).isDisposed(), equalTo(expectedDisposedDancer));
  }

  /**
   * An {@link AbstractGrantType} used to force an exception in lifecycle phases.
   */
  private static class TestGrantType extends AbstractGrantType {

    private final AtomicBoolean throwException = new AtomicBoolean(true);

    private boolean initialised;

    private final TestDancer dancer = new TestDancer();

    public TestGrantType() {
      setRefreshTokenWhen(new ImmutableLiteral<>("#['true']", Boolean.class));
    }

    @Override
    protected void doInitialize() throws InitialisationException {
      super.doInitialize();

      if (throwException.getAndSet(false)) {
        throw new IllegalStateException(GRANT_TYPE_LIFECYCLE_EXCEPTION);
      }

      initialised = true;
    }

    @Override
    public Object getDancer() {
      if (throwException.getAndSet(false)) {
        throw new IllegalStateException(GRANT_TYPE_LIFECYCLE_EXCEPTION);
      }

      return dancer;
    }

    @Override
    public boolean isEncodeClientCredentialsInBody() {
      return false;
    }

    @Override
    public void authenticate(HttpRequestBuilder builder) {
      // Nothing to do
    }

    @Override
    public boolean shouldRetry(Result<Object, HttpResponseAttributes> firstAttemptResult) {
      return false;
    }

    public boolean isInitialised() {
      return initialised;
    }

    /**
     * A dancer to verify that this is started
     */
    private static class TestDancer implements Startable, Disposable, Stoppable {

      private boolean started;
      private boolean disposed;
      private boolean stopped;

      @Override
      public void start() {
        this.started = true;
      }

      @Override
      public void dispose() {
        this.disposed = true;
      }

      @Override
      public void stop() {
        this.stopped = true;
      }

      public boolean isStarted() {
        return started;
      }

      public boolean isDisposed() {
        return disposed;
      }

      public boolean isStopped() {
        return stopped;
      }
    }
  }
}
