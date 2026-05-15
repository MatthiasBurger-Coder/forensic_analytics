import { FormEvent, useMemo, useState } from "react";
import { useNavigate } from "react-router-dom";
import { RefreshCcw, Send } from "lucide-react";

import { ApplicationError, toUserMessage } from "@/application/errors";
import { useStartRepositoryAnalysis } from "@/application/hooks/useStartRepositoryAnalysis";
import {
  REPOSITORY_ANALYSIS_SCHEMA_VERSION,
  type StartRepositoryAnalysisCommand
} from "@/domain/repositoryAnalysis";

const DEFAULT_TIMEOUT_SECONDS = 60;

export const CreateRepositoryAnalysisPage = () => {
  const navigate = useNavigate();
  const starter = useStartRepositoryAnalysis();
  const initialRequestId = useMemo(() => createRequestId(), []);
  const [requestId, setRequestId] = useState(initialRequestId);
  const [schemaVersion, setSchemaVersion] = useState(
    REPOSITORY_ANALYSIS_SCHEMA_VERSION
  );
  const [repositoryUrl, setRepositoryUrl] = useState("");
  const [provider, setProvider] = useState("");
  const [branch, setBranch] = useState("main");
  const [commit, setCommit] = useState("");
  const [buildTool, setBuildTool] = useState("gradle");
  const [buildId, setBuildId] = useState("manual-ui");
  const [rootProjectName, setRootProjectName] = useState("");
  const [declaredModules, setDeclaredModules] = useState("");
  const [attributes, setAttributes] = useState("");
  const [timeoutSeconds, setTimeoutSeconds] = useState(DEFAULT_TIMEOUT_SECONDS);
  const [validationError, setValidationError] = useState<ApplicationError | null>(
    null
  );

  const onSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setValidationError(null);

    const command = buildCommand();
    const validation = validateCommand(command);

    if (validation) {
      setValidationError(validation);
      return;
    }

    try {
      const analysis = await starter.submit(command);
      if (analysis.analysisRunId) {
        navigate(`/analysis-jobs/${analysis.analysisRunId}`);
      }
    } catch {
      // The hook exposes a sanitized error for the form panel.
    }
  };

  const buildCommand = (): StartRepositoryAnalysisCommand => ({
    requestId: requestId.trim(),
    schemaVersion: schemaVersion.trim(),
    repositoryUrl: repositoryUrl.trim(),
    provider: optionalText(provider),
    branch: optionalText(branch),
    commit: optionalText(commit),
    buildContext: {
      buildTool: buildTool.trim(),
      buildId: buildId.trim(),
      rootProjectName: optionalText(rootProjectName),
      declaredModules: parseLines(declaredModules),
      attributes: parseAttributes(attributes)
    },
    workspacePolicy: {
      ephemeral: false,
      allowShallowClone: false,
      allowPartialClone: false,
      allowSparseCheckout: false,
      timeoutSeconds,
      maxWorkspaceBytes: 0
    }
  });

  const error = validationError ?? starter.error;

  return (
    <section className="page">
      <header className="page-header">
        <div>
          <span className="eyebrow">Repository analysis</span>
          <h1>Register and prepare session</h1>
        </div>
      </header>

      <form className="form-grid" onSubmit={onSubmit}>
        <section className="panel form-section">
          <div className="panel-header">
            <div>
              <span className="eyebrow">Command</span>
              <h2>Repository target</h2>
            </div>
          </div>
          <label>
            Repository URL
            <input
              required
              value={repositoryUrl}
              onChange={(event) => setRepositoryUrl(event.target.value)}
              placeholder="https://example.invalid/project.git"
            />
          </label>
          <div className="form-row two">
            <label>
              Provider
              <input
                value={provider}
                onChange={(event) => setProvider(event.target.value)}
                placeholder="git"
              />
            </label>
            <label>
              Workspace name
              <input disabled value="" placeholder="Not submitted" />
            </label>
          </div>
          <div className="form-row two">
            <label>
              Branch
              <input
                value={branch}
                onChange={(event) => setBranch(event.target.value)}
                placeholder="main"
              />
            </label>
            <label>
              Commit
              <input
                value={commit}
                onChange={(event) => setCommit(event.target.value)}
                placeholder="Resolved commit hash"
              />
            </label>
          </div>
          <div className="form-row two">
            <label>
              Request ID
              <span className="input-with-button">
                <input
                  required
                  value={requestId}
                  onChange={(event) => setRequestId(event.target.value)}
                />
                <button
                  className="icon-button"
                  type="button"
                  title="Generate request ID"
                  onClick={() => setRequestId(createRequestId())}
                >
                  <RefreshCcw size={16} aria-hidden="true" />
                </button>
              </span>
            </label>
            <label>
              Schema version
              <input
                required
                value={schemaVersion}
                onChange={(event) => setSchemaVersion(event.target.value)}
              />
            </label>
          </div>
        </section>

        <section className="panel form-section">
          <div className="panel-header">
            <div>
              <span className="eyebrow">Context</span>
              <h2>Build context</h2>
            </div>
          </div>
          <div className="form-row two">
            <label>
              Build tool
              <input
                required
                value={buildTool}
                onChange={(event) => setBuildTool(event.target.value)}
              />
            </label>
            <label>
              Build ID
              <input
                required
                value={buildId}
                onChange={(event) => setBuildId(event.target.value)}
              />
            </label>
          </div>
          <label>
            Root project name
            <input
              value={rootProjectName}
              onChange={(event) => setRootProjectName(event.target.value)}
            />
          </label>
          <label>
            Declared modules
            <textarea
              rows={4}
              value={declaredModules}
              onChange={(event) => setDeclaredModules(event.target.value)}
              placeholder=":app"
            />
          </label>
          <label>
            Attributes
            <textarea
              rows={4}
              value={attributes}
              onChange={(event) => setAttributes(event.target.value)}
              placeholder="key=value"
            />
          </label>
        </section>

        <section className="panel form-section">
          <div className="panel-header">
            <div>
              <span className="eyebrow">Workspace</span>
              <h2>Policy</h2>
            </div>
          </div>
          <div className="form-row two">
            <label>
              Timeout seconds
              <input
                min={0}
                type="number"
                value={timeoutSeconds}
                onChange={(event) =>
                  setTimeoutSeconds(Number(event.target.value))
                }
              />
            </label>
          </div>
        </section>

        <section className="panel form-section submit-panel">
          {error ? (
            <div className="notice danger" role="alert">
              {toUserMessage(error)}
            </div>
          ) : null}
          <button
            className="button primary"
            type="submit"
            disabled={starter.submitting}
          >
            <Send size={16} aria-hidden="true" />
            {starter.submitting ? "Registering" : "Register session"}
          </button>
        </section>
      </form>
    </section>
  );
};

const validateCommand = (
  command: StartRepositoryAnalysisCommand
): ApplicationError | null => {
  if (!command.branch && !command.commit) {
    return new ApplicationError(
      "VALIDATION_ERROR",
      "Branch or commit is required before registering a repository analysis session."
    );
  }

  return null;
};

const optionalText = (value: string): string | null =>
  value.trim() ? value.trim() : null;

const parseLines = (value: string): string[] =>
  value
    .split(/\r?\n|,/)
    .map((item) => item.trim())
    .filter(Boolean);

const parseAttributes = (value: string): Record<string, string> =>
  Object.fromEntries(
    value
      .split(/\r?\n/)
      .map((line) => line.trim())
      .filter(Boolean)
      .map((line) => {
        const separator = line.indexOf("=");
        if (separator === -1) {
          return [line, "true"];
        }

        return [
          line.slice(0, separator).trim(),
          line.slice(separator + 1).trim()
        ];
      })
      .filter(([key, itemValue]) => key && itemValue)
  );

const createRequestId = (): string => {
  if (globalThis.crypto?.randomUUID) {
    return globalThis.crypto.randomUUID();
  }

  return `ui-${Date.now().toString(36)}`;
};
