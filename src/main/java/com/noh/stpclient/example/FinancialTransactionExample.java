package com.noh.stpclient.example;

import com.noh.stpclient.web.dto.FinancialTransactionRequest;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.util.List;

/**
 * Example class demonstrating how to use the financial transaction functionality.
 */
@Slf4j
public class FinancialTransactionExample {

    /**
     * Creates a sample financial transaction request based on the provided XML example.
     */
    public static FinancialTransactionRequest createSampleRequest() {
        var transaction = new FinancialTransactionRequest.TransactionData(
            "2605501332860000",                    // messageId
            "2605501332860000",                    // businessMessageId
            "IDCBLALA",                            // senderBic
            "LPDRLALA",                            // receiverBic
            "IDCBLALA",                            // instructingAgentBic
            "COEBLALAXXX",                         // instructedAgentBic
            "USD",                                 // currency
            new BigDecimal("64000"),               // amount
            "2026-02-25",                          // settlementDate
            "MR KHAMCHANH SOULIGNAPHANH",          // debtorName
            "0700000864347",                       // debtorAccount
            "0000010001150512",                    // debtorAgentAccount
            "SOMPHONE SAIYAVONG MR",               // creditorName
            "0961227683628",                       // creditorAccount
            "0000010000200510",                    // creditorAgentAccount
            List.of(" PHOXAY", " ", " PHOXAY", " "), // debtorAddressLines
            "PURCHASE FOR GOODS"                   // remittanceInformation
        );

        return new FinancialTransactionRequest(
            "sample-session-id", // This will be replaced by actual session ID
            transaction
        );
    }

    /**
     * Example usage of the financial transaction endpoint.
     * 
     * Request body format:
     * {
     *   "data": {
     *     "sessionId": "your-session-id",
     *     "transaction": {
     *       "messageId": "2605501332860000",
     *       "businessMessageId": "2605501332860000",
     *       "senderBic": "IDCBLALA",
     *       "receiverBic": "LPDRLALA",
     *       "instructingAgentBic": "IDCBLALA",
     *       "instructedAgentBic": "COEBLALAXXX",
     *       "currency": "USD",
     *       "amount": 64000,
     *       "settlementDate": "2026-02-25",
     *       "debtorName": "MR KHAMCHANH SOULIGNAPHANH",
     *       "debtorAccount": "0700000864347",
     *       "debtorAgentAccount": "0000010001150512",
     *       "creditorName": "SOMPHONE SAIYAVONG MR",
     *       "creditorAccount": "0961227683628",
     *       "creditorAgentAccount": "0000010000200510",
     *       "debtorAddressLines": [" PHOXAY", " ", " PHOXAY", " "],
     *       "remittanceInformation": "PURCHASE FOR GOODS"
     *     }
     *   }
     * }
     * 
     * Endpoint: POST /gw/financial-transaction
     * 
     * Response will contain the generated XML in the data field if successful.
     */
    public static void main(String[] args) {
        FinancialTransactionRequest request = createSampleRequest();
        log.info("Sample financial transaction request created: {}", request);
    }
}
