package de.burger.forensics.analytics.cli;

sealed interface CliCommand permits AnalyzeCommand, EngineRequestImportCommand, HelpCommand {
}
