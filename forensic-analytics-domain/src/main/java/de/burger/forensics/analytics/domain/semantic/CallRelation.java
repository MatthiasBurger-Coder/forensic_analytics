package de.burger.forensics.analytics.domain.semantic;

public record CallRelation(String callerMethodId, String calleeMethodId, String callNodeId) {
    public CallRelation {
        RequiredSemanticText.requireText(callerMethodId, "caller method id");
        RequiredSemanticText.requireText(calleeMethodId, "callee method id");
        RequiredSemanticText.requireText(callNodeId, "call node id");
    }
}
