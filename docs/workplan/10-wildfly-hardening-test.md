# 10 - WildFly Hardening Test

## Repository

```text
https://github.com/wildfly/wildfly.git
```

## Goal

Use WildFly to evaluate the Git/workspace system under real repository load.

The hardening test measures:

- checkout time,
- workspace size,
- file count,
- detected source roots,
- timeout behavior,
- cleanup behavior.

## Scope

Allowed:

```text
clone -> checkout -> resolve commit -> detect source roots -> cleanup
```

Forbidden:

- parser execution,
- Joern execution,
- BTM generation,
- replay execution,
- graph generation,
- UI work.

## Metrics

Record:

- repository URL,
- branch or commit requested,
- resolved commit,
- clone duration,
- checkout duration,
- total workspace bytes,
- total file count,
- detected source roots,
- cleanup duration,
- timeout or failure diagnostics.

## Safety Controls

- Treat the repository as untrusted input.
- Do not run build scripts, hooks or repository tools.
- Enforce workspace root confinement.
- Enforce timeout and disk policy.
- Keep WildFly outside the default quality gate unless a dedicated opt-in profile is approved.
