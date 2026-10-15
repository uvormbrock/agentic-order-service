.DEFAULT_GOAL := help

SCOPE ?= all
PROJECT_ROOT := $(abspath $(dir $(lastword $(MAKEFILE_LIST))))
MUTATION_EVIDENCE_ROOT ?= $(abspath $(PROJECT_ROOT)/../agentic-order-service-mutation-evidence)
export SCOPE MUTATION_EVIDENCE_ROOT JAVA_HOME

.PHONY: help mutation-test

help:
	@printf '%s\n' \
	  'make mutation-test SCOPE=order|product|customer|shared|all (default: all)' \
	  'Set JAVA_HOME to a Java 21 installation.' \
	  'Override MUTATION_EVIDENCE_ROOT to change the external evidence location.' \
	  'Requires Python 3 (standard library) to validate Surefire XML results.' \
	  'All scopes select the complete project-wide fast-test suite (*Test); *IT integration tests are excluded.'

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
