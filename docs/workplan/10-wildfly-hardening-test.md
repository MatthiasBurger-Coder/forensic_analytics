# WildFly Hardening Test

WildFly is the first large-repository hardening target for workspace and Git behavior.

Repository:

```text
https://github.com/wildfly/wildfly.git
```

## Goal

Validate that the workspace and Git checkout system can handle a real, large repository before parser execution is introduced.

The hardening test checks:

- clone time
- checkout time
- workspace size
- file count
- source-root detection
- timeout behavior
- cleanup behavior
- resolved commit reporting

## Procedure

The procedure is:

```text
clone -> checkout -> resolve commit -> detect source roots -> cleanup
```

No parser, Joern, BTM, replay, LLM or report generation is executed.

## Required Metrics

Record:

- requested branch or commit
- resolved remote URL
- resolved commit
- clone duration
- checkout duration
- total duration
- workspace path
- workspace size in bytes
- file count
- detected source-root count
- timeout policy
- cleanup result

Metrics are operational hardening evidence. They are not analysis findings.

## Execution Rules

- The test is disabled by default.
- The test requires explicit opt-in.
- Disk-space expectations must be checked before execution.
- Timeout and cleanup policy must be configured before execution.
- The result must be documented even when the run times out or fails.

## Acceptance Criteria

The hardening test passes when:

- clone and checkout complete within configured limits, or timeout is reported explicitly
- resolved commit is available after successful checkout
- source-root detection completes without parser execution
- cleanup removes or retains the workspace according to policy
- no generated parser artifacts are created

## Risks

- external network instability
- GitHub rate limiting
- long clone time
- disk pressure
- path-length issues
- cleanup failure after partial clone

Any failure must be reported as an infrastructure or workspace hardening finding, not as an analysis defect.
