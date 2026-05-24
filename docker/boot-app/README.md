# Retired Boot App Container

This directory is retained only as a retirement marker for the former
monolith Boot container documentation. The current Gradle model contains
service roots under `services:*`, and service-local Dockerfiles live with
their owning service directories.

Do not use this directory as an executable container target. Add or update
runtime documentation in the owning service or deployment documentation after
the corresponding service build, container image and health behavior are
verified.
