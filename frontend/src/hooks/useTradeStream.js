// TICKET-ADV116 — useTradeStream() — SSE subscription returning live trades.
import { useState } from 'react';
const maxTrades = 200;
export function useTradeStream(url = '/api/v1/trades/stream') {
  const [trades /*, setTrades */] = useState([]);
  const [isConnected /*, setConnected */] = useState(false);

  useEffect(() => {
    const eventSource = new EventSource(url);
    eventSource.onopen = () => {
      setConnected(true);
    }
    eventSource.onmessage = (e) => {
      const newTrade = JSON.parse(e.data);
      setTrades(prevTrades => [newTrade, ...prevTrades].slice(0, maxTrades));
    }
    eventSource.onerror = () => {
      setConnected(false);
    }

    return () => {
      eventSource.close();
    };
  }, [url]);

  return { trades, isConnected };
}
