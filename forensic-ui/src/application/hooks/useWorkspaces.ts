import { useCallback } from "react";

import { useApplicationServices } from "@/application/ApplicationServicesContext";
import { useAsyncResource } from "@/application/hooks/useAsyncResource";

export const useWorkspaces = () => {
  const services = useApplicationServices();
  const loader = useCallback(
    (signal: AbortSignal) => services.workspaces.listWorkspaces(signal),
    [services]
  );

  return useAsyncResource(loader, (items) => items.length === 0);
};
