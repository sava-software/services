package software.sava.services.solana.transactions;

import software.sava.core.accounts.PublicKey;
import software.sava.core.accounts.SolanaAccounts;
import software.sava.core.tx.Instruction;
import software.sava.core.tx.Transaction;
import software.sava.kms.core.signing.SigningService;
import software.sava.rpc.json.http.client.SolanaRpcClient;
import software.sava.rpc.json.http.request.Commitment;
import software.sava.rpc.json.http.response.*;
import software.sava.services.core.remote.load_balance.LoadBalancer;
import software.sava.services.solana.config.ChainItemFormatter;
import software.sava.services.solana.remote.call.CallWeights;
import software.sava.services.solana.websocket.WebSocketManager;
import software.sava.solana.web2.helius.client.http.HeliusClient;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

import static software.sava.rpc.json.http.request.Commitment.CONFIRMED;

public interface TransactionProcessor {

  static TransactionProcessor createProcessor(final ExecutorService executor,
                                              final SigningService signingService,
                                              final PublicKey feePayer,
                                              final SolanaAccounts solanaAccounts,
                                              final ChainItemFormatter formatter,
                                              final LoadBalancer<SolanaRpcClient> rpcClients,
                                              final LoadBalancer<SolanaRpcClient> sendClients,
                                              final LoadBalancer<HeliusClient> heliusClients,
                                              final CallWeights callWeights,
                                              final WebSocketManager webSocketManager) {
    return new TransactionProcessorRecord(
        executor,
        signingService,
        feePayer,
        solanaAccounts,
        formatter,
        rpcClients,
        sendClients,
        heliusClients,
        callWeights,
        webSocketManager
    );
  }

  ChainItemFormatter formatter();

  static String formatSimulationResult(final TxSimulation simulationResult) {
    return String.format("""
            
            Simulation Result:
              program: %s
              CU consumed: %d
              error: %s
              inner instructions:
              %s
              logs:
              %s
            """,
        simulationResult.programId(),
        simulationResult.unitsConsumed().orElse(-1),
        simulationResult.error(),
        simulationResult.innerInstructions().stream().map(InnerInstructions::toString)
            .collect(Collectors.joining("\n    * ", "  * ", "")),
        simulationResult.logs().stream().collect(Collectors.joining("\n    * ", "  * ", ""))
    );
  }

  WebSocketManager webSocketManager();

  String formatTxMeta(final String sig, final TxMeta txMeta);

  String formatTxResult(final String sig, final TxResult txResult);

  String formatSigStatus(final String sig, final TxStatus sigStatus);

  CompletableFuture<byte[]> sign(final byte[] serialized);

  CompletableFuture<byte[]> sign(final Transaction transaction);

  void setSignature(final byte[] serialized, final byte[] sig);

  void setSignature(final Transaction transaction, final byte[] sig);

  Transaction createTransaction(final SimulationFutures simulationFutures,
                                final TxSimulation simulationResult);

  long setBlockHash(final Transaction transaction, final TxSimulation simulationResult);

  long setBlockHash(final Transaction transaction, final LatestBlockHash blockHash);

  long setBlockHash(final Transaction transaction,
                    final TxSimulation simulationResult,
                    final CompletableFuture<LatestBlockHash> blockHashFuture);

  Transaction createAndSignTransaction(final SimulationFutures simulationFutures,
                                       final TxSimulation simulationResult,
                                       final CompletableFuture<LatestBlockHash> blockHashFuture);

  void signTransaction(final Transaction transaction);

  SendTxContext sendSignedTx(final Transaction transaction, final long blockHeight);

  SendTxContext signAndSignedTx(final Transaction transaction, final long blockHeight);

  SimulationFutures simulateAndEstimate(final Commitment commitment, final List<Instruction> instructions);

  default SimulationFutures simulateAndEstimate(final List<Instruction> instructions) {
    return simulateAndEstimate(CONFIRMED, instructions);
  }
}
