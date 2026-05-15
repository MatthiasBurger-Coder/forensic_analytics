import {
  createContext,
  type PropsWithChildren,
  useContext
} from "react";

import type { ApplicationServices } from "@/application/createApplicationServices";

const ApplicationServicesContext = createContext<ApplicationServices | null>(
  null
);

export const ApplicationServicesProvider = ({
  services,
  children
}: PropsWithChildren<{ services: ApplicationServices }>) => (
  <ApplicationServicesContext.Provider value={services}>
    {children}
  </ApplicationServicesContext.Provider>
);

export const useApplicationServices = (): ApplicationServices => {
  const services = useContext(ApplicationServicesContext);

  if (!services) {
    throw new Error("Application services are not configured.");
  }

  return services;
};
