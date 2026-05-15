import React from "react";
import ReactDOM from "react-dom/client";

import { App } from "@/app/App";
import { createApplicationServices } from "@/application/createApplicationServices";
import { createApiClient } from "@/adapters/api/apiClient";

import "./styles.css";

const services = createApplicationServices(createApiClient());

ReactDOM.createRoot(document.getElementById("root") as HTMLElement).render(
  <React.StrictMode>
    <App services={services} />
  </React.StrictMode>
);
