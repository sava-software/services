package software.sava.services.core.remote.call;

import software.sava.services.core.remote.load_balance.LoadBalancer;
import software.sava.services.core.request_capacity.context.CallContext;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

final class CourteousBalancedCall<I, R> extends GreedyBalancedCall<I, R> {

  private final int maxTryClaim;
  private final boolean forceCall;

  CourteousBalancedCall(final LoadBalancer<I> loadBalancer,
                        final Function<I, CompletableFuture<R>> call,
                        final CallContext callContext,
                        final int callWeight,
                        final int maxTryClaim,
                        final boolean forceCall,
                        final boolean measureCallTime,
                        final String retryLogContext) {
    super(loadBalancer, call, callContext, callWeight, measureCallTime, retryLogContext);
    this.maxTryClaim = maxTryClaim;
    this.forceCall = forceCall;
  }

  @Override
  public CompletableFuture<R> call() {
    this.next = loadBalancer.withContext();
    TRY_NEXT:
    for (int i = 0; i < maxTryClaim; ++i) {
      if (this.next.capacityState().tryClaimRequest(callContext, callWeight)) {
        return call.apply(this.next.item());
      } else {
        if (loadBalancer.size() > 1) {
          loadBalancer.sort();
          final var previous = this.next;
          this.next = loadBalancer.withContext();
          if (previous != this.next && this.next.capacityState().hasCapacity(callContext, callWeight)) {
            continue;
          }
          for (final var item : loadBalancer.items()) {
            if (previous != item && item.capacityState().hasCapacity(callContext, callWeight)) {
              this.next = item;
              continue TRY_NEXT;
            }
          }
        }
        final long delayMillis = this.next.capacityState().durationUntil(callContext, callWeight, MILLISECONDS);
        if (delayMillis <= 0) {
          this.next.capacityState().claimRequest(callContext, callWeight);
          return call.apply(this.next.item());
        } else {
          try {
            Thread.sleep(delayMillis);
          } catch (final InterruptedException e) {
            throw new RuntimeException(e);
          }
          loadBalancer.sort();
          this.next = loadBalancer.withContext();
        }
      }
    }
    if (forceCall) {
      this.next.capacityState().claimRequest(callContext, callWeight);
      return call.apply(this.next.item());
    } else {
      return null;
    }
  }
}
