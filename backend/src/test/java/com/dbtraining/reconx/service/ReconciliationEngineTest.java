package com.dbtraining.reconx.service;

import com.dbtraining.reconx.dto.ReconResult;
import com.dbtraining.reconx.model.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

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
    @DisplayName("two identical EquityTrades + EXACT rule -> one ReconResult with status MATCHED")
    void testReconcile_exactMatch_returnsMatched() {
        EquityTrade internal = equity("EQU-20260603-0001", "100.00", "1000");
        EquityTrade external = equity("EQU-20260603-0001", "100.00", "1000");

        // when
        List<ReconResult> out = engine.reconcile(
                List.of(internal),
                List.of(external),
                ReconciliationRule.EXACT
        );

        // then
        assertThat(out).hasSize(1);
        assertThat(out.get(0).status())
                .isEqualTo(ReconResult.Status.MATCHED);
    }

    @ParameterizedTest(name = "price diff {0} stays within 1% tolerance -> MATCHED")
    @ValueSource(strings = {"0.10", "0.50", "0.99"})
    void testReconcile_priceTolerance_withinThreshold(String diff) {
        // given
        EquityTrade internal = equity("EQU-20260603-0002", "100.00", "1000");

        BigDecimal externalPrice =
                new BigDecimal("100.00").add(new BigDecimal(diff));

        EquityTrade external = equity(
                "EQU-20260603-0002",
                externalPrice.toPlainString(),
                "1000"
        );

        // when
        List<ReconResult> out = engine.reconcile(
                List.of(internal),
                List.of(external),
                ReconciliationRule.PRICE_TOLERANCE_1PCT
        );

        // then
        assertThat(out).hasSize(1);
        assertThat(out.get(0).status())
                .isEqualTo(ReconResult.Status.MATCHED);
    }

    @Test
    @DisplayName("internal trade with no external counterpart -> status BREAK, discrepancyType = \"MISSING_EXTERNAL\"")
    void testReconcile_missingCounterpartyTrade_returnsBreak() {
        // given
        EquityTrade internal = equity("EQU-20260603-0003", "100.00", "1000");

        // when
        List<ReconResult> out = engine.reconcile(
                List.of(internal),
                List.of(),
                ReconciliationRule.EXACT
        );

        // then
        assertThat(out).hasSize(1);
        assertThat(out.get(0).status())
                .isEqualTo(ReconResult.Status.BREAK);
        assertThat(out.get(0).discrepancyType())
                .isEqualTo("MISSING_EXTERNAL");
    }

    @Test
    @DisplayName("empty internal + empty external -> reconcile returns an empty list.")
    void testReconcile_emptyInternal_returnsEmpty() {
        // given
        List<TradeType> internal = List.of();
        List<TradeType> external = List.of();

        // when
        List<ReconResult> out = engine.reconcile(
                internal,
                external,
                ReconciliationRule.EXACT
        );
        // then
        assertThat(out).isEmpty();
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
