import type { DiagnosticMessage } from "@/domain/diagnostic";

export type DatabaseSettingsAvailability = "AVAILABLE" | "UNAVAILABLE" | "UNKNOWN";
export type DatabaseSettingsValidationStatus =
  | "VALID"
  | "INVALID"
  | "UNREACHABLE"
  | "AUTHENTICATION_FAILED"
  | "UNSUPPORTED"
  | "UNKNOWN";

export interface DatabaseSettingsView {
  engine: string;
  host: string;
  port: number;
  databaseName: string;
  username: string;
  authenticationConfigured: boolean;
  schema: string;
  sslMode: string;
  configurationSource: string;
  applyMode: string;
  hotApplySupported: boolean;
}

export interface DatabaseSettingsStatus {
  settings: DatabaseSettingsView;
  status: DatabaseSettingsAvailability;
  diagnostics: DiagnosticMessage[];
}

export interface DatabaseSettingsValidationResult {
  settings: DatabaseSettingsView;
  validationStatus: DatabaseSettingsValidationStatus;
  applyMode: string;
  hotApplySupported: boolean;
  diagnostics: DiagnosticMessage[];
}

export interface GetDatabaseSettingsCommand {
  operatorToken: string;
}

export interface ValidateDatabaseSettingsCommand {
  operatorToken: string;
  host: string;
  port: number;
  databaseName: string;
  username: string;
  password: string;
  schema: string;
  sslMode: string;
}
