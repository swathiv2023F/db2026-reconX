// TICKET-ADV118 — useInfiniteScroll: invokes loadMore() when sentinel is visible.
import { useRef } from 'react';

export function useInfiniteScroll(loadMore) {
  const sentinelRef = useRef(null);
    

  // TODO(TICKET-ADV118): in a useEffect, create an IntersectionObserver that
  //                     calls loadMore() when entries[0].isIntersecting.
  //                     Observe sentinelRef.current. Disconnect in cleanup.
    useEffect(() => {
    if (!sentinelRef.current) return undefined;
    const observer = new IntersectionObserver((entries) => {
      if (entries[0].isIntersecting) loadMore();
    }, { threshold: 0.1 });
    observer.observe(sentinelRef.current);
    return () => observer.disconnect();
  }, [loadMore]);
  return sentinelRef;
}
