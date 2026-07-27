 package com.dbtraining.reconx.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TradeFactoryTest {

    private Map<String, Object> equityMap() {
        Map<String, Object> m = new HashMap<>();
        m.put("tradeRef", "EQU-20260603-0001");
        m.put("symbol", "SAP.DE");
        m.put("quantity", "100");
        m.put("price", "150.50");
        m.put("currency", "EUR");
        m.put("side", "BUY");
        m.put("tradeDate", "2026-06-03");
        m.put("counterpartyId", 1L);
        return m;
    }

    private Map<String, Object> fxMap() {
        Map<String, Object> m = new HashMap<>();
m.put("tradeRef", "FXS-20260603-0002");
        m.put("ccy1", "EUR");
        m.put("ccy2", "USD");
        m.put("notionalCcy1", "100000");
        m.put("fxRate", "1.12");
        m.put("side", "BUY");
        m.put("tradeDate", "2026-06-03");
        m.put("counterpartyId", 2L);
        return m;
    }

    private Map<String, Object> bondMap() {
        Map<String, Object> m = new HashMap<>();
        m.put("tradeRef", "BND-20260603-0003");
        m.put("isin", "DE0001234567");
        m.put("faceValue", "1000");
        m.put("couponRate", "0.05");
        m.put("maturityDate", "2030-06-03");
        m.put("currency", "EUR");
        m.put("side", "SELL");
        m.put("tradeDate", "2026-06-03");
        m.put("counterpartyId", 3L);
        return m;
    }

    private Map<String, Object> derivativeMap() {
        Map<String, Object> m = new HashMap<>();
        m.put("tradeRef", "DER-20260603-0004");
        m.put("underlying", "AAPL");
        m.put("strike", "200");
        m.put("quantity", "10");
        m.put("expiry", "2027-06-03");
        m.put("optionType", "CALL");
        m.put("currency", "USD");
        m.put("side", "BUY");
        m.put("tradeDate", "2026-06-03");
        m.put("counterpartyId", 4L);
        return m;
    }

    @Test
    void create_withEquity_returnsEquityTrade() {
        TradeType trade = TradeFactory.create("EQUITY", equityMap());

        assertThat(trade).isInstanceOf(EquityTrade.class);
        assertThat(trade.assetClass()).isEqualTo(TradeType.AssetClass.EQUITY);
        assertThat(trade.tradeRef()).isEqualTo(TradeRef.of("EQU-20260603-0001"));
        assertThat(trade.tradeDate()).isEqualTo(LocalDate.of(2026, 6, 3));
    }

    @Test
    void create_withFX_returnsFXTrade() {
        TradeType trade = TradeFactory.create("FX", fxMap());

        assertThat(trade).isInstanceOf(FXTrade.class);
        assertThat(trade.assetClass()).isEqualTo(TradeType.AssetClass.FX);
        assertThat(trade.tradeRef()).isEqualTo(TradeRef.of("FX-20260603-0002"));
    }

    @Test
    void create_withBond_returnsBondTrade() {
        TradeType trade = TradeFactory.create("BOND", bondMap());

        assertThat(trade).isInstanceOf(BondTrade.class);
        assertThat(trade.assetClass()).isEqualTo(TradeType.AssetClass.BOND);
        assertThat(trade.tradeRef()).isEqualTo(TradeRef.of("BND-20260603-0003"));
    }

    @Test
    void create_withDerivative_returnsDerivativeTrade() {
        TradeType trade = TradeFactory.create("DERIVATIVE", derivativeMap());

        assertThat(trade).isInstanceOf(DerivativeTrade.class);
        assertThat(trade.assetClass()).isEqualTo(TradeType.AssetClass.DERIVATIVE);
        assertThat(trade.tradeRef()).isEqualTo(TradeRef.of("DER-20260603-0004"));
    }

    @Test
    void create_withLowerCaseAssetClass_handlesCaseInsensitivity() {
        TradeType trade = TradeFactory.create("equity", equityMap());

        assertThat(trade).isInstanceOf(EquityTrade.class);
    }

    @Test
    void create_withUnknownAssetClass_throwsIllegalArgumentException() {
        Map<String, Object> map = equityMap();

        assertThatThrownBy(() -> TradeFactory.create("FOO", map))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void create_withMissingPriceKey_throwsNullPointerException() {
        Map<String, Object> map = equityMap();
        map.remove("price");

        assertThatThrownBy(() -> TradeFactory.create("EQUITY", map))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void create_withMissingTradeRef_throwsNullPointerException() {
        Map<String, Object> map = equityMap();
        map.remove("tradeRef");

        assertThatThrownBy(() -> TradeFactory.create("EQUITY", map))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void create_withEquity_notionalIsPriceTimesQuantity() {
        TradeType trade = TradeFactory.create("EQUITY", equityMap());

        assertThat(trade.notional().amount()).isEqualByComparingTo(new BigDecimal("15050"));
        assertThat(trade.notional().currency().getCurrencyCode()).isEqualTo("EUR");
    }

    @Test
    void constructor_isPrivate() throws Exception {
        assertThat(TradeFactory.class.getDeclaredConstructors())
                .allMatch(c -> c.getParameterCount() == 0 && !c.canAccess(null));
    }

    @Test
    void factory_isFinal() {
        assertThat(java.lang.reflect.Modifier.isFinal(TradeFactory.class.getModifiers())).isTrue();
    }
}
