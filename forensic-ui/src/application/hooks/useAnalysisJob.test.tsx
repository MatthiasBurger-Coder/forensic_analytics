import { act, render, screen } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { ApplicationServicesProvider } from "@/application/ApplicationServicesContext";
import type { ApplicationServices } from "@/application/createApplicationServices";
import { ApplicationError } from "@/application/errors";
import { useAnalysisJob } from "@/application/hooks/useAnalysisJob";
import { createStatusState } from "@/domain/analysisStatus";
import type { AnalysisJob } from "@/domain/repositoryAnalysis";

describe("useAnalysisJob", () => {
  afterEach(() => {
    vi.useRealTimers();
  });

  it("stops polling after a terminal lifecycle state", async () => {
    vi.useFakeTimers();
    const services = servicesWithJobs([
      job("RUNNING", "RUNNING"),
      job("COMPLETED", "SUCCESS")
    ]);

    render(
      <ApplicationServicesProvider services={services}>
        <Probe />
      </ApplicationServicesProvider>
    );

    await flush();
    expect(services.repositoryAnalysis.getAnalysisJob).toHaveBeenCalledTimes(1);
    expect(screen.getByText("RUNNING")).toBeInTheDocument();

    await act(async () => {
      await vi.advanceTimersByTimeAsync(10);
    });
    await flush();

    expect(screen.getByText("SUCCESS")).toBeInTheDocument();

    await act(async () => {
      await vi.advanceTimersByTimeAsync(40);
    });

    expect(services.repositoryAnalysis.getAnalysisJob).toHaveBeenCalledTimes(2);
  });

  it("marks prior data stale when polling loses the backend", async () => {
    vi.useFakeTimers();
    const services = servicesWithJobs([
      job("RUNNING", "RUNNING"),
      new ApplicationError("BACKEND_UNAVAILABLE", "Backend unavailable", {
        retryable: true
      })
    ]);

    render(
      <ApplicationServicesProvider services={services}>
        <Probe />
      </ApplicationServicesProvider>
    );

    await flush();
    expect(screen.getByText("RUNNING")).toBeInTheDocument();

    await act(async () => {
      await vi.advanceTimersByTimeAsync(10);
    });
    await flush();

    expect(screen.getByText("stale")).toBeInTheDocument();
    expect(services.repositoryAnalysis.getAnalysisJob).toHaveBeenCalledTimes(2);
  });
});

const flush = () => act(async () => undefined);

const Probe = () => {
  const state = useAnalysisJob("run-1", { pollIntervalMs: 10 });

  return (
    <div>
      <span>{state.data?.status.lifecycle ?? "none"}</span>
      <span>{state.stale ? "stale" : "fresh"}</span>
    </div>
  );
};

const servicesWithJobs = (
  sequence: Array<AnalysisJob | ApplicationError>
): ApplicationServices => {
  const getAnalysisJob = vi.fn(async () => {
    const next = sequence.shift();

    if (next instanceof ApplicationError) {
      throw next;
    }

    return next ?? job("COMPLETED", "SUCCESS");
  });

  return {
    repositoryAnalysis: {
      listRepositoryAnalyses: vi.fn(),
      getAnalysisJob,
      startRepositoryAnalysis: vi.fn()
    },
    workspaces: {
      listWorkspaces: vi.fn(),
      getWorkspace: vi.fn()
    },
    diagnostics: {
      collectDiagnostics: vi.fn()
    }
  };
};

const job = (
  backendStatus: string,
  lifecycle: "RUNNING" | "SUCCESS"
): AnalysisJob => ({
  analysisRunId: "run-1",
  workspaceId: "workspace-1",
  repositoryUrl: "https://example.invalid/project.git",
  branch: "main",
  commit: null,
  resolvedCommit: null,
  checkoutStatus: "CHECKED_OUT",
  status: createStatusState(backendStatus, lifecycle),
  sourceRoots: [],
  diagnostics: [],
  createdAt: null,
  startedAt: null,
  lastUpdatedAt: null
});
