---
title: Testing & Submitting
---

## Validate your implementation

Run the validation script to check all endpoints return correct responses:

```bash
./scripts/validate.sh your-framework
```

This builds the Docker image, starts the container, and runs checks for every test profile listed in your `meta.json`. It verifies response bodies, status codes, content types, and anti-cheat randomized inputs.

## Run a benchmark

Test a single profile locally:

```bash
./scripts/benchmark.sh your-framework baseline
```

Run all profiles:

```bash
./scripts/benchmark.sh your-framework
```

By default, results are displayed but not saved. Add `--save` to persist results:

```bash
./scripts/benchmark.sh --save your-framework
```

## Submit a PR

Once validation passes and benchmarks run successfully:

1. Fork [HttpArena](https://github.com/MDA2AV/HttpArena)
2. Add your `frameworks/your-framework/` directory
3. Open a pull request

The PR should include:
- `Dockerfile`
- `meta.json`
- Source files for your server implementation

## GitHub Actions

HttpArena uses four GitHub Actions workflows to automate validation, benchmarking, and deployment.

### Validate Framework

**Trigger:** Automatically on every PR that modifies files under `frameworks/` or `scripts/validate.sh`.

Detects which frameworks were changed in the PR and runs `./scripts/validate.sh` against each one. If validation fails, the PR check fails — you must fix the issues before merging.

### Benchmark

**Trigger:** Automatically when a push to `main` modifies files under `frameworks/`, or manually via workflow dispatch.

When triggered automatically, it detects which frameworks changed in the latest commit and benchmarks only those. When triggered manually, you can specify:
- **Framework** — a specific framework name, or leave empty to benchmark all changed frameworks
- **Profile** — a specific test profile (e.g. `baseline`, `baseline-h2`), or leave empty to run all profiles

Results are committed and pushed to `main` automatically by the HttpArena Bot.

### Benchmark PR

**Trigger:** Manual only (workflow dispatch). Requires a PR number and framework name.

Checks out the PR branch, runs the benchmark, and posts the results as a comment on the PR. This lets maintainers benchmark a new framework submission before merging, so contributors can see how their implementation performs on the hosted runner. An optional profile parameter lets you run a specific test instead of the full suite.

### Deploy Site

**Trigger:** Automatically when a push to `main` modifies files under `site/`, or manually via workflow dispatch.

Builds the Hugo site and deploys it to GitHub Pages. This runs on GitHub-hosted Ubuntu runners (not the self-hosted runner).

## Hosted runner

The Validate, Benchmark, and Benchmark PR workflows run on a **self-hosted runner** — a dedicated bare-metal machine configured for reproducible, low-noise benchmarking. This ensures all frameworks are tested on identical hardware under controlled conditions, with CPU governors locked, background services minimized, and no resource contention from other CI jobs.

Only the Deploy Site workflow uses GitHub-hosted runners, since it only builds static HTML and doesn't need controlled hardware.
