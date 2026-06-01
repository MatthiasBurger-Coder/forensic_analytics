import type {
  DatabaseSettingsStatus,
  DatabaseSettingsValidationResult,
  GetDatabaseSettingsCommand,
  ValidateDatabaseSettingsCommand
} from "@/domain/settings";

export interface SettingsPort {
  getRepositorySourceDatabaseSettings(
    command: GetDatabaseSettingsCommand,
    signal?: AbortSignal
  ): Promise<DatabaseSettingsStatus>;

  validateRepositorySourceDatabaseSettings(
    command: ValidateDatabaseSettingsCommand,
    signal?: AbortSignal
  ): Promise<DatabaseSettingsValidationResult>;
}
