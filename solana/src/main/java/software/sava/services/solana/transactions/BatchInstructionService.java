package software.sava.services.solana.transactions;

import software.sava.core.accounts.PublicKey;
import software.sava.core.tx.Instruction;
import software.sava.services.solana.epoch.EpochInfoService;
import software.sava.services.solana.remote.call.RpcCaller;
import software.sava.solana.programs.clients.NativeProgramClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public interface BatchInstructionService extends InstructionService {

  static BatchInstructionService createService(final RpcCaller rpcCaller,
                                               final TransactionProcessor transactionProcessor,
                                               final NativeProgramClient nativeProgramClient,
                                               final EpochInfoService epochInfoService,
                                               final TxMonitorService txMonitorService,
                                               final int batchSize) {
    return new BaseBatchInstructionService(
        rpcCaller,
        transactionProcessor,
        nativeProgramClient,
        epochInfoService,
        txMonitorService,
        batchSize
    );
  }

  TransactionResult processBatch(final List<Instruction> instructions,
                                 final SimulationFutures simulationFutures,
                                 final String logContext) throws InterruptedException;

  List<TransactionResult> batchProcess(final List<Instruction> instructions, final String logContext) throws InterruptedException;

  ArrayList<TransactionResult> batchProcess(final Map<PublicKey, ?> accountsMap,
                                            final String logContext,
                                            final Function<List<PublicKey>, List<Instruction>> batchFactory) throws InterruptedException;
}