package com.dbtraining.reconx.service;

import com.dbtraining.reconx.dto.ReconResult;
import com.dbtraining.reconx.model.BondTrade;
import com.dbtraining.reconx.model.DerivativeTrade;
import com.dbtraining.reconx.model.EquityTrade;
import com.dbtraining.reconx.model.FXTrade;
import com.dbtraining.reconx.model.ReconciliationRule;
import com.dbtraining.reconx.model.TradeType;
import io.micrometer.core.annotation.Timed;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * ============================================================================
 * TICKET-ADV033 — ReconciliationEngine using Streams (parallel matching)
 * TICKET-ADV037 — CompletableFuture: parallel recon by counterparty
 * TICKET-ADV047 — Edge cases: empty/single/all-mismatched inputs handled
 * TICKET-ADV084 — @Timed exports reconciliation_duration_seconds histogram
 *
 * WHAT:    Compares internal trades against external (counterparty) trades and
 *          returns a ReconResult per internal trade (MATCHED or BREAK).
 * HOW:     Index externals by tradeRef, then stream internals and look each
 *          up. CompletableFuture variant batches by counterparty for
 *          throughput on large books.
 * WHY:     This is the spine of the product. Everything else (REST API,
 *          Kafka consumers, dashboard) ultimately calls into here.
 * OBSERVE: Histogram appears at /actuator/prometheus under
 *          reconciliation_duration_seconds.
 * ============================================================================
 */
@Service
public class ReconciliationEngine {

    @Timed(value = "reconciliation.duration", description = "Wall time of reconcile()",
           percentiles = {0.5, 0.95, 0.99}, histogram = true)
    public List<ReconResult> reconcile(List<TradeType> internal,
                                       List<TradeType> external,
                                       ReconciliationRule rule) {
        // TICKET-ADV047: guard against null/empty inputs
        if (internal == null || internal.isEmpty()) return List.of();

        // TICKET-ADV033: index external trades by tradeRef for O(1) lookup
        // (a, b) -> a merge function handles duplicate refs without throwing
        Map<String, TradeType> externalByRef = (external == null ? List.<TradeType>of() : external)
                .stream()
                .collect(Collectors.toMap(t -> t.tradeRef().value(), Function.identity(), (a, b) -> a));

        // TICKET-ADV033: parallelStream over internals, match each one
        return internal.parallelStream()
                .map(in -> matchOne(in, externalByRef.get(in.tradeRef().value()), rule))
                .toList();
    }

    /**
     * TICKET-ADV037 — split by counterparty, reconcile each batch concurrently,
     * combine into a single result list. Caller passes one external feed per
     * counterparty (typical real-world shape).
     *
     * Each counterparty's reconcile call runs on a thread from the JVM common
     * ForkJoinPool. Results are merged via CompletableFuture.allOf and
     * thenApply. The merged result size equals the sum of per-counterparty
     * input sizes.
     */
    public CompletableFuture<List<ReconResult>> reconcileByCounterparty(
            Map<Long, List<TradeType>> internalByCp,
            Map<Long, List<TradeType>> externalByCp,
            ReconciliationRule rule) {

        // Launch one CompletableFuture per counterparty
        List<CompletableFuture<List<ReconResult>>> futures = internalByCp.entrySet().stream()
                .map(e -> CompletableFuture.supplyAsync(() ->
                        reconcile(e.getValue(),
                                externalByCp.getOrDefault(e.getKey(), List.of()),
                                rule)))
                .toList();

        // allOf waits for every future, thenApply flatMaps results into one list
        return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new))
                .thenApply(v -> futures.stream()
                        .flatMap(f -> f.join().stream())
                        .toList());
    }

    /**
     * Match a single internal trade against its external counterpart.
     *
     * @param internal the internal trade record
     * @param external the matching external trade, or null if not found
     * @param rule     the reconciliation rule to apply
     * @return MATCHED if external exists and matches, BREAK otherwise
     */
    private ReconResult matchOne(TradeType internal, TradeType external, ReconciliationRule rule) {
        String ref = internal.tradeRef().value();

        // TICKET-ADV047: no matching external trade -> BREAK
        if (external == null) {
            return ReconResult.breakResult(ref, "MISSING_EXTERNAL",
                    "No external trade found for " + ref);
        }

        // TICKET-ADV033: compare price/quantity via the rule
        BigDecimal[] iPair = priceQty(internal);
        BigDecimal[] ePair = priceQty(external);

        if (rule.matches(iPair[0], iPair[1], ePair[0], ePair[1])) {
            return ReconResult.matched(ref);
        }

        // TICKET-ADV047: mismatch -> BREAK with VALUE_MISMATCH
        return ReconResult.breakResult(ref, "VALUE_MISMATCH",
                "internal=%s/%s external=%s/%s".formatted(iPair[0], iPair[1], ePair[0], ePair[1]));
    }

    /**
     * TICKET-ADV018 — Extract price and quantity from any concrete TradeType
     * via an exhaustive switch over the sealed hierarchy. The compiler enforces
     * that every permitted subtype has a case.
     */
    private BigDecimal[] priceQty(TradeType t) {
        return switch (t) {
            case EquityTrade e     -> new BigDecimal[]{e.price(), e.quantity()};
            case FXTrade fx        -> new BigDecimal[]{fx.fxRate(), fx.notionalCcy1()};
            case BondTrade b       -> new BigDecimal[]{b.couponRate(), b.faceValue()};
            case DerivativeTrade d -> new BigDecimal[]{d.strike(), d.quantity()};
        };
    }
}
