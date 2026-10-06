# SAAS - HENRY FORD (backend)

Eight microservices for the `mackllc` pharmaceutical manufacturing platform: seven Java 17 Spring Boot services and one Node 20 service. Each has its own CI pipeline that builds, scans, signs and pushes an image to ECR, then updates the image tag in `gitops` so Argo CD deploys it.

Companion repos: [infra](https://github.com/Alexatlanta1981/infra) (AWS, Terraform, bootstrap scripts), [gitops](https://github.com/Alexatlanta1981/gitops) (desired state), [frontend](https://github.com/Alexatlanta1981/frontend) (`mackllc-ui`).

## Architecture

```
 push to develop / release/**                                   pull request
          │                                                          │
          ▼                                                          ▼
 ci-<service>.yml ──► _java-build / _node-build            ci-pr-<service>.yml ──► _java-pr-check / _node-pr-check
   test + JaCoCo >=80%, SonarCloud, OWASP Dependency Check, Trivy      (tests and scans only)
   build image ──► push ECR (tag sha-<7>) ──► Cosign keyless sign
          │
          ▼  GitHub App token
   commit new tag to gitops envs/dev ──► Argo CD syncs ──► EKS

 promote-qa.yml / promote-prod.yml: move an already-built tag to qa / prod values in gitops
```

AWS access from CI uses GitHub OIDC (no stored keys). The API gateway fronts the other services.

## Layout

| Path | Service | Port (dev) |
|---|---|---|
| `api-gateway/` | Entry point, routes `/api` | 8080 |
| `auth-service/` | Login, JWT | 8081 |
| `drug-catalog-service/` | Drug catalog (`catalog-service` in gitops) | 8082 |
| `inventory-service/` | Inventory | 8083 |
| `supplier-service/` | Suppliers | 8084 |
| `manufacturing-service/` | Manufacturing orders | 8085 |
| `qc-service/` | Quality control | 8086 |
| `notification-service/` | Notifications (Node 20) | 3000 |
| `.github/workflows/` | `_*.yml` reusable builds, `ci-*` and `ci-pr-*` per service, `promote-qa`, `promote-prod` | |

Ports are taken from the `gitops` dev values. Confirm against each service's config if they change.

## Running it

Local (per service, Java):

```bash
cd auth-service
export SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/mackllc
export SPRING_DATASOURCE_USERNAME=mackllc
export SPRING_DATASOURCE_PASSWORD=mackllc
export JWT_SECRET=local-dev-secret
mvn spring-boot:run
mvn verify                        # tests + JaCoCo coverage
mvn verify -Pintegration-tests    # integration tests only
```

Notification service: `cd notification-service && npm ci && npm test && npm start`.

In CI: push to `develop` or `release/**` runs the full pipeline for the services whose folder changed. Pull requests run checks only. Promote with the promote workflows (manual dispatch).

Required repo settings: variables `GITOPS_APP_ID`, `GITOPS_REPO`; secrets `GITOPS_APP_PRIVATE_KEY`, `AWS_ACCOUNT_ID`, `SONAR_TOKEN`. GitHub App setup: [infra runbook](https://github.com/Alexatlanta1981/infra/blob/main/docs/DEPLOY-RUNBOOK.md).

## Why it is designed this way

- **One pipeline per service, shared reusable workflows.** Services release independently; build logic lives in one place.
- **Path-filtered triggers.** Only changed services build.
- **Quality gates in CI.** Coverage 80% or more, SonarCloud, OWASP Dependency Check, Trivy. A bad build never reaches ECR.
- **Immutable `sha-<7>` tags and Cosign signing.** Every deployed image traces back to a commit and is verifiable.
- **OIDC to AWS, GitHub App to gitops.** No long-lived keys or personal tokens.
- **CI never touches the cluster.** It only commits a tag to `gitops`; Argo CD does the deploy, so Git is the audit trail and rollback is a revert.
- **Promotion moves a tag, not a rebuild.** QA and prod run the exact image tested in dev.

## Known gaps

- Trivy findings are non-blocking.
- Per-service ports and env vars are not centrally documented; the `gitops` values are the reference.
- Local run needs your own Postgres; there is no docker-compose.
