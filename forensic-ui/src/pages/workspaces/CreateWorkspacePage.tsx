import {
  FormEvent,
  type MutableRefObject,
  useMemo,
  useRef,
  useState
} from "react";
import {
  AlertCircle,
  CheckCircle2,
  RefreshCcw,
  Save,
  Search
} from "lucide-react";

import { useApplicationServices } from "@/application/ApplicationServicesContext";
import { ApplicationError, toUserMessage } from "@/application/errors";
import type {
  BranchRefreshResult,
  Workspace,
  WorkspaceBranch,
  WorkspaceMetadata,
  WorkspacePolicy
} from "@/domain/workspace";
import { DiagnosticList } from "@/widgets/DiagnosticList";

const DEFAULT_TIMEOUT_SECONDS = 60;
const DEFAULT_MAX_WORKSPACE_BYTES = 1_073_741_824;

type OperationName = "metadata" | "save" | "refresh";

type Phase =
  | "idle"
  | "reading-metadata"
  | "ready-to-save"
  | "saving"
  | "workspace-ready"
  | "workspace-failed"
  | "refreshing"
  | "branch-up-to-date"
  | "branch-updated";

interface StableOperationKey {
  fingerprint: string;
  idempotencyKey: string;
}

export const CreateWorkspacePage = () => {
  const services = useApplicationServices();
  const [repositoryUrl, setRepositoryUrl] = useState("");
  const [selectedBranch, setSelectedBranch] = useState("");
  const [allowShallowClone, setAllowShallowClone] = useState(true);
  const [timeoutSeconds, setTimeoutSeconds] = useState(DEFAULT_TIMEOUT_SECONDS);
  const [maxWorkspaceBytes, setMaxWorkspaceBytes] = useState(
    DEFAULT_MAX_WORKSPACE_BYTES
  );
  const [metadata, setMetadata] = useState<WorkspaceMetadata | null>(null);
  const [workspace, setWorkspace] = useState<Workspace | null>(null);
  const [refreshResult, setRefreshResult] = useState<BranchRefreshResult | null>(
    null
  );
  const [phase, setPhase] = useState<Phase>("idle");
  const [busyOperation, setBusyOperation] = useState<OperationName | null>(null);
  const [error, setError] = useState<unknown>(null);
  const metadataKey = useRef<StableOperationKey | null>(null);
  const saveKey = useRef<StableOperationKey | null>(null);
  const refreshKey = useRef<StableOperationKey | null>(null);
  const repositoryUrlRef = useRef("");
  const metadataInFlight = useRef<Promise<WorkspaceMetadata> | null>(null);
  const saveInFlight = useRef<Promise<Workspace> | null>(null);
  const refreshInFlight = useRef<Promise<BranchRefreshResult> | null>(null);

  const workspacePolicy = useMemo<WorkspacePolicy>(
    () => ({
      ephemeral: false,
      allowShallowClone,
      allowPartialClone: false,
      allowSparseCheckout: false,
      timeoutSeconds,
      maxWorkspaceBytes
    }),
    [allowShallowClone, maxWorkspaceBytes, timeoutSeconds]
  );
  const primaryBranch = workspace?.branches[0] ?? null;
  const diagnostics = [
    ...(metadata?.diagnostics ?? []),
    ...(workspace?.diagnostics ?? []),
    ...(primaryBranch?.diagnostics ?? []),
    ...(refreshResult?.diagnostics ?? [])
  ];

  const previewMetadata = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();

    if (metadataInFlight.current) {
      await metadataInFlight.current;
      return;
    }

    const repository = repositoryUrl.trim();
    const validation = validateRepositoryUrl(repository);

    if (validation) {
      setError(validation);
      setPhase("workspace-failed");
      return;
    }

    setBusyOperation("metadata");
    setError(null);
    setPhase("reading-metadata");
    setWorkspace(null);
    setRefreshResult(null);
    repositoryUrlRef.current = repository;

    metadataInFlight.current = services.workspaces
      .previewMetadata({
        repositoryUrl: repository,
        correlationId: createPublicId("ui-workspace-metadata-correlation"),
        idempotencyKey: stableIdempotencyKey(
          metadataKey,
          "ui-workspace-metadata",
          repository
        )
      })
      .then((result) => {
        if (!isCurrentRepositoryRequest(repositoryUrlRef, repository)) {
          return result;
        }

        setMetadata(result);
        setSelectedBranch((current) =>
          current.trim() ? current : result.defaultBranch ?? ""
        );
        setPhase("ready-to-save");
        return result;
      })
      .catch((caught) => {
        if (!isCurrentRepositoryRequest(repositoryUrlRef, repository)) {
          return null as unknown as WorkspaceMetadata;
        }

        setError(caught);
        setPhase("workspace-failed");
        throw caught;
      })
      .finally(() => {
        metadataInFlight.current = null;
        setBusyOperation(null);
      });

    try {
      await metadataInFlight.current;
    } catch {
      // The page renders sanitized error state.
    }
  };

  const saveWorkspace = async () => {
    if (saveInFlight.current) {
      await saveInFlight.current;
      return;
    }

    const repository = repositoryUrl.trim();
    const validation =
      validateRepositoryUrl(repository) ??
      validateMetadata(metadata) ??
      validateWorkspacePolicy(workspacePolicy);

    if (validation) {
      setError(validation);
      setPhase("workspace-failed");
      return;
    }

    setBusyOperation("save");
    setError(null);
    setPhase("saving");
    setRefreshResult(null);

    const branch = optionalText(selectedBranch);
    const fingerprint = JSON.stringify({
      repositoryUrl: repository,
      selectedBranch: branch,
      workspacePolicy
    });

    saveInFlight.current = services.workspaces
      .createWorkspace({
        repositoryUrl: repository,
        selectedBranch: branch,
        workspacePolicy,
        correlationId: createPublicId("ui-workspace-save-correlation"),
        idempotencyKey: stableIdempotencyKey(
          saveKey,
          "ui-workspace-save",
          fingerprint
        )
      })
      .then((result) => {
        setWorkspace(result);
        setSelectedBranch(
          result.branches[0]?.repositoryBranch ?? branch ?? selectedBranch
        );
        setPhase(hasFailed(result) ? "workspace-failed" : "workspace-ready");
        return result;
      })
      .catch((caught) => {
        setError(caught);
        setPhase("workspace-failed");
        throw caught;
      })
      .finally(() => {
        saveInFlight.current = null;
        setBusyOperation(null);
      });

    try {
      await saveInFlight.current;
    } catch {
      // The page renders sanitized error state.
    }
  };

  const refreshBranch = async (branch: WorkspaceBranch) => {
    if (!workspace) {
      setError(
        new ApplicationError(
          "VALIDATION_ERROR",
          "A checked-out workspace branch is required before refreshing."
        )
      );
      setPhase("workspace-failed");
      return;
    }

    if (refreshInFlight.current) {
      await refreshInFlight.current;
      return;
    }

    setBusyOperation("refresh");
    setError(null);
    setPhase("refreshing");

    const fingerprint = JSON.stringify({
      workspaceId: workspace.workspaceId,
      workspaceBranchId: branch.workspaceBranchId,
      repositoryBranch: branch.repositoryBranch
    });

    refreshInFlight.current = services.workspaces
      .refreshBranch({
        workspaceId: workspace.workspaceId,
        workspaceBranchId: branch.workspaceBranchId,
        correlationId: createPublicId("ui-branch-refresh-correlation"),
        idempotencyKey: stableIdempotencyKey(
          refreshKey,
          "ui-branch-refresh",
          fingerprint
        )
      })
      .then((result) => {
        setRefreshResult(result);
        setWorkspace(applyBranchRefresh(workspace, result));
        setPhase(result.changed ? "branch-updated" : "branch-up-to-date");
        refreshKey.current = null;
        return result;
      })
      .catch((caught) => {
        setError(caught);
        setPhase("workspace-failed");
        throw caught;
      })
      .finally(() => {
        refreshInFlight.current = null;
        setBusyOperation(null);
      });

    try {
      await refreshInFlight.current;
    } catch {
      // The page renders sanitized error state.
    }
  };

  const titleValue = metadata?.workspaceTitle ?? workspace?.workspaceTitle ?? "";
  const phaseLabel = labelForPhase(phase);

  return (
    <section className="page workspace-page">
      <header className="page-header">
        <div>
          <span className="eyebrow">Workspaces</span>
          <h1>Create repository workspace</h1>
        </div>
      </header>

      <div className="workspace-layout">
        <form className="panel form-section" onSubmit={previewMetadata}>
          <div className="panel-header">
            <div>
              <span className="eyebrow">Repository</span>
              <h2>Metadata preview</h2>
            </div>
          </div>
          <label>
            Repository URL
            <input
              required
              value={repositoryUrl}
              onChange={(event) => {
                const nextRepositoryUrl = event.target.value;
                repositoryUrlRef.current = nextRepositoryUrl;
                setRepositoryUrl(nextRepositoryUrl);
                setSelectedBranch("");
                setMetadata(null);
                setWorkspace(null);
                setRefreshResult(null);
                setPhase("idle");
              }}
              placeholder="https://github.com/wildfly/wildfly.git"
            />
          </label>
          <div className="form-row two">
            <label>
              Workspace title
              <input
                aria-readonly="true"
                readOnly
                value={titleValue}
                placeholder="Unavailable"
              />
            </label>
            <label>
              Selected branch
              <input
                value={selectedBranch}
                onChange={(event) => {
                  setSelectedBranch(event.target.value);
                  setWorkspace(null);
                  setRefreshResult(null);
                  setPhase(metadata ? "ready-to-save" : "idle");
                }}
                placeholder={metadata?.defaultBranch ?? "Optional branch"}
              />
            </label>
          </div>
          <div className="button-row">
            <button
              className="button secondary"
              disabled={busyOperation !== null}
              type="submit"
            >
              <Search size={16} aria-hidden="true" />
              {busyOperation === "metadata" ? "Reading metadata" : "Preview"}
            </button>
            <button
              className="button primary"
              disabled={busyOperation !== null || metadata === null}
              onClick={saveWorkspace}
              type="button"
            >
              <Save size={16} aria-hidden="true" />
              {busyOperation === "save" ? "Saving workspace" : "Save"}
            </button>
          </div>
        </form>

        <section className="panel form-section">
          <div className="panel-header">
            <div>
              <span className="eyebrow">Policy</span>
              <h2>Checkout limits</h2>
            </div>
          </div>
          <div className="form-row two">
            <label>
              Timeout seconds
              <input
                min={1}
                type="number"
                value={timeoutSeconds}
                onChange={(event) =>
                  setTimeoutSeconds(Number(event.target.value))
                }
              />
            </label>
            <label>
              Workspace byte limit
              <input
                min={1}
                type="number"
                value={maxWorkspaceBytes}
                onChange={(event) =>
                  setMaxWorkspaceBytes(Number(event.target.value))
                }
              />
            </label>
          </div>
          <div className="toggle-grid">
            <label>
              <input
                checked={allowShallowClone}
                onChange={(event) => setAllowShallowClone(event.target.checked)}
                type="checkbox"
              />
              Shallow clone
            </label>
          </div>
        </section>
      </div>

      <section className="panel form-section" aria-live="polite">
        <div className="panel-header">
          <div>
            <span className="eyebrow">Progress</span>
            <h2>{phaseLabel}</h2>
          </div>
          {busyOperation ? <div className="spinner" aria-hidden="true" /> : null}
        </div>
        <ol className="progress-list">
          {observedProgressLabels(phase, phaseLabel).map((label) => (
            <li className={label === phaseLabel ? "active" : undefined} key={label}>
              {label}
            </li>
          ))}
        </ol>
      </section>

      {error ? (
        <div className="notice danger" role="alert">
          <AlertCircle size={18} aria-hidden="true" />
          {toUserMessage(error)}
        </div>
      ) : null}

      {metadata ? (
        <section className="panel">
          <div className="panel-header">
            <div>
              <span className="eyebrow">Resolved metadata</span>
              <h2>{metadata.repositoryName}</h2>
            </div>
            <span className="pill">{metadata.repositoryKey}</span>
          </div>
          <dl className="detail-grid">
            <div>
              <dt>Host</dt>
              <dd>{metadata.repositoryHost}</dd>
            </div>
            <div>
              <dt>Owner</dt>
              <dd>{metadata.repositoryOwner ?? "Unavailable"}</dd>
            </div>
            <div>
              <dt>Default branch</dt>
              <dd>{metadata.defaultBranch ?? "Unavailable"}</dd>
            </div>
          </dl>
        </section>
      ) : null}

      {workspace ? (
        <section className="panel">
          <div className="panel-header">
            <div>
              <span className="eyebrow">Workspace status</span>
              <h2>{workspace.workspaceTitle}</h2>
            </div>
            <span className="pill">{workspace.status}</span>
          </div>
          <dl className="detail-grid">
            <div>
              <dt>Workspace ID</dt>
              <dd>{workspace.workspaceId}</dd>
            </div>
            <div>
              <dt>Repository</dt>
              <dd>{workspace.repository.repositoryKey}</dd>
            </div>
            <div>
              <dt>Branch count</dt>
              <dd>{workspace.branches.length}</dd>
            </div>
          </dl>
          <div className="branch-grid">
            {workspace.branches.map((branch) => (
              <BranchPanel
                branch={branch}
                key={branch.workspaceBranchId}
                onRefresh={() => refreshBranch(branch)}
                refreshing={busyOperation === "refresh"}
              />
            ))}
          </div>
        </section>
      ) : null}

      {refreshResult ? (
        <div className="notice">
          <CheckCircle2 size={18} aria-hidden="true" />
          {refreshResult.changed ? "Branch updated" : "Branch up to date"}
        </div>
      ) : null}

      {diagnostics.length > 0 ? (
        <section className="panel">
          <div className="panel-header">
            <div>
              <span className="eyebrow">Diagnostics</span>
              <h2>Sanitized messages</h2>
            </div>
          </div>
          <DiagnosticList diagnostics={diagnostics} />
        </section>
      ) : null}
    </section>
  );
};

const BranchPanel = ({
  branch,
  onRefresh,
  refreshing
}: {
  branch: WorkspaceBranch;
  onRefresh: () => void;
  refreshing: boolean;
}) => (
  <article className="branch-panel">
    <header>
      <div>
        <span className="eyebrow">Branch</span>
        <h3>{branch.repositoryBranch}</h3>
      </div>
      <span className="pill">{branch.status}</span>
    </header>
    <dl className="detail-grid">
      <div>
        <dt>Branch ID</dt>
        <dd>{branch.workspaceBranchId}</dd>
      </div>
      <div>
        <dt>Commit</dt>
        <dd>{branch.resolvedCommit ?? "Unavailable"}</dd>
      </div>
      <div>
        <dt>Source snapshot</dt>
        <dd>{branch.sourceSnapshotId ?? "Unavailable"}</dd>
      </div>
    </dl>
    {branch.sourceRoots.length > 0 ? (
      <ul className="source-root-list">
        {branch.sourceRoots.map((sourceRoot) => (
          <li key={sourceRoot}>{sourceRoot}</li>
        ))}
      </ul>
    ) : null}
    <button
      className="button secondary"
      disabled={refreshing}
      onClick={onRefresh}
      type="button"
    >
      <RefreshCcw size={16} aria-hidden="true" />
      {refreshing ? "Refreshing branch" : "Update branch"}
    </button>
  </article>
);

const labelForPhase = (phase: Phase): string => {
  switch (phase) {
    case "reading-metadata":
      return "Reading metadata...";
    case "ready-to-save":
      return "Ready to save";
    case "saving":
      return "Saving workspace...";
    case "workspace-ready":
      return "Workspace ready";
    case "workspace-failed":
      return "Workspace failed";
    case "refreshing":
      return "Refreshing branch...";
    case "branch-up-to-date":
      return "Branch up to date";
    case "branch-updated":
      return "Branch updated";
    case "idle":
      return "Awaiting repository URL";
  }
};

const observedProgressLabels = (phase: Phase, currentLabel: string): string[] => {
  const labels = ["Reading metadata..."];

  if (phase !== "idle" && phase !== "reading-metadata") {
    labels.push("Ready to save");
  }

  if (
    phase === "saving" ||
    phase === "workspace-ready" ||
    phase === "workspace-failed" ||
    phase === "refreshing" ||
    phase === "branch-up-to-date" ||
    phase === "branch-updated"
  ) {
    labels.push("Saving workspace...");
  }

  if (phase === "workspace-ready") {
    labels.push("Workspace ready");
  }

  if (phase === "workspace-failed") {
    labels.push("Workspace failed");
  }

  if (
    phase === "refreshing" ||
    phase === "branch-up-to-date" ||
    phase === "branch-updated"
  ) {
    labels.push("Workspace ready", "Refreshing branch...");
  }

  if (phase === "branch-up-to-date") {
    labels.push("Branch up to date");
  }

  if (phase === "branch-updated") {
    labels.push("Branch updated");
  }

  return phase === "idle" ? [currentLabel] : Array.from(new Set(labels));
};

const stableIdempotencyKey = (
  ref: MutableRefObject<StableOperationKey | null>,
  prefix: string,
  fingerprint: string
): string => {
  if (ref.current?.fingerprint === fingerprint) {
    return ref.current.idempotencyKey;
  }

  ref.current = {
    fingerprint,
    idempotencyKey: createPublicId(prefix)
  };

  return ref.current.idempotencyKey;
};

const isCurrentRepositoryRequest = (
  ref: MutableRefObject<string>,
  repositoryUrl: string
): boolean => ref.current.trim() === repositoryUrl;

const applyBranchRefresh = (
  workspace: Workspace,
  refresh: BranchRefreshResult
): Workspace => ({
  ...workspace,
  branches: workspace.branches.map((branch) =>
    branch.workspaceBranchId === refresh.workspaceBranchId
      ? {
          ...branch,
          status: refresh.status,
          resolvedCommit: refresh.resolvedCommit,
          sourceSnapshotId: refresh.sourceSnapshotId,
          diagnostics: refresh.diagnostics
        }
      : branch
  ),
  status: refresh.status === "FAILED" ? "FAILED" : workspace.status
});

const hasFailed = (workspace: Workspace): boolean =>
  workspace.status === "FAILED" ||
  workspace.branches.some((branch) => branch.status === "FAILED");

const validateRepositoryUrl = (repositoryUrl: string): ApplicationError | null =>
  repositoryUrl
    ? null
    : new ApplicationError(
        "VALIDATION_ERROR",
        "Repository URL is required before previewing metadata."
      );

const validateMetadata = (
  metadata: WorkspaceMetadata | null
): ApplicationError | null =>
  metadata
    ? null
    : new ApplicationError(
        "VALIDATION_ERROR",
        "Repository metadata must be previewed before saving the workspace."
      );

const validateWorkspacePolicy = (
  workspacePolicy: WorkspacePolicy
): ApplicationError | null =>
  workspacePolicy.timeoutSeconds > 0 && workspacePolicy.maxWorkspaceBytes > 0
    ? null
    : new ApplicationError(
        "VALIDATION_ERROR",
        "Workspace timeout and byte limits must be positive."
      );

const optionalText = (value: string): string | null =>
  value.trim() ? value.trim() : null;

const createPublicId = (prefix: string): string => {
  if (globalThis.crypto?.randomUUID) {
    return `${prefix}-${globalThis.crypto.randomUUID()}`;
  }

  return `${prefix}-${Date.now().toString(36)}`;
};
