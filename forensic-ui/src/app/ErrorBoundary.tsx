import { Component, type ErrorInfo, type PropsWithChildren, type ReactNode } from "react";

import { sanitizeDiagnosticText } from "@/shared/safeText";

interface ErrorBoundaryState {
  error: Error | null;
}

export class ErrorBoundary extends Component<
  PropsWithChildren<{ fallback?: (error: Error) => ReactNode }>,
  ErrorBoundaryState
> {
  state: ErrorBoundaryState = {
    error: null
  };

  static getDerivedStateFromError(error: Error): ErrorBoundaryState {
    return { error };
  }

  componentDidCatch(error: Error, errorInfo: ErrorInfo) {
    console.error("Route rendering failed", {
      message: sanitizeDiagnosticText(error.message),
      componentStack: sanitizeDiagnosticText(errorInfo.componentStack)
    });
  }

  render() {
    if (this.state.error) {
      return this.props.fallback ? (
        this.props.fallback(this.state.error)
      ) : (
        <div className="panel state-panel danger" role="alert">
          <span className="eyebrow">UI failure</span>
          <h2>View unavailable</h2>
          <p>{sanitizeDiagnosticText(this.state.error.message)}</p>
        </div>
      );
    }

    return this.props.children;
  }
}

export const RouteBoundary = ({ children }: PropsWithChildren) => (
  <ErrorBoundary>{children}</ErrorBoundary>
);
