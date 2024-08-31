package software.sava.services.solana.accounts.lookup;

import software.sava.core.accounts.PublicKey;
import software.sava.core.accounts.lookup.AddressLookupTable;
import software.sava.core.accounts.meta.LookupTableAccountMeta;
import software.sava.rpc.json.http.client.SolanaRpcClient;
import software.sava.services.core.remote.call.BalancedErrorHandler;
import software.sava.services.core.remote.load_balance.LoadBalancer;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

import static software.sava.services.core.remote.call.ErrorHandler.linearBackoff;
import static software.sava.services.solana.remote.call.RemoteCallUtil.createRpcClientErrorHandler;

public interface LookupTableCache {

  static LookupTableCache createCache(final ExecutorService executorService,
                                      final int initialCapacity,
                                      final LoadBalancer<SolanaRpcClient> rpcClients,
                                      final BalancedErrorHandler<SolanaRpcClient> errorHandler) {
    return new LookupTableCacheMap(
        executorService,
        initialCapacity,
        rpcClients,
        errorHandler,
        AddressLookupTable.LOOKUP_TABLE_MAX_ADDRESSES);
  }

  static LookupTableCache createCache(final ExecutorService executorService,
                                      final int initialCapacity,
                                      final LoadBalancer<SolanaRpcClient> rpcClients) {
    final var errorHandler = createRpcClientErrorHandler(linearBackoff(1, 21));
    return createCache(executorService, initialCapacity, rpcClients, errorHandler);
  }

  LoadBalancer<SolanaRpcClient> rpcClients();

  AddressLookupTable getTable(final PublicKey lookupTableKey);

  AddressLookupTable getOrFetchTable(final PublicKey lookupTableKey);

  LookupTableAccountMeta[] getOrFetchTables(final List<PublicKey> lookupTableKeys);

  CompletableFuture<AddressLookupTable> getOrFetchTableAsync(final PublicKey lookupTableKey);

  CompletableFuture<LookupTableAccountMeta[]> getOrFetchTablesAsync(final List<PublicKey> lookupTableKeys);

  void refreshStaleAccounts(final Duration staleIfOlderThan, final int batchSize);

  default void refreshStaleAccounts(final Duration staleIfOlderThan) {
    refreshStaleAccounts(staleIfOlderThan, SolanaRpcClient.MAX_MULTIPLE_ACCOUNTS);
  }

  int refreshOldestAccounts(final int limit);
}
