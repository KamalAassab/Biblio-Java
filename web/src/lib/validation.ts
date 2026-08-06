import { z } from "zod";

/**
 * Request schemas.
 *
 * Every mutating route parses its body through one of these. SQL injection is already
 * prevented by bound parameters in `db.ts`; this layer rejects oversized payloads,
 * malformed contact details, and the invisible Unicode tricks that make a stored
 * display name render as something other than what it is.
 */

// These patterns are built with the RegExp constructor rather than literals so the
// source file stays pure ASCII — a literal control character in a regex class is
// easy to corrupt in transit and impossible to review.

/** C0 and C1 control characters. */
const CONTROL = new RegExp("[\\u0000-\\u001F\\u007F-\\u009F]", "g");

/** Same, but preserving tab (09), newline (0A) and carriage return (0D). */
const CONTROL_KEEP_NEWLINES = new RegExp(
  "[\\u0000-\\u0008\\u000B\\u000C\\u000E-\\u001F\\u007F-\\u009F]",
  "g",
);

/** Zero-width characters and bidirectional overrides, used to disguise text. */
const INVISIBLE = new RegExp(
  "[\\u200B-\\u200F\\u202A-\\u202E\\u2066-\\u2069\\uFEFF]",
  "g",
);

/** Strips control characters, bidi overrides and zero-width joiners, then collapses spaces. */
const clean = (value: string) =>
  value.replace(CONTROL, "").replace(INVISIBLE, "").replace(/\s+/g, " ").trim();

/** Same, but keeps newlines — for multi-line fields such as summaries. */
const cleanMultiline = (value: string) =>
  value
    .replace(CONTROL_KEEP_NEWLINES, "")
    .replace(INVISIBLE, "")
    .replace(/[ \t]+/g, " ")
    .replace(/(\r?\n){3,}/g, "\n\n")
    .trim();

/** Accepts generous raw input, then normalises before the real length check runs. */
const trimmed = (max: number) =>
  z
    .string()
    .max(max * 2)
    .transform(clean);

export const loginSchema = z.object({
  nom: z.string().min(1).max(80).transform(clean),
  motDePasse: z.string().min(1).max(128),
});

export const bookSchema = z.object({
  titre: trimmed(200).pipe(
    z.string().min(1, "error.title.required").max(200, "error.title.required"),
  ),
  auteur: trimmed(120).pipe(
    z.string().min(1, "error.author.required").max(120, "error.author.required"),
  ),
  genre: trimmed(60).pipe(z.string().max(60, "error.genre.invalid")).optional().default(""),
  resume: z
    .string()
    .max(8000)
    .transform(cleanMultiline)
    .pipe(z.string().max(4000, "error.summary.invalid"))
    .optional()
    .default(""),
  disponibilite: z.boolean().optional().default(true),
});

export const loanSchema = z.object({
  idUtilisateur: z.coerce.number().int().positive(),
  idLivre: z.coerce.number().int().positive(),
  dateRetour: z
    .string()
    .regex(/^\d{4}-\d{2}-\d{2}$/, "error.date.invalid")
    .refine((value) => !Number.isNaN(Date.parse(value)), "error.date.invalid"),
});

export const reservationSchema = z.object({
  idUtilisateur: z.coerce.number().int().positive(),
  dateReservation: z
    .string()
    .regex(/^\d{4}-\d{2}-\d{2}$/, "error.date.invalid")
    .refine((value) => !Number.isNaN(Date.parse(value)), "error.date.invalid"),
});

/**
 * Passwords: at least 8 characters mixing three of the four character classes.
 * Mirrors `Validate.password` in the desktop client so the rules cannot diverge.
 */
export const passwordSchema = z
  .string()
  .min(8, "error.password.length")
  .max(128, "error.password.length")
  .refine((value) => {
    let classes = 0;
    if (/[a-z]/.test(value)) classes++;
    if (/[A-Z]/.test(value)) classes++;
    if (/[0-9]/.test(value)) classes++;
    if (/[^A-Za-z0-9]/.test(value)) classes++;
    return classes >= 3;
  }, "error.password.weak");

export const profileSchema = z.object({
  nom: trimmed(80).pipe(
    z
      .string()
      .min(3, "error.username.invalid")
      .max(80, "error.username.invalid")
      .regex(/^[\p{L}\p{N} ._'-]+$/u, "error.username.invalid"),
  ),
  email: z
    .string()
    .max(254)
    .transform((value) => clean(value).toLowerCase())
    .pipe(z.union([z.literal(""), z.string().email("error.email.invalid")])),
  numero: z.coerce.number().int().min(0).max(999_999_999).optional().default(0),
});

export const changePasswordSchema = z.object({
  currentPassword: z.string().min(1).max(128),
  newPassword: passwordSchema,
});

export const userSchema = profileSchema.extend({
  motDePasse: passwordSchema,
  role: z.enum(["Admin", "Lecteur"]),
});

/** Bounded search input; wildcards removed so a query cannot force a full scan. */
export const searchSchema = z.object({
  q: z
    .string()
    .max(120)
    .transform((value) => clean(value).replace(/[%_]/g, ""))
    .optional()
    .default(""),
  genre: trimmed(60).optional(),
  availability: z.enum(["all", "available", "borrowed"]).optional().default("all"),
});

export const idSchema = z.coerce.number().int().positive();
