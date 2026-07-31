// TICKET-ADV115 — useWebSocket(url) with auto-reconnect (exp backoff up to 5 tries).
import { useState, useEffect, useRef, useCallback } from 'react';


export function useWebSocket(url, { reconnect = true, maxRetries = 5 } = {}) {

  const [data , setData ] = useState(null);
  const [status , setStatus ] = useState('connecting');
  const socketRef = useRef(null);
  const retriesRef = useRef(0);
  const reconnectTimeoutRef = useRef(null);
  const cancelledRef = useRef(false);

  const unmountRef = useRef(false);

  useEffect(() => {
    cancelledRef.current = false;
    retriesRef.current = 0;

    function connect() {
      if (cancelledRef.current) return;
      
      const socket = new WebSocket(url);
      socketRef.current = socket;
      setStatus('connecting');
      socket.onopen = () => {
        if (!cancelledRef.current) {
          setStatus('open');
          retriesRef.current = 0; // reset retries on successful connection
        }
      };
      socket.onmessage = (event) => {
        if (cancelledRef.current) return;
        try {
          const parsedData = JSON.parse(event.data);
          setData(parsedData);
        } catch (e) {
          setData(event.data); // fallback to raw string if JSON parsing fails
        }
      };

      socket.onclose = () => {
        if (cancelledRef.current) return;
        setStatus('closed');
        if (reconnect && retriesRef.current < maxRetries) {
          const delay = Math.min(500 * Math.pow(2, retriesRef.current), 30000); // exponential backoff
          retriesRef.current += 1;
          reconnectTimeoutRef.current = setTimeout(connect, delay);
        }
      };
      socket.onerror = () => {
        if (cancelledRef.current) return;
        setStatus('error');
      };
    }

    connect();

    return () => {
      cancelledRef.current = true;
      if (socketRef.current && socketRef.current.readyState <= WebSocket.OPEN) {
        socketRef.current.close();
      }
      if (reconnectTimeoutRef.current) {
        clearTimeout(reconnectTimeoutRef.current);
      }
    };
  }, [url, reconnect, maxRetries]);

  const send = useCallback((payload) => {
    const socket = socketRef.current;
    if (!socket || socket.readyState !== WebSocket.OPEN) return;
    socket.send(typeof payload === 'string' ? payload : JSON.stringify(payload));
  }, []);


  return { data, status, send };
}
