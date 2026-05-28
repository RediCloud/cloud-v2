# AGENT.md

---

## Workflow Orchestration

### 1. Plan Node Default
- Enter plan mode for ANY non-trivial task (3+ steps or architectural decisions)
- If something goes sideways, STOP and re-plan immediately - don't keep pushing
- Use plan mode for verification steps, not just building
- Write detailed specs upfront to reduce ambiguity

### 2. Subagent Strategy
- Use subagents liberally to keep main context window clean
- Offload research, exploration, and parallel analysis to subagents
- For complex problems, throw more compute at it via subagents
- One tack per subagent for focused execution

### 3. Self-Improvement Loop
- After ANY correction from the user: update `tasks/lessons.md` with the pattern
- Write rules for yourself that prevent the same mistake
- Ruthlessly iterate on these lessons until mistake rate drops
- Review lessons at session start for relevant project

### 4. Verification Before Done
- Never mark a task complete without proving it works
- Diff behavior between main and your changes when relevant
- Ask yourself: "Would a staff engineer approve this?"
- Run tests, check logs, demonstrate correctness

### 5. Demand Elegance (Balanced)
- For non-trivial changes: pause and ask "is there a more elegant way?"
- If a fix feels hacky: "Knowing everything I know now, implement the elegant solution"
- Skip this for simple, obvious fixes - don't over-engineer
- Challenge your own work before presenting it

### 6. Autonomous Bug Fizing
- When given a bug report: just fix it. Don't ask for hand-holding
- Point at logs, errors, failing tests - then resolve them
- Zero context switching required from the user
- Go fix failing CI tests without being told how

### Task Management

1. **Plan First**: Write plan to `tasks/todo.md` with checkable items
2. **Verify Plan**: Check in before starting implementation
3. **Track Progress**: Mark items complete as you go
4. **Explain Changes**: High-level summary at each step
5. **Document Results**: Add review section to `tasks/todo.md`
6. **Capture Lessons**: Update `tasks/lessons.md` after corrections

### Core Principles

- **Simplicity First**: Make every change as simple as possible. Impact minimal code.
- **No Laziness**: Find root causes. No temporary fixes. Senior developer standards.
- **Minimat Impact**: Changes should only touch what's necessary. Avoid introducing bugs.

---

## Development Workflow

### Branch model

- **`main`** — stable, production-ready. Only receives PRs from `dev`, never direct commits.
- **`dev`** — integration branch for features and fixes. Regularly merged into `main` when stable.
- **Feature / fix branches** — short-lived, branched from `dev`.

---

## Conventions

### Git
- Conventional commits: `feat:`, `fix:`, `refactor:`, `chore:`
- all lowercase
- No commit descriptions, just a small message, no co-authors, no emojis
- Do **not** include the GitHub issue number in the commit message (e.g. `(#122)` is wrong)
- Branch convention: `<type>/<short-description>` (type: feat, fix, refactor, chore)
- Never commit `.env` files or private certificates
- **Do not include the project name in branch names, commit subjects, or issue titles** — the repo *is* the project, it is redundant. Use the feature/component name directly (`feat/updater-foundation`, not `feat/project-updater-foundation`; `Project Phase 1: Foundation`, not `Project Updater Phase 1: Foundation`). "Argus" remains acceptable in prose inside issue bodies, commit descriptions, plans, specs, and docs where it refers to the system as a named noun.
- Examples: `feat(agent): add ws reconnect logic`, `chore: add pyproject.toml for agent`

---

## Build System

- **Gradle 9.5.1** with **Kotlin 2.3.21** (K2 compiler)
- **JVM target: Java 8** — the cloud and all connectors must run on Java 8+
- Dependencies are managed via **Gradle Version Catalog** at `gradle/libs.versions.toml`
- Project version is defined in `gradle.properties` (`cloudVersion`)
- The `allprojects {}` block in root `build.gradle.kts` applies common config; `libs` is accessed via `rootLibs` (captured before the block)
- **LibLoader plugin** (`dev.redicloud.libloader`) handles runtime dependency loading — `doBootstrapShade` is `false` in root, subprojects explicitly `shade(libs.libloader.bootstrap)`
- **Shadow plugin** (`com.gradleup.shadow`) is used in `bukkit-connector` for netty/gson relocation
- `bukkit-legacy` shades logback 1.3.x (`libs.logback.core.legacy` / `libs.logback.classic.legacy`) for Java 8 compat
- `velocity-connector` uses `kotlin("kapt")` (not `alias()`) because the Kotlin plugin is already on the classpath
- **Detekt** (`io.gitlab.arturbosch.detekt`) is applied to all subprojects for static analysis

### CI

- GitHub Actions with **GitHub-hosted runners** (`ubuntu-latest`)
- JDK 21 (temurin) for builds
- `gradle/actions/setup-gradle@v4` for Gradle setup
- **`ci.yml`** — runs on push/PR to `dev` and `main`: build + test + detekt
- **`release.yml`** — manual dispatch with branch choice (`dev` or `main`): build + publish + artifact upload
