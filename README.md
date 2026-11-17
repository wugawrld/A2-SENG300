# SENG 300 Assignment 2

This is Group 8's room booking testing project.

## Repository

[wugawrld/A2-SENG300](https://github.com/wugawrld/A2-SENG300)

## Group 8

| Name | UCID |
| --- | --- |
| Raman Raut | 30247548 |
| Sardar Waheed | 30096015 |
| AbduMalek Ben Issa | 30240712 |
| Divakar Gaba | 30242369 |
| Saarim Ali | 30211679 |
| Selina Vuong | 30241862 |
| Abdullah Warraich | 30245056 |

## Requirements

Use JDK 17. Check your setup with `java -version` and `javac -version`.

## Run

Open a terminal at the repo root. The three Maven commands below run the tests, generate JaCoCo coverage, and run PIT.

**Windows PowerShell**

```powershell
cd booking-tests
.\mvnw.cmd clean test
.\mvnw.cmd clean verify
.\mvnw.cmd test-compile org.pitest:pitest-maven:mutationCoverage
```

**macOS or Linux**

```bash
cd booking-tests
./mvnw clean test
./mvnw clean verify
./mvnw test-compile org.pitest:pitest-maven:mutationCoverage
```

## Reports and evidence

* [Shared and individual reports](reports/)
* [Test plan, test map, run log, and contribution record](booking-tests/evidence/templates/)
* [Saved test output, coverage, and mutation reports](booking-tests/evidence/runs/)
* [Bug repair patch](faulty-version/repair-from-faulty.patch)
