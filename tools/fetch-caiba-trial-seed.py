#!/usr/bin/env python3
"""Fetch or verify the approved Caiba 55125 bundled trial-number seed."""

from __future__ import annotations

import argparse
import datetime as dt
import hashlib
import json
import re
import sys
import urllib.request
from html.parser import HTMLParser
from pathlib import Path
from urllib.parse import urlparse


SOURCE = "CAIBA_55125"
SCHEMA_VERSION = 1
ANNUAL_URLS = {
    2025: "https://www.55125.cn/3d/3dsjhcx-2025.htm",
    2026: "https://www.55125.cn/3d/3dsjhcx-2026.htm",
}
ISSUE = re.compile(r"20\d{5}")
NUMBER = re.compile(r"\d{3}")
ISO_DATE = re.compile(r"\d{4}-\d{2}-\d{2}")


class TableParser(HTMLParser):
    def __init__(self) -> None:
        super().__init__(convert_charrefs=True)
        self.tables: list[list[list[str]]] = []
        self._table: list[list[str]] | None = None
        self._row: list[str] | None = None
        self._cell: list[str] | None = None

    def handle_starttag(self, tag: str, attrs: list[tuple[str, str | None]]) -> None:
        del attrs
        if tag == "table" and self._table is None:
            self._table = []
        elif tag == "tr" and self._table is not None:
            self._row = []
        elif tag in {"th", "td"} and self._row is not None:
            self._cell = []

    def handle_data(self, data: str) -> None:
        if self._cell is not None:
            self._cell.append(data)

    def handle_endtag(self, tag: str) -> None:
        if tag in {"th", "td"} and self._cell is not None and self._row is not None:
            self._row.append(normalize_text("".join(self._cell)))
            self._cell = None
        elif tag == "tr" and self._row is not None and self._table is not None:
            if any(self._row):
                self._table.append(self._row)
            self._row = None
        elif tag == "table" and self._table is not None:
            self.tables.append(self._table)
            self._table = None


def normalize_text(value: str) -> str:
    return re.sub(r"\s+", " ", value).strip()


def normalize_number(value: str) -> str:
    if not re.fullmatch(r"\d(?:\s*[,，]\s*\d){2}|\d{3}", value):
        raise ValueError(f"invalid trial number cell: {value!r}")
    number = re.sub(r"[\s,，]", "", value)
    if not NUMBER.fullmatch(number):
        raise ValueError(f"invalid normalized trial number: {number!r}")
    return number


def download(url: str) -> str:
    request = urllib.request.Request(
        url,
        headers={"Accept": "text/html", "User-Agent": "Lucky3D seed builder/1.0"},
    )
    with urllib.request.urlopen(request, timeout=30) as response:
        final = response.geturl()
        if final != url:
            raise ValueError(f"unexpected redirect: {url} -> {final}")
        content_type = response.headers.get_content_type()
        if content_type != "text/html":
            raise ValueError(f"unexpected content type for {url}: {content_type}")
        payload = response.read(2 * 1024 * 1024 + 1)
        if len(payload) > 2 * 1024 * 1024:
            raise ValueError(f"annual page too large: {url}")
        charset = response.headers.get_content_charset() or "utf-8"
        if charset.lower().replace("-", "") != "utf8":
            raise ValueError(f"unexpected charset for {url}: {charset}")
        return payload.decode("utf-8")


def parse_annual_page(year: int, url: str, html: str) -> list[dict[str, str]]:
    parser = TableParser()
    parser.feed(html)
    candidates: list[tuple[list[list[str]], int]] = []
    for table in parser.tables:
        for index, row in enumerate(table):
            if row[:3] == ["期号", "日期", "试机号"]:
                candidates.append((table, index))
    if len(candidates) != 1:
        raise ValueError(f"expected one approved annual table for {year}, found {len(candidates)}")
    table, header_index = candidates[0]
    unique: dict[str, dict[str, str]] = {}
    for row in table[header_index + 1 :]:
        if len(row) < 3 or not ISSUE.fullmatch(row[0]):
            continue
        issue, source_date_text = row[0], row[1]
        if not ISO_DATE.fullmatch(source_date_text):
            raise ValueError(f"invalid date for {issue}: {source_date_text!r}")
        source_date = dt.date.fromisoformat(source_date_text)
        if int(issue[:4]) != year or source_date.year != year:
            raise ValueError(f"year mismatch for {issue}: {source_date}")
        record = {
            "issue": issue,
            "number": normalize_number(row[2]),
            "sourceDate": source_date.isoformat(),
            "sourcePageUrl": url,
        }
        previous = unique.get(issue)
        if previous is not None and previous != record:
            raise ValueError(f"conflicting duplicate issue: {issue}")
        unique[issue] = record
    records = sorted(unique.values(), key=lambda item: item["issue"])
    if not records:
        raise ValueError(f"no trial records parsed for {year}")
    expected_last = 351 if year == 2025 else int(records[-1]["issue"][-3:])
    expected = [f"{year}{sequence:03d}" for sequence in range(1, expected_last + 1)]
    actual = [record["issue"] for record in records]
    if actual != expected:
        missing = sorted(set(expected) - set(actual))
        raise ValueError(f"non-contiguous {year} issues; missing={missing[:10]}")
    return records


def validate_payload(payload: object) -> dict[str, object]:
    if not isinstance(payload, dict):
        raise ValueError("seed root must be an object")
    if payload.get("schemaVersion") != SCHEMA_VERSION or payload.get("source") != SOURCE:
        raise ValueError("unexpected seed schema or source")
    records = payload.get("records")
    if not isinstance(records, list):
        raise ValueError("records must be a list")
    unique: dict[str, dict[str, str]] = {}
    for raw in records:
        if not isinstance(raw, dict) or set(raw) != {"issue", "number", "sourceDate", "sourcePageUrl"}:
            raise ValueError("record fields do not match the approved contract")
        record = {key: str(value) for key, value in raw.items()}
        issue = record["issue"]
        if not ISSUE.fullmatch(issue) or not NUMBER.fullmatch(record["number"]):
            raise ValueError(f"invalid issue or number: {record}")
        source_date = dt.date.fromisoformat(record["sourceDate"])
        if source_date.year != int(issue[:4]):
            raise ValueError(f"date year mismatch: {record}")
        parsed_url = urlparse(record["sourcePageUrl"])
        expected_url = ANNUAL_URLS.get(source_date.year)
        if parsed_url.scheme != "https" or parsed_url.hostname != "www.55125.cn" or record["sourcePageUrl"] != expected_url:
            raise ValueError(f"unapproved source URL: {record['sourcePageUrl']}")
        previous = unique.get(issue)
        if previous is not None and previous != record:
            raise ValueError(f"conflicting duplicate issue: {issue}")
        unique[issue] = record
    ordered = [unique[key] for key in sorted(unique)]
    for year in ANNUAL_URLS:
        year_records = [record for record in ordered if record["issue"].startswith(str(year))]
        if not year_records:
            raise ValueError(f"missing year {year}")
        expected_last = 351 if year == 2025 else int(year_records[-1]["issue"][-3:])
        expected = [f"{year}{sequence:03d}" for sequence in range(1, expected_last + 1)]
        if [record["issue"] for record in year_records] != expected:
            raise ValueError(f"non-contiguous year {year}")
    if ordered[0]["issue"] != "2025001" or ordered[-1]["issue"] < "2026001":
        raise ValueError("seed boundary does not include full 2025 and current 2026")
    generated_at = payload.get("generatedAt")
    if not isinstance(generated_at, str) or not generated_at.endswith("Z"):
        raise ValueError("generatedAt must be a UTC instant")
    dt.datetime.fromisoformat(generated_at.replace("Z", "+00:00"))
    normalized = dict(payload)
    normalized["records"] = ordered
    return normalized


def build_payload() -> dict[str, object]:
    records: list[dict[str, str]] = []
    for year, url in ANNUAL_URLS.items():
        records.extend(parse_annual_page(year, url, download(url)))
    latest_date = dt.date.fromisoformat(records[-1]["sourceDate"])
    generated_at = dt.datetime.combine(
        latest_date,
        dt.time(18, 30),
        tzinfo=dt.timezone(dt.timedelta(hours=8)),
    ).astimezone(dt.timezone.utc)
    return validate_payload(
        {
            "schemaVersion": SCHEMA_VERSION,
            "source": SOURCE,
            "generatedAt": generated_at.isoformat().replace("+00:00", "Z"),
            "records": records,
        }
    )


def serialized(payload: dict[str, object]) -> bytes:
    return (json.dumps(payload, ensure_ascii=False, indent=2, sort_keys=True) + "\n").encode("utf-8")


def verify(path: Path) -> None:
    payload = validate_payload(json.loads(path.read_text(encoding="utf-8")))
    data = serialized(payload)
    if path.read_bytes() != data:
        raise ValueError("seed JSON is not in deterministic normalized form")
    records = payload["records"]
    assert isinstance(records, list)
    digest = hashlib.sha256(data).hexdigest().upper()
    print(f"CAIBA_TRIAL_COUNT={len(records)}")
    print(f"CAIBA_TRIAL_FIRST={records[0]['issue']}")
    print(f"CAIBA_TRIAL_LAST={records[-1]['issue']}")
    print(f"CAIBA_TRIAL_SHA256={digest}")


def main() -> int:
    parser = argparse.ArgumentParser()
    mode = parser.add_mutually_exclusive_group(required=True)
    mode.add_argument("--output", type=Path)
    mode.add_argument("--verify", type=Path)
    args = parser.parse_args()
    try:
        if args.output is not None:
            payload = build_payload()
            args.output.parent.mkdir(parents=True, exist_ok=True)
            args.output.write_bytes(serialized(payload))
            verify(args.output)
        else:
            verify(args.verify)
        return 0
    except (OSError, ValueError, json.JSONDecodeError) as error:
        print(f"ERROR: {error}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
