package de.burger.forensics.analytics.services.cliclient.adapter.in.cli;

sealed interface CliClientCommand permits GatewaySubmitCliCommand, HelpCommand {
}
