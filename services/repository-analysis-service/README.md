# Repository Analysis Service

## Scope

This service owns repository checkout, revision resolution, workspace
preparation, source-root detection and source snapshot handoff. Other services
receive opaque workspace and artifact references only; private service
filesystem paths are never part of the public contract.

## Runtime

- gRPC port: `9092`
- health port: `8083`
- Docker profile workspace root:
  `/var/lib/forensic-analytics/repository-workspaces`

The service accepts clean HTTPS repository URLs only. Local paths, `file:`
URLs, SSH/SCP remotes, submodules, build execution and parser execution are out
of scope for Slice 06.

Repository checkout runs in a service-owned workspace. Public responses expose
opaque workspace IDs, source snapshot IDs, relative source roots and artifact
references only. Git command output and filesystem paths are not returned in
public error descriptions.
