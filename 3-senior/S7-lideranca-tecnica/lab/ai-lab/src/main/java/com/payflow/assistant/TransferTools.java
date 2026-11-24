package com.payflow.assistant;

import java.util.Map;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

/**
 * The ONLY tools the assistant gets: read-only and scoped to the customer of the ticket.
 * There is no refund/transfer tool: a manipulated model cannot call what does not exist.
 */
public class TransferTools {

    private final String ticketOwnerId;
    private final Map<String, TransferSnapshot> transfers;

    public TransferTools(String ticketOwnerId, Map<String, TransferSnapshot> transfers) {
        this.ticketOwnerId = ticketOwnerId;
        this.transfers = transfers;
    }

    @Tool(description = "Returns the status of one transfer of the customer who opened the ticket")
    public String transferStatus(@ToolParam(description = "transfer id") String transferId) {
        TransferSnapshot t = transfers.get(transferId);
        if (t == null || !t.ownerId().equals(ticketOwnerId)) {
            return "NOT_FOUND";                       // same answer for "missing" and "not yours" (IDOR, J8.06)
        }
        return t.status();
    }
}
