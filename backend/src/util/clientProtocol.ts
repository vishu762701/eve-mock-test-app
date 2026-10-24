import { Context } from "hono";

/**
 * Evaluates whether question answers and explanations should be omitted from the response.
 * True if the user is NOT an admin AND (header X-Eve-Client >= 2 OR env.LEGACY_ANSWER_LEAK !== "on").
 * Admins always receive full data.
 */
export function hideAnswers(c: Context<any>): boolean {
  const user = c.get("user");
  if (user && user.isAdmin) {
    return false;
  }

  const clientHeader = c.req.header("X-Eve-Client") || c.req.header("x-eve-client");
  const clientVersion = clientHeader ? parseInt(clientHeader, 10) : 0;
  const legacyLeak = c.env?.LEGACY_ANSWER_LEAK;

  if ((!isNaN(clientVersion) && clientVersion >= 2) || legacyLeak !== "on") {
    return true;
  }

  return false;
}
