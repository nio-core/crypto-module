package contracts;

import contracts.ContractReceipt;

public interface ContractProcessingCallback {
    void processingFinished(ContractReceipt contractReceipt);
}
