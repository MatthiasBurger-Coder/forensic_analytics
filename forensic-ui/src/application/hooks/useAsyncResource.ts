import { useCallback, useEffect, useRef, useState } from "react";

import { isBackendUnavailableError } from "@/application/errors";

export interface AsyncResourceState<T> {
  data: T | null;
  loading: boolean;
  error: unknown;
  stale: boolean;
  empty: boolean;
  reload: () => void;
}

export const useAsyncResource = <T,>(
  loader: (signal: AbortSignal) => Promise<T>,
  isEmpty: (value: T) => boolean
): AsyncResourceState<T> => {
  const [data, setData] = useState<T | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<unknown>(null);
  const [stale, setStale] = useState(false);
  const [reloadToken, setReloadToken] = useState(0);
  const lastData = useRef<T | null>(null);

  const reload = useCallback(() => {
    setReloadToken((current) => current + 1);
  }, []);

  useEffect(() => {
    const controller = new AbortController();
    let active = true;

    setLoading(true);
    setError(null);

    loader(controller.signal)
      .then((value) => {
        if (!active) {
          return;
        }

        lastData.current = value;
        setData(value);
        setStale(false);
        setError(null);
      })
      .catch((caught) => {
        if (!active) {
          return;
        }

        const previous = lastData.current;

        if (previous !== null && isBackendUnavailableError(caught)) {
          setData(previous);
          setStale(true);
        } else {
          setData(null);
          setStale(false);
        }

        setError(caught);
      })
      .finally(() => {
        if (active) {
          setLoading(false);
        }
      });

    return () => {
      active = false;
      controller.abort();
    };
  }, [loader, reloadToken]);

  return {
    data,
    loading,
    error,
    stale,
    empty: data !== null && isEmpty(data),
    reload
  };
};
