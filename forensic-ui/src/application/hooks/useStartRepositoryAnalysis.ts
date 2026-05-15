import { useCallback, useEffect, useRef, useState } from "react";

import { useApplicationServices } from "@/application/ApplicationServicesContext";
import type {
  RepositoryAnalysis,
  StartRepositoryAnalysisCommand
} from "@/domain/repositoryAnalysis";

export const useStartRepositoryAnalysis = () => {
  const services = useApplicationServices();
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<unknown>(null);
  const inFlight = useRef<Promise<RepositoryAnalysis> | null>(null);
  const controller = useRef<AbortController | null>(null);

  useEffect(
    () => () => {
      controller.current?.abort();
    },
    []
  );

  const submit = useCallback(
    (command: StartRepositoryAnalysisCommand) => {
      if (inFlight.current) {
        return inFlight.current;
      }

      const currentController = new AbortController();
      controller.current = currentController;
      setSubmitting(true);
      setError(null);

      inFlight.current = services.repositoryAnalysis
        .startRepositoryAnalysis(command, currentController.signal)
        .catch((caught) => {
          setError(caught);
          throw caught;
        })
        .finally(() => {
          inFlight.current = null;
          controller.current = null;
          setSubmitting(false);
        });

      return inFlight.current;
    },
    [services]
  );

  return {
    submit,
    submitting,
    error
  };
};
