package software.sava.services.core.remote.call;

import software.sava.services.core.remote.load_balance.BalancedItem;
import software.sava.services.core.remote.load_balance.LoadBalancer;
import software.sava.services.core.request_capacity.context.CallContext;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static software.sava.services.core.remote.call.ComposedCall.throwException;

class UncheckedBalancedCall<I, R> implements Call<R> {

  protected final LoadBalancer<I> loadBalancer;
  protected final Function<I, CompletableFuture<R>> call;
  protected final CallContext callContext;
  protected final String retryLogContext;

  protected BalancedItem<I> next;

  UncheckedBalancedCall(final LoadBalancer<I> loadBalancer,
                        final Function<I, CompletableFuture<R>> call,
                        final CallContext callContext,
                        final String retryLogContext) {
    this.loadBalancer = loadBalancer;
    this.call = call;
    this.callContext = callContext;
    this.retryLogContext = retryLogContext;
  }

  protected CompletableFuture<R> call() {
    this.next = loadBalancer.withContext();
    return call.apply(this.next.item());
  }

  @Override
  public final R get() {
    try {
      var callFuture = call();
      if (callFuture == null) {
        return null;
      }
      long start = callContext.measureCallTime() ? System.currentTimeMillis() : 0;
      final int numItems = loadBalancer.size();
      for (long errorCount = 0, retry = 0; ; ) {
        try {
          final var result = callFuture.get();
          if (start > 0) {
            this.next.sample(System.currentTimeMillis() - start);
          }
          this.next.success();
          return result;
        } catch (final ExecutionException e) {
          final long sleep = this.next.onError(++errorCount, retryLogContext, e.getCause(), MILLISECONDS);
          loadBalancer.sort();
          if (sleep < 0 || errorCount > callContext.maxRetries()) {
            throw throwException(e);
          }
          if (++retry < numItems && !loadBalancer.peek().equals(this.next)) {
            errorCount = retry - 1; // try next balanced item.
          } else if (sleep > 0) {
            //noinspection BusyWait
            Thread.sleep(sleep);
          }
          callFuture = call();
          if (callFuture == null) {
            return null;
          }
          if (start > 0) {
            start = System.currentTimeMillis();
          }
        }
      }
    } catch (final InterruptedException ex) {
      throw new RuntimeException(ex);
    }
  }
}
