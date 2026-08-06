import { cn, hueFor } from "@/lib/utils";

/**
 * A composed book cover.
 *
 * The catalogue stores no artwork, so a cover is built instead: a two-tone gradient,
 * a darker spine down the left edge, the title set large and the author beneath.
 * Genre picks the palette where it is recognised; otherwise the hue is derived from
 * the title's hash, so a given book always looks the same — the shelf reads as a row
 * of distinct objects rather than repeated placeholders.
 *
 * The same derivation runs in `Theme.coverGradient` on the desktop client, so a book
 * looks identical in both editions.
 */

const GENRE_PALETTES: { match: string[]; from: string; to: string }[] = [
  { match: ["trag", "drame"], from: "#8e1f2e", to: "#c43b45" },
  { match: ["polic", "thrill", "sf", "science"], from: "#1b3a6b", to: "#2f6bb8" },
  { match: ["hist"], from: "#5b3a1e", to: "#9a6b38" },
  { match: ["roman"], from: "#004080", to: "#0b5aa8" },
  { match: ["autobi", "bio", "mémo", "memo"], from: "#0e6e66", to: "#18a796" },
  { match: ["conte", "fant", "merveil"], from: "#5a358c", to: "#8c5ac8" },
  { match: ["philo", "essai", "poé", "poe"], from: "#a45b0b", to: "#e9a400" },
];

export function coverPalette(title: string, genre?: string | null): { from: string; to: string } {
  const g = (genre ?? "").toLowerCase();
  if (g) {
    for (const palette of GENRE_PALETTES) {
      if (palette.match.some((needle) => g.includes(needle))) {
        return { from: palette.from, to: palette.to };
      }
    }
  }
  const hue = hueFor(title);
  return {
    from: `hsl(${hue} 42% 42%)`,
    to: `hsl(${(hue + 22) % 360} 50% 60%)`,
  };
}

export function BookCover({
  title,
  author,
  genre,
  className,
}: {
  title: string;
  author: string;
  genre?: string | null;
  className?: string;
}) {
  const { from, to } = coverPalette(title, genre);

  return (
    <div
      className={cn(
        "relative isolate aspect-[1/1.42] w-full overflow-hidden rounded-xl",
        "shadow-[--shadow-cover] transition-shadow duration-300",
        "group-hover:shadow-[--shadow-cover-hover]",
        className,
      )}
      style={{ backgroundImage: `linear-gradient(150deg, ${to}, ${from})` }}
    >
      {/* Spine: the strongest cue that this is a book and not a coloured tile. */}
      <div className="absolute inset-y-0 left-0 w-[8%] bg-black/25" />
      <div className="absolute inset-y-0 left-[8%] w-px bg-white/25" />

      {/* Diagonal sheen across the upper corner. */}
      <div className="absolute inset-0 bg-gradient-to-br from-white/20 via-transparent to-transparent" />

      <div className="relative flex h-full flex-col justify-between px-[14%] py-[10%]">
        <div>
          <p className="line-clamp-4 text-[clamp(0.8rem,1.35vw,1.05rem)] font-bold leading-tight text-white drop-shadow-[0_1px_2px_rgb(0_0_0/0.35)]">
            {title}
          </p>
          <div className="mt-3 h-0.5 w-8 rounded-full bg-white/45" />
        </div>
        <p className="truncate text-[clamp(0.6rem,0.9vw,0.75rem)] text-white/85">{author}</p>
      </div>

      {/* Hairline edge so the cover separates from the white card behind it. */}
      <div className="pointer-events-none absolute inset-0 rounded-xl ring-1 ring-inset ring-black/15" />
    </div>
  );
}
