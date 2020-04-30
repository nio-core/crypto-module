package contracts;

import contracts.ContractReceipt;

public interface IContractProcessingCallback {
    void processingFinished(ContractReceipt contractReceipt);
}
