const MAX_DIAGNOSTIC_LENGTH = 2000;

const SECRET_ASSIGNMENT =
  /\b(token|secret|password|passwd|api[_-]?key|credential|authorization)\b\s*[:=]\s*["']?[^"'\s,;]+/gi;
const CREDENTIAL_URL = /\bhttps?:\/\/[^\s/@:]+(?::[^\s/@]+)?@[^\s<>"']+/gi;
const JDBC_H2_URL = /\bjdbc:h2:[^\s<>"']+/gi;
const RAW_STREAM = /\b(?:raw\s+)?std(?:out|err)\b/gi;
const REPOSITORY_STORAGE =
  /\b(?:repository-source-data|repository-workspaces)\b[^\s<>"']*/gi;
const WINDOWS_PATH = /[A-Za-z]:\\[^\s<>"']+/g;
const UNIX_PRIVATE_PATH =
  /\/(?:Users|home|mnt|var|tmp|etc|root)\/[^\s<>"']+/g;
const STACK_FRAME = /^\s*at\s+.+$/gim;

export const sanitizeDiagnosticText = (value: unknown): string => {
  const text = String(value ?? "")
    .slice(0, MAX_DIAGNOSTIC_LENGTH)
    .replace(CREDENTIAL_URL, "[credential-url-redacted]")
    .replace(JDBC_H2_URL, "[local-database-url]")
    .replace(SECRET_ASSIGNMENT, "$1=[redacted]")
    .replace(REPOSITORY_STORAGE, "[repository-storage-redacted]")
    .replace(WINDOWS_PATH, "[local-path]")
    .replace(UNIX_PRIVATE_PATH, "[local-path]")
    .replace(RAW_STREAM, "[stream-redacted]")
    .replace(STACK_FRAME, "[stack-frame-redacted]");

  return text.trim() || "No diagnostic text was provided.";
};
