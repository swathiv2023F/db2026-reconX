package com.dbtraining.reconx.service;

import com.dbtraining.reconx.dto.ReconResult;
import com.dbtraining.reconx.model.*;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * TICKET-ADV040 / ADV041 / ADV042 / ADV047 — TDD tests for ReconciliationEngine.
 *
 * Tests cover:
 * - Exact match (ADV040)
 * - Price tolerance match (ADV041)
 * - Missing external trade -> BREAK (ADV042)
 * - Empty internal -> empty result (ADV047)
 */
class ReconciliationEngineTest {

    private final ReconciliationEngine engine = new ReconciliationEngine();

    @Test
    void testReconcile_exactMatch_returnsMatched() {
        // TICKET-ADV040: two identical EquityTrades + EXACT rule -> MATCHED
        EquityTrade internal = equity("EQU-20260603-0001", "100.00", "1000");
        EquityTrade external = equity("EQU-20260603-0001", "100.00", "1000");

        List<ReconResult> out = engine.reconcile(List.of(internal), List.of(external), ReconciliationRule.EXACT);

        assertThat(out).hasSize(1);
        assertThat(out.get(0).status()).isEqualTo(ReconResult.Status.MATCHED);
    }

    @Test
    void testReconcile_priceTolerance_withinThreshold() {
        // TICKET-ADV041: prices 100.00 vs 100.50 + PRICE_TOLERANCE_1PCT -> MATCHED
        EquityTrade internal = equity("EQU-20260603-0002", "100.00", "1000");
        EquityTrade external = equity("EQU-20260603-0002", "100.50", "1000");

        List<ReconResult> out = engine.reconcile(List.of(internal), List.of(external),
                ReconciliationRule.PRICE_TOLERANCE_1PCT);

        assertThat(out.get(0).status()).isEqualTo(ReconResult.Status.MATCHED);
    }

    @Test
    void testReconcile_missingCounterpartyTrade_returnsBreak() {
        // TICKET-ADV042: internal trade with no external counterpart -> BREAK + MISSING_EXTERNAL
        EquityTrade internal = equity("EQU-20260603-0003", "100.00", "1000");

        List<ReconResult> out = engine.reconcile(List.of(internal), List.of(), ReconciliationRule.EXACT);

        assertThat(out.get(0).status()).isEqualTo(ReconResult.Status.BREAK);
        assertThat(out.get(0).discrepancyType()).isEqualTo("MISSING_EXTERNAL");
    }

    @Test
    void testReconcile_emptyInternal_returnsEmpty() {
        // TICKET-ADV047: empty/null internal -> return empty list, no exception
        assertThat(engine.reconcile(List.of(), List.of(), ReconciliationRule.EXACT)).isEmpty();
        assertThat(engine.reconcile(null, List.of(), ReconciliationRule.EXACT)).isEmpty();
    }

    private EquityTrade equity(String ref, String price, String qty) {
        return EquityTrade.builder()
                .tradeRef(TradeRef.of(ref))
                .instrumentSymbol("SAP.DE")
                .price(new BigDecimal(price))
                .quantity(new BigDecimal(qty))
                .currency("EUR").side(Side.BUY)
                .tradeDate(LocalDate.of(2026, 6, 3))
                .counterpartyId(1L)
                .build();
    }
}
