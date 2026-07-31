// TICKET-ADV117 — useDebouncedSearch(query, delay).
import { useState, useEffect } from 'react';

export function useDebouncedSearch(query, delay = 300) {
  const [debounced, setDebounced ] = useState(query);
  
  useEffect(() => {
    const handler = setTimeout(() => 
      setDebounced(query),
     delay);

    return () => {
      clearTimeout(handler);
    };
  }, [query, delay]);

  return debounced;
}
