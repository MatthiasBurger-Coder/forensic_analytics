package de.burger.forensics.analytics.services.cliclient.adapter.in.cli;

import de.burger.forensics.analytics.services.cliclient.domain.CliClientSubmissionCommand;

record GatewaySubmitCliCommand(CliClientSubmissionCommand command) implements CliClientCommand {
}
