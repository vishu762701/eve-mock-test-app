/**
 * Security and Secret Sanitization for EVE JARVIS Gemini Sync
 * Ensures zero credential or sensitive token leakage into published Google Drive knowledge documents.
 */

export const SECRET_PATTERNS = [
  { name: 'Private Key block', regex: /-----BEGIN (RSA |EC |DSA |OPENSSH |PGP )?PRIVATE KEY-----[\s\S]*?-----END (RSA |EC |DSA |OPENSSH |PGP )?PRIVATE KEY-----/g },
  { name: 'GitHub Personal Access Token', regex: /(?:ghp|gho|ghu|ghs|ghr)_[A-Za-z0-9_]{36,}/g },
  { name: 'Fine-grained GitHub Token', regex: /github_pat_[A-Za-z0-9_]{82,}/g },
  { name: 'Slack Bot/User Token', regex: /xox[baprs]-[0-9]{10,13}-[0-9]{10,13}-[a-zA-Z0-9]{24,}/g },
  { name: 'AWS Access Key ID', regex: /(?:A3T[A-Z0-9]|AKIA|AGPA|AIDA|AROA|AIPA|ANPA|ANVA|ASIA)[A-Z0-9]{16}/g },
  { name: 'Google Cloud API Key', regex: /AIza[0-9A-Za-z\-_]{35}/g },
  { name: 'Stripe Secret Key', regex: /(?:sk|rk)_live_[0-9a-zA-Z]{24,}/g },
  { name: 'Supabase Service Role Key', regex: /sbp_[a-zA-Z0-9]{40,}/g },
  { name: 'JSON Web Token / Bearer Token', regex: /ey[A-Za-z0-9_-]{15,}\.ey[A-Za-z0-9_-]{15,}\.[A-Za-z0-9_-]{15,}/g },
  { name: 'URI Embedded Password Credentials', regex: /https?:\/\/[^:@/\s]+:[^:@/\s]+@[a-zA-Z0-9_.-]+/g },
  { name: 'Generic Password Assignment', regex: /(?:password|secret|api_key|apikey|private_key)\s*[:=]\s*["']([A-Za-z0-9@#$%^&*-_+=]{16,})["']/gi }
];

export const SENSITIVE_PATH_PATTERNS = [
  /\.(jks|keystore|key|pem|p12|pfx|crt|der)$/i,
  /^\.env(?:\..+)?$/i,
  /\.jarvis-oauth.*\.env$/i,
  /\/google-services\.json$/i,
  /^google-services\.json$/i,
  /service-account.*\.json$/i,
  /credentials.*\.json$/i,
  /id_rsa/i,
  /id_ed25519/i
];

/**
 * Checks if a relative path corresponds to a credential or secret file.
 * @param {string} relPath
 * @returns {boolean}
 */
export function isSensitivePath(relPath) {
  const normalized = relPath.replace(/\\/g, '/');
  return SENSITIVE_PATH_PATTERNS.some(pattern => pattern.test(normalized));
}

/**
 * Detects sensitive patterns within string content.
 * @param {string} content
 * @returns {Array<{ name: string, match: string }>}
 */
export function detectSecrets(content) {
  const matches = [];
  for (const pattern of SECRET_PATTERNS) {
    pattern.regex.lastIndex = 0;
    let match;
    while ((match = pattern.regex.exec(content)) !== null) {
      // Exclude obvious placeholders / mocks / tests
      const raw = match[0];
      if (
        raw.includes('REDACTED') ||
        raw.includes('dummy') ||
        raw.includes('mock') ||
        raw.includes('example') ||
        raw.includes('placeholder')
      ) {
        continue;
      }
      matches.push({ name: pattern.name, match: raw });
    }
  }
  return matches;
}

/**
 * Sanitizes content by masking any detected secret patterns.
 * @param {string} filePath
 * @param {string} content
 * @returns {{ sanitized: string, redactedCount: number }}
 */
export function sanitizeContent(filePath, content) {
  let sanitized = content;
  let redactedCount = 0;

  for (const pattern of SECRET_PATTERNS) {
    pattern.regex.lastIndex = 0;
    sanitized = sanitized.replace(pattern.regex, (match) => {
      if (
        match.includes('REDACTED') ||
        match.includes('dummy') ||
        match.includes('mock') ||
        match.includes('example') ||
        match.includes('placeholder')
      ) {
        return match;
      }
      redactedCount++;
      return `[REDACTED_BY_EVE_JARVIS_SYNC: ${pattern.name}]`;
    });
  }

  return { sanitized, redactedCount };
}
