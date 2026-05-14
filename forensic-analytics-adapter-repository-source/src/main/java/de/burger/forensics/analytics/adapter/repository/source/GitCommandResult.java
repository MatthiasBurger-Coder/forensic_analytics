package de.burger.forensics.analytics.adapter.repository.source;

record GitCommandResult(String output) {
    String trimmedOutput() {
        return output.strip();
    }
}
