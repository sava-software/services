package software.sava.services.solana.accounts.lookup.http;

import com.sun.net.httpserver.HttpExchange;
import software.sava.core.accounts.PublicKey;
import software.sava.core.tx.Transaction;
import software.sava.rpc.json.PublicKeyEncoding;
import software.sava.services.solana.accounts.lookup.LookupTableCache;
import software.sava.services.solana.accounts.lookup.LookupTableDiscoveryService;
import systems.comodal.jsoniter.JsonIterator;

import java.util.HashSet;

final class FromAccountsHandler extends OptimalTablesHandler {

  FromAccountsHandler(final LookupTableDiscoveryService tableService,
                      final LookupTableCache tableCache) {
    super(tableService, tableCache);
  }

  protected void handlePost(final HttpExchange exchange,
                            final long startExchange,
                            final byte[] body) {
    final boolean accountsOnly = accountsOnly(exchange);

    final var ji = JsonIterator.parse(body);
    final var distinctAccounts = HashSet.<PublicKey>newHashSet(Transaction.MAX_ACCOUNTS);
    while (ji.readArray()) {
      distinctAccounts.add(PublicKeyEncoding.parseBase58Encoded(ji));
    }

    final long start = System.currentTimeMillis();
    final var lookupTables = tableService.findOptimalSetOfTables(distinctAccounts);
    writeResponse(exchange, startExchange, accountsOnly, start, lookupTables);
  }
}