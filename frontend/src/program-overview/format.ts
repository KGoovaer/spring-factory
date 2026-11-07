const kilograms = new Intl.NumberFormat("en", { maximumFractionDigits: 2 });

/** A Volume in kilograms: `3,600 kg`. */
export function formatVolume(volume: number): string {
  return `${kilograms.format(volume)} kg`;
}

/** An Adherence percentage, or a dash for a Week without Sessions. */
export function formatAdherence(adherence: number | null): string {
  return adherence === null ? "–" : `${adherence}%`;
}
