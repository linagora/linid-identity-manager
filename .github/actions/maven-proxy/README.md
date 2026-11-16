# Maven Central proxy

This composite action writes a private settings file under `RUNNER_TEMP` without writing to GitHub environment files.
Call Maven with `-s "$RUNNER_TEMP/maven-settings.xml"`. Configure the organization Actions secrets
`MAVEN_PROXY_USERNAME` and `MAVEN_PROXY_PASSWORD` and grant this repository access.

With both credentials present, Central is mirrored to `https://maven.linagora.com/central/` using the read-only CI
account. Other repository IDs remain unchanged. If neither credential is available (for example, a fork PR), an empty
settings file preserves direct Central access. A partially configured credential pair fails explicitly.

Run this action right before the first step that needs the file, and remove `$RUNNER_TEMP/maven-settings.xml` in an
`always()` step right after the last one, so that the other steps cannot read it.

Docker builds receive the file as the `maven_settings` BuildKit secret, which only the API and Sync API Dockerfiles
mount. The Taskfile passes it to every image build when `MAVEN_SETTINGS_FILE` is set on the build step to
`${{ runner.temp }}/maven-settings.xml`, and `docker/build-push-action` through its `secret-files` input. Local builds
without `MAVEN_SETTINGS_FILE` continue to use the usual Maven configuration. Credentials are never passed as Docker
build arguments or copied into image layers.
