// TICKET-ADV114 — Compound DataTable.
// TICKET-ADV117 — useDebouncedSearch.
import React, { useCallback, useEffect, useState } from 'react';
import { withAuth } from '@components/withAuth.jsx';
import DataTable from '@components/DataTable.jsx';
import { useDebouncedSearch } from '@hooks/useDebouncedSearch.js';
import { api } from '@services/apiService.js';

function Trades() {
  const [search, setSearch] = useState('');
  const debounced = useDebouncedSearch(search, 300);

  const [page, setPage] = useState(0);
  const [selectedId, setSelectedId] = useState(null);

  const [data, setData] = useState({
    items: [],
    totalPages: 0,
  });

  const handleSelect = useCallback((id) => {
    setSelectedId(id);
  }, []);

  useEffect(() => {
    let cancelled = false;

    const params = new URLSearchParams();
    params.set('page', String(page));

    if (debounced) {
      params.set('status', debounced);
    }

    api.listTrades(params.toString())
      .then((res) => {
        if (cancelled) return;

        setData({
          items: res.items ?? [],
          totalPages: res.totalPages ?? 0,
        });
      })
      .catch(() => {
        if (!cancelled) {
          setData({
            items: [],
            totalPages: 0,
          });
        }
      });

    return () => {
      cancelled = true;
    };
  }, [page, debounced]);

  return (
    <section>
      <h2>Trades</h2>

      <input
        aria-label="Filter by status"
        placeholder="status filter (PENDING/MATCHED/…)"
        value={search}
        onChange={(e) => {
          setSearch(e.target.value.toUpperCase());
          setPage(0);
        }}
      />

      <DataTable data={data.items}>
        <DataTable.Header
          columns={[
            { key: 'tradeRef', label: 'Ref' },
            { key: 'symbol', label: 'Symbol' },
            { key: 'qty', label: 'Qty' },
            { key: 'price', label: 'Price' },
            { key: 'status', label: 'Status' },
          ]}
        />

        <DataTable.Body
          renderRow={(trade) => (
            <TradeRow
              trade={trade}
              onClick={handleSelect}
            />
          )}
        />

        <DataTable.Pagination />
      </DataTable>
    </section>
  );
}

const TradeRow = React.memo(function TradeRow({ trade, onClick }) {
  return (
    <button
      type="button"
      onClick={() => onClick(trade.id)}
    >
      <span>{trade.tradeRef}</span>
      <span>{trade.symbol ?? trade.instrument}</span>
      <span>{trade.qty ?? trade.quantity}</span>
      <span>{trade.price}</span>
      <span>{trade.status}</span>
    </button>
  );
});

export default withAuth(Trades);