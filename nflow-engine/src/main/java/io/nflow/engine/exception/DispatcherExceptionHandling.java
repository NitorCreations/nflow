package io.nflow.engine.exception;

/**
 * Controls how an exception should be handled by the dispatcher.
 */
public class DispatcherExceptionHandling extends ExceptionHandling {
  /**
   * True when dispatcher should log the exception.
   */
  public final boolean log;
  /**
   * True when dispatcher should sleep a while after exception.
   */
  public final boolean sleep;
  /**
   * True when the sleep time should be randomized.
   */
  public final boolean randomizeSleep;

  DispatcherExceptionHandling(Builder builder) {
    super(builder);
    this.log = builder.log;
    this.sleep = builder.sleep;
    this.randomizeSleep = builder.randomizeSleep;
  }

  /**
   * Builder for exception handling settings.
   */
  public static class Builder extends ExceptionHandling.Builder<Builder> {
    boolean log = true;
    boolean sleep = true;
    boolean randomizeSleep = false;

    /**
     * {@inheritDoc}
     */
    @Override
    public Builder getThis() {
      return this;
    }

    /**
     * Set if dispatcher should log the exception or not.
     *
     * @param log
     *          True if dispatcher should log the exception.
     * @return This.
     */
    public Builder setLog(boolean log) {
      this.log = log;
      return this;
    }

    /**
     * Set if dispatcher should sleep a while after exception or not.
     *
     * @param sleep
     *          True if dispatcher should sleep a while after exception.
     * @return This.
     */
    public Builder setSleep(boolean sleep) {
      this.sleep = sleep;
      return this;
    }

    /**
     * Set if sleep time should be randomized or not.
     *
     * @param randomizeSleep
     *          True if sleep time should be randomized.
     * @return This.
     */
    public Builder setRandomizeSleep(boolean randomizeSleep) {
      this.randomizeSleep = randomizeSleep;
      return this;
    }

    /**
     * Create the dispatcher exception handling object.
     *
     * @return Dispatcher exception handling.
     */
    @Override
    public DispatcherExceptionHandling build() {
      return new DispatcherExceptionHandling(this);
    }
  }
}
