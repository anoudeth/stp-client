package com.noh.stpclient.web.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO for Financial Transaction Request.
 *
 * @param sessionId The session ID for authentication.
 * @param transaction The financial transaction data.
 */
public record FinancialTransactionRequest(
        @NotBlank(message = "Session ID cannot be blank")
        String sessionId,
        @NotNull(message = "Transaction data cannot be null")
        @Valid
        TransactionData transaction
) {
    /**
     * DTO for the financial transaction data.
     */
    public record TransactionData(
            @NotBlank(message = "Message ID cannot be blank")
            String messageId,

            @NotBlank(message = "Message sequence cannot be blank")
            String msgSequence,

            @NotBlank(message = "Business message ID cannot be blank")
            String businessMessageId,

            @NotBlank(message = "Sender BIC cannot be blank")
            String senderBic,

            @NotBlank(message = "Receiver BIC cannot be blank")
            String receiverBic,

            @NotBlank(message = "Instructing agent BIC cannot be blank")
            String instructingAgentBic,

            @NotBlank(message = "Instructed agent BIC cannot be blank")
            String instructedAgentBic,

            @NotBlank(message = "Debtor agent BIC cannot be blank")
            String debtorAgentBic,
            
            @NotBlank(message = "Currency cannot be blank")
            String currency,
            
            @NotNull(message = "Amount cannot be null")
            @Positive(message = "Amount must be positive")
            BigDecimal amount,
            
            @NotNull(message = "Settlement date cannot be null")
            String settlementDate,
            
            @NotBlank(message = "Debtor name cannot be blank")
            String debtorName,
            
            @NotBlank(message = "Debtor account cannot be blank")
            String debtorAccount,
            
            @NotBlank(message = "Debtor agent account cannot be blank")
            String debtorAgentAccount,
            
            @NotBlank(message = "Creditor name cannot be blank")
            String creditorName,
            
            @NotBlank(message = "Creditor account cannot be blank")
            String creditorAccount,
            
            @NotBlank(message = "Creditor agent account cannot be blank")
            String creditorAgentAccount,
            
            List<String> debtorAddressLines,

            String instrForNxtAgt,

            @NotBlank(message = "Remittance information cannot be blank")
            String remittanceInformation
    ) {}
}
