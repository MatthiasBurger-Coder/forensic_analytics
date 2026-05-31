import {
  type Dispatch,
  type FormEvent,
  type SetStateAction,
  useState
} from "react";
import {
  AlertTriangle,
  CheckCircle2,
  Database,
  KeyRound,
  RefreshCcw,
  ShieldCheck
} from "lucide-react";

import { useApplicationServices } from "@/application/ApplicationServicesContext";
import { ApplicationError, toUserMessage } from "@/application/errors";
import type {
  DatabaseSettingsStatus,
  DatabaseSettingsValidationResult,
  DatabaseSettingsValidationStatus,
  DatabaseSettingsView
} from "@/domain/settings";
import { DiagnosticList } from "@/widgets/DiagnosticList";

const SSL_MODES = [
  "UNSPECIFIED",
  "disable",
  "allow",
  "prefer",
  "require",
  "verify-ca",
  "verify-full"
] as const;

interface FormState {
  host: string;
  port: string;
  databaseName: string;
  username: string;
  password: string;
  schema: string;
  sslMode: string;
}

const EMPTY_FORM: FormState = {
  host: "",
  port: "5432",
  databaseName: "",
  username: "",
  password: "",
  schema: "repository_source",
  sslMode: "UNSPECIFIED"
};

export const SettingsPage = () => {
  const services = useApplicationServices();
  const [operatorToken, setOperatorToken] = useState("");
  const [form, setForm] = useState<FormState>(EMPTY_FORM);
  const [current, setCurrent] = useState<DatabaseSettingsStatus | null>(null);
  const [validation, setValidation] =
    useState<DatabaseSettingsValidationResult | null>(null);
  const [loadingCurrent, setLoadingCurrent] = useState(false);
  const [validating, setValidating] = useState(false);
  const [error, setError] = useState<unknown>(null);

  const loadCurrent = async () => {
    if (!services.settings) {
      setError(
        new ApplicationError(
          "BACKEND_UNAVAILABLE",
          "Settings services are not configured."
        )
      );
      return;
    }
    setLoadingCurrent(true);
    setError(null);
    setValidation(null);

    try {
      const status = await services.settings.getRepositorySourceDatabaseSettings({
        operatorToken
      });
      setCurrent(status);
      setForm(formFromSettings(status.settings));
    } catch (caught) {
      setError(caught);
    } finally {
      setLoadingCurrent(false);
    }
  };

  const validateCandidate = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    if (!services.settings) {
      setError(
        new ApplicationError(
          "BACKEND_UNAVAILABLE",
          "Settings services are not configured."
        )
      );
      return;
    }
    setValidating(true);
    setError(null);
    setValidation(null);

    try {
      const result =
        await services.settings.validateRepositorySourceDatabaseSettings({
          operatorToken,
          host: form.host,
          port: Number(form.port),
          databaseName: form.databaseName,
          username: form.username,
          password: form.password,
          schema: form.schema,
          sslMode: form.sslMode
        });
      setValidation(result);
    } catch (caught) {
      setError(caught);
    } finally {
      setForm((currentForm) => ({ ...currentForm, password: "" }));
      setValidating(false);
    }
  };

  const busy = loadingCurrent || validating;
  const tokenMissing = operatorToken.trim().length === 0;
  const diagnostics = [
    ...(current?.diagnostics ?? []),
    ...(validation?.diagnostics ?? [])
  ];

  return (
    <section className="page">
      <header className="page-header">
        <div>
          <span className="eyebrow">Console</span>
          <h1>Settings</h1>
        </div>
        <button
          className="button secondary"
          disabled={busy || tokenMissing}
          onClick={() => {
            void loadCurrent();
          }}
          type="button"
        >
          <RefreshCcw
            className={loadingCurrent ? "spin-icon" : undefined}
            size={16}
            aria-hidden="true"
          />
          Refresh
        </button>
      </header>

      {error ? (
        <div className="notice danger" role="alert">
          <AlertTriangle size={18} aria-hidden="true" />
          {toUserMessage(error)}
        </div>
      ) : null}

      <div className="settings-grid">
        <section className="panel settings-status-panel" aria-labelledby="settings-status-heading">
          <header className="panel-header">
            <div>
              <span className="eyebrow">Repository Source</span>
              <h2 id="settings-status-heading">Database status</h2>
            </div>
            <Database size={20} aria-hidden="true" />
          </header>

          <label>
            Operator token
            <input
              autoComplete="off"
              inputMode="text"
              onChange={(event) => setOperatorToken(event.target.value)}
              placeholder="Required"
              type="password"
              value={operatorToken}
            />
          </label>

          {current ? (
            <DatabaseSettingsSummary settings={current.settings} status={current.status} />
          ) : (
            <div className="settings-empty-state">
              <KeyRound size={22} aria-hidden="true" />
              <span>Operator token required</span>
            </div>
          )}
        </section>

        <form className="panel settings-form" onSubmit={validateCandidate}>
          <header className="panel-header">
            <div>
              <span className="eyebrow">Validation</span>
              <h2>Candidate settings</h2>
            </div>
            <ShieldCheck size={20} aria-hidden="true" />
          </header>

          <div className="form-row two">
            <label>
              Host
              <input
                autoComplete="off"
                onChange={(event) => setFormValue(setForm, "host", event.target.value)}
                required
                type="text"
                value={form.host}
              />
            </label>
            <label>
              Port
              <input
                max={65535}
                min={1}
                onChange={(event) => setFormValue(setForm, "port", event.target.value)}
                required
                type="number"
                value={form.port}
              />
            </label>
          </div>

          <div className="form-row two">
            <label>
              Database
              <input
                autoComplete="off"
                onChange={(event) =>
                  setFormValue(setForm, "databaseName", event.target.value)
                }
                required
                type="text"
                value={form.databaseName}
              />
            </label>
            <label>
              Schema
              <input
                autoComplete="off"
                onChange={(event) => setFormValue(setForm, "schema", event.target.value)}
                required
                type="text"
                value={form.schema}
              />
            </label>
          </div>

          <div className="form-row two">
            <label>
              Username
              <input
                autoComplete="off"
                onChange={(event) => setFormValue(setForm, "username", event.target.value)}
                required
                type="text"
                value={form.username}
              />
            </label>
            <label>
              Password
              <input
                autoComplete="new-password"
                onChange={(event) => setFormValue(setForm, "password", event.target.value)}
                type="password"
                value={form.password}
              />
            </label>
          </div>

          <label>
            SSL mode
            <select
              onChange={(event) => setFormValue(setForm, "sslMode", event.target.value)}
              value={form.sslMode}
            >
              {SSL_MODES.map((mode) => (
                <option key={mode} value={mode}>
                  {mode}
                </option>
              ))}
            </select>
          </label>

          <div className="button-row">
            <button className="button primary" disabled={busy || tokenMissing} type="submit">
              <ShieldCheck
                className={validating ? "spin-icon" : undefined}
                size={16}
                aria-hidden="true"
              />
              Validate
            </button>
          </div>
        </form>
      </div>

      {validation ? (
        <section
          className={`panel settings-validation settings-validation-${validation.validationStatus.toLowerCase()}`}
          aria-live="polite"
        >
          <header className="panel-header">
            <div>
              <span className="eyebrow">Result</span>
              <h2>Validation {validation.validationStatus}</h2>
            </div>
            <ValidationIcon status={validation.validationStatus} />
          </header>
          <dl className="detail-grid">
            <KeyValue label="Apply mode" value={validation.applyMode} />
            <KeyValue
              label="Hot apply"
              value={validation.hotApplySupported ? "Supported" : "Not supported"}
            />
            <KeyValue
              label="Authentication"
              value={
                validation.settings.authenticationConfigured
                  ? "Configured"
                  : "Not provided"
              }
            />
          </dl>
        </section>
      ) : null}

      {diagnostics.length > 0 ? (
        <section className="panel">
          <header className="panel-header">
            <div>
              <span className="eyebrow">Diagnostics</span>
              <h2>Settings diagnostics</h2>
            </div>
          </header>
          <DiagnosticList diagnostics={diagnostics} />
        </section>
      ) : null}
    </section>
  );
};

const DatabaseSettingsSummary = ({
  settings,
  status
}: {
  settings: DatabaseSettingsView;
  status: string;
}) => (
  <div className="settings-summary">
    <span className={`status-badge status-${status.toLowerCase()}`}>
      <span aria-hidden="true" />
      {status}
    </span>
    <dl className="detail-grid">
      <KeyValue label="Engine" value={settings.engine} />
      <KeyValue label="Host" value={settings.host} />
      <KeyValue label="Port" value={String(settings.port)} />
      <KeyValue label="Database" value={settings.databaseName} />
      <KeyValue label="Schema" value={settings.schema} />
      <KeyValue label="Username" value={settings.username} />
      <KeyValue label="SSL mode" value={settings.sslMode} />
      <KeyValue label="Source" value={settings.configurationSource} />
      <KeyValue label="Apply mode" value={settings.applyMode} />
    </dl>
  </div>
);

const KeyValue = ({ label, value }: { label: string; value: string }) => (
  <div>
    <dt>{label}</dt>
    <dd>{value || "Unavailable"}</dd>
  </div>
);

const ValidationIcon = ({
  status
}: {
  status: DatabaseSettingsValidationStatus;
}) =>
  status === "VALID" ? (
    <CheckCircle2 size={20} aria-hidden="true" />
  ) : (
    <AlertTriangle size={20} aria-hidden="true" />
  );

const formFromSettings = (settings: DatabaseSettingsView): FormState => ({
  host: settings.host,
  port: String(settings.port || 5432),
  databaseName: settings.databaseName,
  username: settings.username,
  password: "",
  schema: settings.schema,
  sslMode: settings.sslMode || "UNSPECIFIED"
});

const setFormValue = (
  setForm: Dispatch<SetStateAction<FormState>>,
  key: keyof FormState,
  value: string
) => {
  setForm((current) => ({ ...current, [key]: value }));
};
