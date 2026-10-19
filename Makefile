.DEFAULT_GOAL := help

SCOPE ?= all
PROJECT_ROOT := $(abspath $(dir $(lastword $(MAKEFILE_LIST))))
MUTATION_EVIDENCE_ROOT ?= $(abspath $(PROJECT_ROOT)/../agentic-order-service-mutation-evidence)
export SCOPE MUTATION_EVIDENCE_ROOT JAVA_HOME

.PHONY: help mutation-test sonar-local sonar-findings

help:
	@printf '%s\n' \
	  'make mutation-test SCOPE=order|product|customer|shared|all (default: all)' \
	  'make sonar-local (clean verify + SonarQube Cloud upload and Quality Gate; main/develop only)' \
	  'make sonar-findings (retrieve current branch findings only; no Maven or analysis submission)' \
	  'Set JAVA_HOME to a Java 21 installation.' \
	  'Override MUTATION_EVIDENCE_ROOT to change the external evidence location.' \
	  'Requires Python 3 (standard library) to validate Surefire XML results.' \
	  'All scopes select the complete project-wide fast-test suite (*Test); *IT integration tests are excluded.' \
	  'Local demo: make demo-setup, demo-run, demo-seed, demo-status, demo-db, demo-down' \
	  'Demo logging: make demo-run-logged [LOG_LEVEL=INFO|DEBUG], make demo-logs' \
	  'Demo uses ports 15432/18080 and temporary database storage; demo-down discards demo data.'

mutation-test:
	@set -eu; \
	cd "$(PROJECT_ROOT)"; \
	case "$$SCOPE" in \
	  order) classes='com.example.agenticorderservice.order.domain.*' ;; \
	  product) classes='com.example.agenticorderservice.product.*' ;; \
	  customer) classes='com.example.agenticorderservice.customer.*' ;; \
	  shared) classes='com.example.agenticorderservice.shared.*' ;; \
	  all) classes='com.example.agenticorderservice.*' ;; \
	  *) printf 'Unknown SCOPE: %s. Use order, product, customer, shared or all.\n' "$$SCOPE" >&2; exit 2 ;; \
	esac; \
	baseline_tests='com/example/agenticorderservice/**/*Test.java'; \
	pit_tests='com.example.agenticorderservice.*Test'; \
	: "$${JAVA_HOME:?Set JAVA_HOME to a Java 21 installation}"; \
	java_settings=$$("$$JAVA_HOME/bin/java" -XshowSettings:properties -version 2>&1); \
	printf '%s\n' "$$java_settings" | grep -Eq 'java.specification.version = 21[[:space:]]*$$' || \
	  { printf '%s\n' 'Java 21 is required; check JAVA_HOME.' >&2; exit 2; }; \
	mkdir -p "$$MUTATION_EVIDENCE_ROOT"; \
	evidence_root=$$(cd "$$MUTATION_EVIDENCE_ROOT" && pwd -P); \
	case "$$evidence_root/" in "$(PROJECT_ROOT)/"*) \
	  printf '%s\n' 'Evidence must be outside the project directory.' >&2; exit 2 ;; esac; \
	evidence=$$(mktemp -d "$$evidence_root/PIT-$$SCOPE-$$(date -u +%Y%m%dT%H%M%SZ)-XXXXXX"); \
	run_id=$${evidence##*/}; \
	status='FAILED (setup)'; \
	trap 'printf "Mutation scope: %s; result: %s\nEvidence: %s\n" "$$SCOPE" "$$status" "$$evidence"' 0; \
	cp -p Makefile "$$evidence/Makefile"; \
	(cd "$$evidence" && shasum -a 256 Makefile > Makefile.sha256); \
	git rev-parse HEAD > "$$evidence/revision.txt"; \
	git branch --show-current > "$$evidence/branch.txt"; \
	git status --short > "$$evidence/git-status.txt"; \
	git diff > "$$evidence/working-tree.diff"; \
	git diff --cached > "$$evidence/staged.diff"; \
	shasum -a 256 pom.xml > "$$evidence/pom.sha256"; \
	printf 'JAVA_HOME=%s\n%s\n' "$$JAVA_HOME" "$$java_settings" > "$$evidence/environment.txt"; \
	printf 'Production: %s\nSurefire baseline: %s\nPIT eligible tests: %s\n' \
	  "$$classes" "$$baseline_tests" "$$pit_tests" > "$$evidence/selection.txt"; \
	cat "$$evidence/selection.txt"; \
	printf '%s\n' 'Fast-test selection only; Spring/Mockito compatibility beyond Order is unverified.'; \
	printf "./mvnw -B -ntp -Djacoco.skip=true '-Dtest=%s' '-Dsurefire.reportNameSuffix=%s' -DfailIfNoTests=true test\n" \
	  "$$baseline_tests" "$$run_id" > "$$evidence/baseline-command.txt"; \
	start=$$(date +%s); \
	if ./mvnw -B -ntp -Djacoco.skip=true "-Dtest=$$baseline_tests" \
	    "-Dsurefire.reportNameSuffix=$$run_id" -DfailIfNoTests=true test \
	    > "$$evidence/baseline.log" 2>&1; then rc=0; else rc=$$?; fi; \
	printf '%s\n' "$$rc" > "$$evidence/baseline-exit-code.txt"; \
	printf '%s\n' "$$(( $$(date +%s) - start ))" > "$$evidence/baseline-duration-seconds.txt"; \
	status='FAILED (baseline)'; \
	mkdir "$$evidence/surefire-reports"; \
	for report in target/surefire-reports/*-"$$run_id".xml target/surefire-reports/*-"$$run_id".txt; do \
	  if [ -f "$$report" ]; then cp -p "$$report" "$$evidence/surefire-reports/"; fi; \
	done; \
	grep -E 'Tests run:|BUILD (SUCCESS|FAILURE)' "$$evidence/baseline.log" || true; \
	if python3 -c 'import json, sys, xml.etree.ElementTree as ET; from pathlib import Path; \
	  evidence = Path(sys.argv[1]); reports = sorted((evidence / "surefire-reports").glob("TEST-*.xml")); \
	  suites = [ET.parse(report).getroot() for report in reports]; \
	  counts = [{key: int(suite.attrib[key]) for key in ("tests", "failures", "errors", "skipped")} for suite in suites]; \
	  totals = {key: sum(count[key] for count in counts) for key in ("tests", "failures", "errors", "skipped")}; \
	  totals["executed"] = totals["tests"] - totals["skipped"]; \
	  valid = (bool(suites) and all(suite.tag == "testsuite" for suite in suites) \
	    and all(all(value >= 0 for value in count.values()) and count["skipped"] <= count["tests"] for count in counts) \
	    and totals["executed"] > 0 and totals["failures"] == totals["errors"] == 0); \
	  summary = dict(totals, valid=valid, suites=[suite.attrib["name"] for suite in suites]); \
	  (evidence / "baseline-summary.json").write_text(json.dumps(summary, indent=2) + "\n"); \
	  print("Baseline XML results:", json.dumps(summary)); \
	  sys.exit(0 if valid else "Baseline XML validation failed; PIT must not run.")' \
	  "$$evidence" > "$$evidence/baseline-validation.log" 2>&1; then xml_rc=0; else xml_rc=$$?; fi; \
	printf '%s\n' "$$xml_rc" > "$$evidence/baseline-validation-exit-code.txt"; \
	cat "$$evidence/baseline-validation.log"; \
	if [ "$$rc" -ne 0 ]; then printf '%s\n' 'Baseline failed; PIT was not run.' >&2; exit "$$rc"; fi; \
	if [ "$$xml_rc" -ne 0 ]; then printf '%s\n' 'Baseline XML invalid; PIT was not run.' >&2; exit "$$xml_rc"; fi; \
	printf "./mvnw -B -ntp -Pmutation-order -Djacoco.skip=true '-DtargetClasses=%s' '-DtargetTests=%s' -Dverbose=true '-DreportsDirectory=%s/pit-reports' test-compile org.pitest:pitest-maven:mutationCoverage\n" \
	  "$$classes" "$$pit_tests" "$$evidence" > "$$evidence/pit-command.txt"; \
	start=$$(date +%s); \
	if ./mvnw -B -ntp -Pmutation-order -Djacoco.skip=true \
	    "-DtargetClasses=$$classes" "-DtargetTests=$$pit_tests" -Dverbose=true \
	    "-DreportsDirectory=$$evidence/pit-reports" \
	    test-compile org.pitest:pitest-maven:mutationCoverage \
	    > "$$evidence/pit.log" 2>&1; then rc=0; else rc=$$?; fi; \
	printf '%s\n' "$$rc" > "$$evidence/pit-exit-code.txt"; \
	printf '%s\n' "$$(( $$(date +%s) - start ))" > "$$evidence/pit-duration-seconds.txt"; \
	status='FAILED (PIT)'; \
	grep -E '^>> |BUILD (SUCCESS|FAILURE)' "$$evidence/pit.log" || true; \
	if [ "$$rc" -ne 0 ]; then printf '%s\n' 'PIT failed; inspect pit.log.' >&2; exit "$$rc"; fi; \
	test -s "$$evidence/pit-reports/mutations.xml"; \
	test -s "$$evidence/pit-reports/index.html"; \
	printf '%s\n' 'Actual PIT execution counts are above; test discovery/execution details are in verbose pit.log and XML killingTest entries.' \
	  'Selection does not establish that every eligible test ran. No PostgreSQL *IT verification is claimed.'; \
	status='EXECUTED - PENDING HUMAN REVIEW'

.PHONY: demo-setup demo-run demo-seed demo-status demo-db demo-down

demo-setup demo-run demo-seed demo-status demo-db demo-down:
	@python3 demo/demo.py $(@:demo-%=%)

LOG_LEVEL ?= INFO
.PHONY: demo-run-logged demo-logs

demo-run-logged: export DEMO_HTTP_LOG_LEVEL = $(LOG_LEVEL)
demo-run-logged:
	@case "$$DEMO_HTTP_LOG_LEVEL" in INFO|DEBUG) ;; \
	  *) printf '%s\n' 'Unsupported LOG_LEVEL; use INFO or DEBUG.' >&2; exit 2 ;; esac
	@mkdir -p logs
	@bash -o pipefail -c 'python3 demo/demo.py run 2>&1 | (trap "" INT; exec tee -a logs/demo.log)'

demo-logs:
	@test -f logs/demo.log || { printf '%s\n' 'No logs/demo.log found. Start make demo-run-logged first.' >&2; exit 1; }
	@tail -f logs/demo.log

# Local branch analysis only; authentication is never passed as a Maven argument.
sonar-local:
	@set +x; set +a; set -eu; \
	sonar_token=$${SONAR_TOKEN-}; unset SONAR_TOKEN; \
	cd "$(PROJECT_ROOT)"; \
	case "$${GITHUB_EVENT_NAME-}" in pull_request*) \
	  printf '%s\n' 'sonar-local supports branch analysis only; PR context is unsupported.' >&2; exit 2 ;; esac; \
	if [ -n "$${CHANGE_ID-}" ] || [ -n "$${CI_MERGE_REQUEST_IID-}" ] || [ -n "$${SONAR_PULL_REQUEST_KEY-}" ]; then \
	  printf '%s\n' 'sonar-local supports branch analysis only; PR context is unsupported.' >&2; exit 2; fi; \
	if ! sonar_branch=$$(git symbolic-ref --quiet --short HEAD); then \
	  printf '%s\n' 'sonar-local requires an attached main or develop branch.' >&2; exit 2; fi; \
	case "$$sonar_branch" in main|develop) ;; *) \
	  printf '%s\n' 'sonar-local supports only main and develop branch analysis.' >&2; exit 2 ;; esac; \
	if [ -z "$$sonar_token" ]; then \
	  if [ "$$(uname -s)" != Darwin ]; then \
	    printf '%s\n' 'Missing SONAR_TOKEN; Keychain fallback is available only on macOS.' >&2; exit 2; fi; \
	  sonar_account=$${USER:-$$(id -un)}; \
	  if ! sonar_token=$$(security find-generic-password -a "$$sonar_account" -s agentic-order-service-sonar -w 2>/dev/null); then \
	    printf '%s\n' 'Sonar credential unavailable: Keychain access denied, approval required, or item missing. Resolve access in macOS or supply SONAR_TOKEN securely.' >&2; exit 2; fi; \
	  if [ -z "$$sonar_token" ]; then \
	    printf '%s\n' 'Sonar credential unavailable: Keychain returned an empty value.' >&2; exit 2; fi; \
	fi; \
	printf 'SonarQube Cloud: verifying local working tree and analyzing branch %s; upload requires authorization.\n' "$$sonar_branch"; \
	sonar_run=$$(python3 tools/sonar_findings.py prepare --branch "$$sonar_branch"); \
	if SONAR_TOKEN="$$sonar_token" ./mvnw -B -ntp clean verify \
	  org.sonarsource.scanner.maven:sonar-maven-plugin:5.8.0.7211:sonar \
	  -Dsonar.projectKey=uvormbrock_agentic-order-service \
	  -Dsonar.branch.name="$$sonar_branch" \
	  -Dsonar.qualitygate.wait=true \
	  -Dsonar.qualitygate.timeout=300; then sonar_rc=0; else sonar_rc=$$?; fi; \
	if SONAR_TOKEN="$$sonar_token" python3 tools/sonar_findings.py collect \
	  --branch "$$sonar_branch" --run "$$sonar_run" --maven-exit "$$sonar_rc"; \
	  then sonar_findings_rc=0; else sonar_findings_rc=$$?; fi; \
	if [ "$$sonar_rc" -ne 0 ]; then exit "$$sonar_rc"; fi; \
	exit "$$sonar_findings_rc"

# Read-only retrieval; no scanner task or analysis identity is inferred.
sonar-findings:
	@set +x; set +a; set -eu; \
	sonar_token=$${SONAR_TOKEN-}; unset SONAR_TOKEN; \
	cd "$(PROJECT_ROOT)"; \
	case "$${GITHUB_EVENT_NAME-}" in pull_request*) \
	  printf '%s\n' 'sonar-findings supports branch analysis only; PR context is unsupported.' >&2; exit 2 ;; esac; \
	if [ -n "$${CHANGE_ID-}" ] || [ -n "$${CI_MERGE_REQUEST_IID-}" ] || [ -n "$${SONAR_PULL_REQUEST_KEY-}" ]; then \
	  printf '%s\n' 'sonar-findings supports branch analysis only; PR context is unsupported.' >&2; exit 2; fi; \
	if ! sonar_branch=$$(git symbolic-ref --quiet --short HEAD); then \
	  printf '%s\n' 'sonar-findings requires an attached main or develop branch.' >&2; exit 2; fi; \
	case "$$sonar_branch" in main|develop) ;; *) \
	  printf '%s\n' 'sonar-findings supports only main and develop branch analysis.' >&2; exit 2 ;; esac; \
	if [ -z "$$sonar_token" ]; then \
	  if [ "$$(uname -s)" != Darwin ]; then \
	    printf '%s\n' 'Missing SONAR_TOKEN; Keychain fallback is available only on macOS.' >&2; exit 2; fi; \
	  sonar_account=$${USER:-$$(id -un)}; \
	  if ! sonar_token=$$(security find-generic-password -a "$$sonar_account" -s agentic-order-service-sonar -w 2>/dev/null); then \
	    printf '%s\n' 'Sonar credential unavailable: Keychain access denied, approval required, or item missing. Resolve access in macOS or supply SONAR_TOKEN securely.' >&2; exit 2; fi; \
	  if [ -z "$$sonar_token" ]; then \
	    printf '%s\n' 'Sonar credential unavailable: Keychain returned an empty value.' >&2; exit 2; fi; \
	fi; \
	SONAR_TOKEN="$$sonar_token" python3 tools/sonar_findings.py findings --branch "$$sonar_branch"
