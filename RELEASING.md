# Releasing

Each library is released on its own, by pushing a tag with its prefix. Versions follow [semantic versioning](https://semver.org); while they're below 1.0, a minor version may change the API.

| Library | Tag | Published to | Workflow |
|---|---|---|---|
| Kotlin/JVM | `kotlin/v0.1.0` | Maven Central, `io.github.rohang27:thesportsdb-client` | `release-kotlin.yml` |
| PHP | `php/v0.1.0` | Packagist, `rohang27/thesportsdb-client` (from the mirror [thesportsdb-client-php](https://github.com/RohanG27/thesportsdb-client-php)) | `php-split.yml` |
| Python | `python/v0.1.0` | PyPI, `thesportsdb-client` | `release-python.yml` |
| JavaScript/TypeScript | `javascript/v0.1.0` | npm, `thesportsdb-client` | `release-npm.yml` |
| GraphQL schema | `graphql/v0.1.0` | npm, `thesportsdb-graphql-schema` | `release-npm.yml` |

## Releasing a version

1. Set the version and date it in the changelog: `version` in `package.json` (and `package-lock.json`) for npm, `__version__` in `src/thesportsdb_client/__init__.py` for Python. Kotlin takes its version from the tag; PHP from the mirror's tag. The Python and npm workflows fail if the tag and the version differ.
2. Commit, then push to `main` and wait for CI to pass.
3. Tag the commit and push the tag:

   ```sh
   git tag kotlin/v0.1.0 && git push origin kotlin/v0.1.0
   ```

- **Kotlin:** the workflow runs the tests, then signs and uploads to Maven Central and releases the upload. It shows up on [central.sonatype.com](https://central.sonatype.com/namespace/io.github.rohang27) within minutes, and in Gradle and Maven searches within about an hour. **A version on Maven Central can never be changed or deleted.** To fix a bad release, publish a new version.
- **Python and npm:** the workflow runs the tests, checks the version, and publishes. PyPI and npm versions can't be re-uploaded once published (npm allows unpublishing only within 72 hours).
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

**PyPI** (trusted publishing: no token is stored)
1. On [pypi.org](https://pypi.org), go to **Your account → Publishing → Add a new pending publisher → GitHub**. Fill in: PyPI project name `thesportsdb-client`, owner `RohanG27`, repository `thesportsdbLibrary`, workflow `release-python.yml`, environment `pypi`.
2. The first `python/v…` tag creates the project on PyPI.

**npm** (trusted publishing: no token is stored)
1. npm only lets you set up trusted publishing for a package that already exists, so publish each package's first version from your machine:
   `npm login`, then `npm publish --access public` in `javascript/` and in `graphql/`. (`prepack` builds first.)
2. On [npmjs.com](https://www.npmjs.com), open each package → **Settings → Trusted Publisher → GitHub Actions**. Fill in: organization or user `RohanG27`, repository `thesportsdbLibrary`, workflow `release-npm.yml`, environment `npm`.
3. Optionally, under **Publishing access**, choose *Require two-factor authentication and disallow tokens*, so only the workflow can publish.
