# Releasing

Each library is released on its own, by pushing a tag with its prefix. Versions follow [semantic versioning](https://semver.org); while they're below 1.0, a minor version may change the API.

| Library | Tag | Published to | Workflow |
|---|---|---|---|
| Kotlin/JVM | `kotlin/v0.1.0` | Maven Central, `io.github.rohang27:thesportsdb-client` | `release-kotlin.yml` |
| PHP | `php/v0.1.0` | Packagist, `rohang27/thesportsdb-client` (from the mirror [thesportsdb-client-php](https://github.com/RohanG27/thesportsdb-client-php)) | `php-split.yml` |

## Releasing a version

1. Update the library's changelog: change `(unreleased)` to the release date.
2. Commit, then push to `main` and wait for CI to pass.
3. Tag the commit and push the tag:

   ```sh
   git tag kotlin/v0.1.0 && git push origin kotlin/v0.1.0
   ```

- **Kotlin:** the workflow runs the tests, then signs and uploads to Maven Central and releases the upload. It shows up on [central.sonatype.com](https://central.sonatype.com/namespace/io.github.rohang27) within minutes, and in Gradle and Maven searches within about an hour. **A version on Maven Central can never be changed or deleted.** To fix a bad release, publish a new version.
- **PHP:** the workflow pushes the tag to the mirror as `v0.1.0`, and Packagist picks it up within a minute. Packagist versions can be deleted, but anyone who already installed one keeps it.

## One-time setup (done once per maintainer)

**Maven Central**
1. Sign in at [central.sonatype.com](https://central.sonatype.com) with GitHub. The `io.github.rohang27` namespace is then verified automatically.
2. Under **View Account → Generate User Token**, create a token, then store it:
   `gh secret set MAVEN_CENTRAL_USERNAME` and `gh secret set MAVEN_CENTRAL_PASSWORD`.
3. Create the signing key: `tools/setup-release.sh signing-key`. This sets `MAVEN_SIGNING_KEY` and `MAVEN_SIGNING_KEY_PASSWORD`, and uploads the public key to the keyservers Maven Central checks.

**Packagist**
1. Create the mirror and its deploy key: `tools/setup-release.sh php-split`. Then fill the mirror: `gh workflow run php-split.yml`.
2. Sign in at [packagist.org](https://packagist.org) with GitHub. Submit `https://github.com/RohanG27/thesportsdb-client-php`. Signing in with GitHub makes Packagist update automatically on every push.

## Not yet automated

- **PyPI** (`thesportsdb-client`) and **npm** (`thesportsdb-client`, `thesportsdb-graphql-schema`). Both support trusted publishing from GitHub Actions, so they can publish without storing tokens.
