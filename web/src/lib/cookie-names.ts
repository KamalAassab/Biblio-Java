/**
 * Cookie and header names, kept in a module with no imports.
 *
 * The Edge middleware needs these constants but cannot load `node:crypto`, which
 * `session.ts` depends on. Importing them from there would pull the whole crypto
 * module into the Edge bundle and fail at runtime, so they live on their own.
 */

export const SESSION_COOKIE = "biblio_session";
export const CSRF_COOKIE = "biblio_csrf";
export const CSRF_HEADER = "x-csrf-token";
