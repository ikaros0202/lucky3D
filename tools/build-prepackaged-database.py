#!/usr/bin/env python3
"""Build and verify Lucky3D's Room prepackaged database from the approved seed."""

from __future__ import annotations

import argparse
import hashlib
import json
import sqlite3
from pathlib import Path


PROJECT_ROOT = Path(__file__).resolve().parents[1]
DEFAULT_SEED = PROJECT_ROOT / "data" / "fc3d-seed.json"
DEFAULT_SCHEMA = (
    PROJECT_ROOT
    / "app"
    / "schemas"
    / "com.lucky3d.app.data.local.Lucky3dDatabase"
    / "1.json"
)
DEFAULT_OUTPUT = (
    PROJECT_ROOT / "app" / "src" / "main" / "assets" / "database" / "lucky3d.db"
)


def fingerprint(record: dict) -> str:
    payload = (
        f"{record['issue']}|{record['drawDate']}|"
        f"{''.join(str(digit) for digit in record['digits'])}|"
        f"{record['officialDetailUrl']}"
    )
    return hashlib.sha256(payload.encode("utf-8")).hexdigest().upper()


def validate_records(records: list[dict]) -> None:
    if len(records) != 3334:
        raise ValueError(f"Expected 3334 records, found {len(records)}")
    issues = [record["issue"] for record in records]
    if len(set(issues)) != len(issues):
        raise ValueError("Seed contains duplicate issues")
    if issues[0] != "2017001" or issues[-1] != "2026198":
        raise ValueError(f"Unexpected seed range: {issues[0]}..{issues[-1]}")
    for record in records:
        issue = record["issue"]
        digits = record["digits"]
        if len(issue) != 7 or not issue.isdigit():
            raise ValueError(f"Invalid issue: {issue}")
        if len(digits) != 3 or any(not isinstance(d, int) or d not in range(10) for d in digits):
            raise ValueError(f"Invalid digits for {issue}: {digits}")


def build_database(seed_path: Path, schema_path: Path, output_path: Path) -> None:
    seed_document = json.loads(seed_path.read_text(encoding="utf-8"))
    records = seed_document["draws"]
    declared_range = seed_document["range"]
    if declared_range != {
        "firstIssue": "2017001",
        "lastIssue": "2026198",
        "count": 3334,
    }:
        raise ValueError(f"Unexpected declared seed range: {declared_range}")
    validate_records(records)
    schema = json.loads(schema_path.read_text(encoding="utf-8"))["database"]

    output_path.parent.mkdir(parents=True, exist_ok=True)
    if output_path.exists():
        output_path.unlink()

    connection = sqlite3.connect(output_path)
    try:
        connection.execute("PRAGMA journal_mode=DELETE")
        connection.execute("PRAGMA foreign_keys=ON")
        for entity in schema["entities"]:
            connection.execute(
                entity["createSql"].replace("${TABLE_NAME}", entity["tableName"])
            )
            for index in entity.get("indices", []):
                connection.execute(
                    index["createSql"].replace("${TABLE_NAME}", entity["tableName"])
                )
        for query in schema["setupQueries"]:
            connection.execute(query)

        connection.executemany(
            """
            INSERT INTO draws(
                issue, drawDate, hundreds, tens, ones,
                officialDetailUrl, officialFingerprint
            ) VALUES (?, ?, ?, ?, ?, ?, ?)
            """,
            [
                (
                    record["issue"],
                    record["drawDate"],
                    record["digits"][0],
                    record["digits"][1],
                    record["digits"][2],
                    record["officialDetailUrl"],
                    fingerprint(record),
                )
                for record in records
            ],
        )
        connection.execute(
            """
            INSERT INTO sync_metadata(
                id, lastAttemptEpochMillis, lastSuccessEpochMillis,
                latestIssue, lastFailureType, correctedIssuesJson
            ) VALUES (1, NULL, NULL, ?, NULL, '[]')
            """,
            (records[-1]["issue"],),
        )
        connection.execute(f"PRAGMA user_version={schema['version']}")
        connection.commit()

        count = connection.execute("SELECT COUNT(*) FROM draws").fetchone()[0]
        first, last = connection.execute(
            "SELECT MIN(issue), MAX(issue) FROM draws"
        ).fetchone()
        integrity = connection.execute("PRAGMA integrity_check").fetchone()[0]
        if (count, first, last, integrity) != (
            3334,
            "2017001",
            "2026198",
            "ok",
        ):
            raise ValueError(
                f"Database verification failed: {count}, {first}, {last}, {integrity}"
            )
        connection.execute("VACUUM")
    finally:
        connection.close()

    digest = hashlib.sha256(output_path.read_bytes()).hexdigest().upper()
    print(f"Created {output_path}")
    print(f"Records: 3334 (2017001..2026198)")
    print(f"SHA-256: {digest}")


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--seed", type=Path, default=DEFAULT_SEED)
    parser.add_argument("--schema", type=Path, default=DEFAULT_SCHEMA)
    parser.add_argument("--output", type=Path, default=DEFAULT_OUTPUT)
    args = parser.parse_args()
    build_database(args.seed.resolve(), args.schema.resolve(), args.output.resolve())


if __name__ == "__main__":
    main()
