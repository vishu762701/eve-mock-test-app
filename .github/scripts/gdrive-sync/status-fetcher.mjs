/**
 * Real-time GitHub Actions Status Fetcher for EVE JARVIS Gemini Sync
 * Queries GitHub REST API for actual CI/CD workflow and check-run results.
 * Never invents or assumes build statuses.
 */

/**
 * Fetches real build/test statuses for a commit SHA from GitHub REST API.
 * @param {string} commitSha
 * @param {string} [repo] e.g. "vishu762701/eve-mock-test-app"
 * @param {string} [token] GitHub token
 * @returns {Promise<{ available: boolean, runs: Array<{ name: string, status: string, conclusion: string, url: string, runId: string }>, summary: string }>}
 */
export async function fetchCommitStatus(commitSha, repo = process.env.GITHUB_REPOSITORY, token = process.env.GITHUB_TOKEN) {
  if (!commitSha) {
    return {
      available: false,
      runs: [],
      summary: 'Commit SHA unavailable'
    };
  }

  if (!repo || !token) {
    return {
      available: false,
      runs: [],
      summary: 'GitHub API credentials (GITHUB_REPOSITORY / GITHUB_TOKEN) not available in current environment; status not fetched from remote.'
    };
  }

  try {
    const url = `https://api.github.com/repos/${repo}/actions/runs?head_sha=${commitSha}&exclude_pull_requests=true`;
    const res = await fetch(url, {
      headers: {
        'Accept': 'application/vnd.github.v3+json',
        'Authorization': `Bearer ${token}`,
        'User-Agent': 'eve-jarvis-gdrive-sync'
      }
    });

    if (!res.ok) {
      return {
        available: false,
        runs: [],
        summary: `GitHub API returned HTTP ${res.status}: ${res.statusText}`
      };
    }

    const data = await res.json();
    const runs = (data.workflow_runs || []).map(r => ({
      name: r.name,
      status: r.status,
      conclusion: r.conclusion || 'in_progress',
      url: r.html_url,
      runId: String(r.id),
      event: r.event,
      updatedAt: r.updated_at
    }));

    return {
      available: true,
      runs,
      summary: runs.length > 0
        ? `Found ${runs.length} real GitHub Actions workflow runs for commit ${commitSha.slice(0, 7)}.`
        : `No workflow runs recorded yet for commit ${commitSha.slice(0, 7)}.`
    };
  } catch (err) {
    return {
      available: false,
      runs: [],
      summary: `Failed to query GitHub Actions API: ${err.message}`
    };
  }
}

/**
 * Formats workflow run data into Markdown table for inclusion in system index.
 * @param {{ available: boolean, runs: Array<any>, summary: string }} statusData
 * @param {string} commitSha
 * @returns {string}
 */
export function formatStatusMarkdown(statusData, commitSha) {
  let md = `### GitHub Actions Verified CI Status\n\n`;
  md += `> **Commit SHA**: \`${commitSha}\`\n\n`;

  if (!statusData.available || statusData.runs.length === 0) {
    md += `| Verification Status | Details |\n`;
    md += `| :--- | :--- |\n`;
    md += `| Remote CI Check | ${statusData.summary} |\n\n`;
    return md;
  }

  md += `| Workflow | Event | Status | Conclusion | Run Link |\n`;
  md += `| :--- | :--- | :--- | :--- | :--- |\n`;

  for (const run of statusData.runs) {
    const icon = run.conclusion === 'success' ? '✅'
      : run.conclusion === 'failure' ? '❌'
      : run.conclusion === 'cancelled' ? '⚠️'
      : '⏳';
    md += `| **${run.name}** | \`${run.event}\` | \`${run.status}\` | ${icon} \`${run.conclusion}\` | [#${run.runId}](${run.url}) |\n`;
  }

  md += `\n`;
  return md;
}
