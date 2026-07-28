package com.lucky3d.app.feature.scheme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.lucky3d.app.R
import com.lucky3d.app.domain.backtest.BacktestReport
import com.lucky3d.app.domain.backtest.BacktestResult
import com.lucky3d.app.domain.backtest.BacktestStatus
import java.text.NumberFormat

@Composable
internal fun BacktestSection(
    startIssue: String,
    endIssue: String,
    isRunning: Boolean,
    report: BacktestReport?,
    onRangeChange: (String, String) -> Unit,
    onRun: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            stringResource(R.string.backtest_title),
            style = MaterialTheme.typography.titleLarge,
        )
        Text(
            stringResource(R.string.backtest_disclaimer),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = startIssue,
                onValueChange = { onRangeChange(it, endIssue) },
                label = { Text(stringResource(R.string.backtest_start_issue)) },
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
            OutlinedTextField(
                value = endIssue,
                onValueChange = { onRangeChange(startIssue, it) },
                label = { Text(stringResource(R.string.backtest_end_issue)) },
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
        }
        Button(
            onClick = onRun,
            enabled = !isRunning,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                stringResource(
                    if (isRunning) R.string.backtest_running else R.string.backtest_run,
                ),
            )
        }
        if (isRunning) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }
        report?.let { BacktestReportContent(it) }
    }
}

@Composable
private fun BacktestReportContent(report: BacktestReport) {
    val rate = report.coverageRate?.let {
        NumberFormat.getPercentInstance().apply { maximumFractionDigits = 1 }.format(it)
    } ?: "--"
    Card(
        modifier = Modifier.fillMaxWidth(),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                stringResource(
                    R.string.backtest_overview,
                    report.coveredCount,
                    report.eligibleCount,
                    rate,
                ),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                stringResource(
                    R.string.backtest_cost,
                    report.averageBetCount?.let { "%.1f".format(it) } ?: "--",
                    report.cumulativeAmountYuan,
                ),
            )
        }
    }
}

@Composable
internal fun BacktestResultCard(result: BacktestResult) {
    val status = when (result.status) {
        BacktestStatus.INSUFFICIENT_SAMPLE ->
            stringResource(R.string.backtest_sample_insufficient)
        BacktestStatus.CONDITION_CONFLICT ->
            stringResource(R.string.pick_conflict_title)
        BacktestStatus.EVALUATED -> stringResource(
            if (result.covered == true) {
                R.string.backtest_period_covered
            } else {
                R.string.backtest_period_not_covered
            },
        )
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                stringResource(
                    R.string.backtest_period_row,
                    result.targetIssue,
                    status,
                    result.candidateCount,
                    result.amountYuan,
                ),
                style = MaterialTheme.typography.titleSmall,
            )
            if (result.candidates.isNotEmpty()) {
                Text(
                    stringResource(
                        R.string.backtest_period_candidates,
                        result.candidates.joinToString(" ") { it.value },
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
