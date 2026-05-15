import { useCallback, useEffect, useRef, useState } from "react";

import { isBackendUnavailableError } from "@/application/errors";
import { useApplicationServices } from "@/application/ApplicationServicesContext";
import type { AnalysisJob } from "@/domain/repositoryAnalysis";

export const DEFAULT_JOB_POLL_INTERVAL_MS = 5000;

export interface AnalysisJobState {
  data: AnalysisJob | null;
  loading: boolean;
  error: unknown;
  stale: boolean;
  polling: boolean;
  reload: () => void;
}

export const useAnalysisJob = (
  analysisRunId: string,
  options: { pollIntervalMs?: number } = {}
): AnalysisJobState => {
  const pollIntervalMs =
    options.pollIntervalMs ?? DEFAULT_JOB_POLL_INTERVAL_MS;
  const services = useApplicationServices();
  const [data, setData] = useState<AnalysisJob | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<unknown>(null);
  const [stale, setStale] = useState(false);
  const [polling, setPolling] = useState(false);
  const [reloadToken, setReloadToken] = useState(0);
  const lastData = useRef<AnalysisJob | null>(null);

  const reload = useCallback(() => {
    setReloadToken((current) => current + 1);
  }, []);

  useEffect(() => {
    const controller = new AbortController();
    let active = true;
    let timer: number | null = null;

    const load = (isPoll: boolean) => {
      if (!isPoll) {
        setLoading(true);
      }

      services.repositoryAnalysis
        .getAnalysisJob(analysisRunId, controller.signal)
        .then((job) => {
          if (!active) {
            return;
          }

          lastData.current = job;
          setData(job);
          setError(null);
          setStale(false);
          setLoading(false);
          setPolling(!job.status.terminal);

          if (!job.status.terminal) {
            timer = window.setTimeout(() => load(true), pollIntervalMs);
          }
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
          setLoading(false);
          setPolling(false);
        });
    };

    load(false);

    return () => {
      active = false;
      controller.abort();

      if (timer !== null) {
        window.clearTimeout(timer);
      }
    };
  }, [analysisRunId, pollIntervalMs, reloadToken, services]);

  return {
    data,
    loading,
    error,
    stale,
    polling,
    reload
  };
};
