import { useCallback } from "react";

import { useApplicationServices } from "@/application/ApplicationServicesContext";
import { useAsyncResource } from "@/application/hooks/useAsyncResource";

export const useDiagnostics = () => {
  const services = useApplicationServices();
  const loader = useCallback(
    (signal: AbortSignal) => services.diagnostics.collectDiagnostics(signal),
    [services]
  );

  return useAsyncResource(loader, (items) => items.length === 0);
};
