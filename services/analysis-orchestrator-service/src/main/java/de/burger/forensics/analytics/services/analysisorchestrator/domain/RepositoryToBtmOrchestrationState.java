package de.burger.forensics.analytics.services.analysisorchestrator.domain;

public enum RepositoryToBtmOrchestrationState {
    ACCEPTED,
    WAITING_FOR_REPOSITORY,
    READY_FOR_BTM_DELIVERY,
    INCOMPLETE,
    FAILED
}
