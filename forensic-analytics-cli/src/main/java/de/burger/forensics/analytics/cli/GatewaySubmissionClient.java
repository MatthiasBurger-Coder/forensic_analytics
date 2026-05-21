package de.burger.forensics.analytics.cli;

interface GatewaySubmissionClient {
    GatewaySubmissionResult submit(GatewaySubmitCommand command);
}
