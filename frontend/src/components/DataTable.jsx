// Compound <DataTable> with Header / Body / Pagination subcomponents.
import React, { createContext, useContext, useMemo, useState } from 'react';

const DataTableContext = createContext(null);

function useDataTable() {
  const context = useContext(DataTableContext);

  if (!context) {
    throw new Error('DataTable subcomponents must be used inside <DataTable>');
  }

  return context;
}

export default function DataTable({ data, pageSize = 10, children }) {
  const [page, setPage] = useState(0);
  const [sortKey, setSortKey] = useState(null);
  const [sortDir, setSortDir] = useState('asc');

  const sortedRows = useMemo(() => {
    if (!sortKey) return data;

    return [...data].sort((a, b) => {
      const aValue = a[sortKey];
      const bValue = b[sortKey];

      if (aValue < bValue) return sortDir === 'asc' ? -1 : 1;
      if (aValue > bValue) return sortDir === 'asc' ? 1 : -1;
      return 0;
    });
  }, [data, sortKey, sortDir]);

  const pagedRows = useMemo(() => {
    const start = page * pageSize;
    return sortedRows.slice(start, start + pageSize);
  }, [sortedRows, page, pageSize]);

  const toggleSort = (key) => {
    if (sortKey === key) {
      setSortDir((current) => (current === 'asc' ? 'desc' : 'asc'));
    } else {
      setSortKey(key);
      setSortDir('asc');
    }

    setPage(0);
  };

  const value = useMemo(
    () => ({
      rows: pagedRows,
      page,
      pageSize,
      totalPages: Math.ceil(sortedRows.length / pageSize),
      sortKey,
      sortDir,
      toggleSort,
      setPage,
    }),
    [
      pagedRows,
      page,
      pageSize,
      sortedRows.length,
      sortKey,
      sortDir,
    ]
  );

  return (
    <DataTableContext.Provider value={value}>
      <div className="data-table">{children}</div>
    </DataTableContext.Provider>
  );
}

function Header({ columns }) {
  const { sortKey, sortDir, toggleSort } = useDataTable();

  return (
    <div className="data-table__header" role="row">
      {columns.map((column) => (
        <button
          key={column.key}
          type="button"
          className={
            sortKey === column.key
              ? `data-table__th data-table__th--active ${sortDir}`
              : 'data-table__th'
          }
          onClick={() => toggleSort(column.key)}
        >
          {column.label}
        </button>
      ))}
    </div>
  );
}

function Body({ renderRow }) {
  const { rows } = useDataTable();

  return (
    <div className="data-table__body">
      {rows.map((row, index) => (
        <div
          key={row.id ?? index}
          className="data-table__row"
          role="row"
        >
          {renderRow(row)}
        </div>
      ))}
    </div>
  );
}

function Pagination() {
  const { page, totalPages, setPage } = useDataTable();

  return (
    <nav className="data-table__pagination" aria-label="Pagination">
      <button
        type="button"
        disabled={page === 0}
        onClick={() => setPage(page - 1)}
      >
        ‹
      </button>

      <span>
        {page + 1} / {totalPages}
      </span>

      <button
        type="button"
        disabled={page === totalPages - 1}
        onClick={() => setPage(page + 1)}
      >
        ›
      </button>
    </nav>
  );
}

DataTable.Header = Header;
DataTable.Body = Body;
DataTable.Pagination = Pagination;