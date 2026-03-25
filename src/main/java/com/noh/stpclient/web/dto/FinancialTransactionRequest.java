package com.noh.stpclient.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.util.List;

/**
 * DTO for Financial Transaction Request.
 *
 * @param sessionId The session ID for authentication.
 * @param transaction The financial transaction data.
 */
@Schema(description = "Financial transaction submission request")
public record FinancialTransactionRequest(
//        @NotBlank(message = "Session ID cannot be blank")
        @Schema(description = "Gateway session ID (managed internally when using /gw/send or /gw/financial-transaction)", hidden = true)
        String sessionId,
        @NotNull(message = "Transaction data cannot be null")
        @Valid
        @Schema(description = "SWIFT transaction details")
        TransactionData transaction
) {
    /**
     * DTO for the financial transaction data.
     */
    @Schema(description = "SWIFT pacs.008 / pacs.009 RTGS transaction fields. Fields marked (pacs.008 only) are not required for pacs.009.")
    public record TransactionData(
            @NotBlank(message = "Message ID cannot be blank")
            @Schema(description = "Unique message identifier", example = "MSG20260314001")
            String messageId,

            @NotBlank(message = "Message sequence cannot be blank")
            @Schema(description = "SWIFT message sequence number", example = "0000001")
            String msgSequence,

            @NotBlank(message = "Message type cannot be blank")
            @Schema(description = "SWIFT message type", example = "pacs.008.001.08")
            String msgType,

            @NotBlank(message = "Business service cannot be blank")
            @Schema(description = "Business service identifier for AppHdr", example = "RTGS")
            String bizSvc,

            @NotBlank(message = "Business message ID cannot be blank")
            @Schema(description = "Business-level message identifier (EndToEndId)", example = "BIZID-20260314-001")
            String businessMessageId,

            @NotBlank(message = "Sender BIC cannot be blank")
            @Schema(description = "BIC of the sending institution", example = "LBBCLALABXXX")
            String senderBic,

            @NotBlank(message = "Receiver BIC cannot be blank")
            @Schema(description = "BIC of the receiving institution", example = "LPDRLALAXATS")
            String receiverBic,

            @NotBlank(message = "Instructing agent BIC cannot be blank")
            @Schema(description = "BIC of the instructing agent", example = "LBBCLALABXXX")
            String instructingAgentBic,

            @NotBlank(message = "Instructed agent BIC cannot be blank")
            @Schema(description = "BIC of the instructed agent", example = "LPDRLALAXATS")
            String instructedAgentBic,

            @NotBlank(message = "Debtor agent BIC cannot be blank")
            @Schema(description = "BIC of the debtor's bank", example = "LBBCLALABXXX")
            String debtorAgentBic,

            @NotBlank(message = "Currency cannot be blank")
            @Schema(description = "ISO 4217 currency code", example = "LAK")
            String currency,

            @NotNull(message = "Amount cannot be null")
            @Positive(message = "Amount must be positive")
            @Schema(description = "Transaction amount (must be positive)", example = "1000000.00")
            BigDecimal amount,

            @NotNull(message = "Settlement date cannot be null")
            @Schema(description = "Value/settlement date (YYYY-MM-DD)", example = "2026-03-14")
            String settlementDate,

            @Schema(description = "(pacs.008 only) Full name of the debtor", example = "Lao Brewery Co., Ltd.")
            String debtorName,

            @Schema(description = "(pacs.008 only) Debtor's account number", example = "0100001234567")
            String debtorAccount,

            @NotBlank(message = "Debtor agent account cannot be blank")
            @Schema(description = "Debtor agent's nostro/settlement account (pacs.008: DbtrAgtAcct, pacs.009: DbtrAcct)", example = "0199990000001")
            String debtorAgentAccount,

            @Schema(description = "(pacs.008 only) Full name of the creditor", example = "Phongsavanh Bank")
            String creditorName,

            @Schema(description = "(pacs.008 only) Creditor's account number", example = "0200007654321")
            String creditorAccount,

            @NotBlank(message = "Creditor agent account cannot be blank")
            @Schema(description = "Creditor agent's nostro/settlement account (pacs.008: CdtrAgtAcct, pacs.009: CdtrAcct)", example = "0299990000002")
            String creditorAgentAccount,

            @Schema(description = "(pacs.008 only) Debtor's address lines (max 2 lines)", example = "[\"123 Lane Xang Avenue\", \"Vientiane, Lao PDR\"]")
            List<String> debtorAddressLines,

            @Schema(description = "(pacs.008 only) Creditor's address lines (max 2 lines)", example = "[\"456 Samsenthai Road\", \"Vientiane, Lao PDR\"]")
            List<String> creditorAddressLines,

            @Schema(description = "(pacs.009 only) Instruction information for the next agent — maps to InstrForNxtAgt/InstrInf in the Document. Optional free-text field (e.g. payment purpose or routing instruction).", example = "test pacs009 format")
            String instrForNxtAgt,

            @Schema(description = "(pacs.008 only) Payment reference / remittance information", example = "Payment for invoice INV-2026-0042")
            String remittanceInformation
    ) {}
}
