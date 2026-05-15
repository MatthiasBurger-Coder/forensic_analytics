const MAX_DIAGNOSTIC_LENGTH = 2000;

const SECRET_ASSIGNMENT =
  /\b(token|secret|password|passwd|api[_-]?key|authorization)\b\s*[:=]\s*["']?[^"'\s,;]+/gi;
const WINDOWS_PATH = /[A-Za-z]:\\[^\s<>"']+/g;
const UNIX_PRIVATE_PATH =
  /\/(?:Users|home|mnt|var|tmp|etc|root)\/[^\s<>"']+/g;
const STACK_FRAME = /^\s*at\s+.+$/gim;

export const sanitizeDiagnosticText = (value: unknown): string => {
  const text = String(value ?? "")
    .slice(0, MAX_DIAGNOSTIC_LENGTH)
    .replace(SECRET_ASSIGNMENT, "$1=[redacted]")
    .replace(WINDOWS_PATH, "[local-path]")
    .replace(UNIX_PRIVATE_PATH, "[local-path]")
    .replace(STACK_FRAME, "[stack-frame-redacted]");

  return text.trim() || "No diagnostic text was provided.";
};
