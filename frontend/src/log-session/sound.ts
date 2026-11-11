import { createContext, useContext, useEffect, useRef } from "react";
import { countdownBeep, type Beep } from "./beeps";

/**
 * Plays a beep, or does nothing when the Lifter muted the beeps. The log form
 * provides it; outside one, nothing beeps.
 */
export const BeepContext = createContext<(beep: Beep) => void>(() => {});

/**
 * Beeps the last 5 seconds of a countdown and its 0, each time `secondsLeft`
 * reaches a new value; null means the countdown is not running.
 */
export function useCountdownBeep(secondsLeft: number | null): void {
  const beep = useContext(BeepContext);
  const latest = useRef(beep);
  useEffect(() => {
    latest.current = beep;
  });
  useEffect(() => {
    const due = countdownBeep(secondsLeft);
    if (due) {
      latest.current(due);
    }
  }, [secondsLeft]);
}

let audio: AudioContext | null = null;

/**
 * The page's one AudioContext, made and resumed on demand, or null in a browser
 * without Web Audio. Browsers only let it play after the Lifter clicked on the
 * page, so the form calls this on each click too.
 */
export function unlockAudio(): AudioContext | null {
  const Audio = globalThis.AudioContext;
  if (!Audio) {
    return null;
  }
  if (!(audio instanceof Audio)) {
    audio = new Audio();
  }
  if (audio.state === "suspended") {
    void audio.resume();
  }
  return audio;
}

/** Plays a beep: a short high one, or a long lower one. */
export function playBeep(beep: Beep): void {
  const context = unlockAudio();
  if (!context) {
    return;
  }
  const seconds = beep === "long" ? 0.8 : 0.15;
  const start = context.currentTime;
  const tone = context.createOscillator();
  const volume = context.createGain();
  tone.type = "sine";
  tone.frequency.value = beep === "long" ? 660 : 880;
  volume.gain.setValueAtTime(0.4, start);
  volume.gain.setValueAtTime(0.4, start + seconds - 0.05);
  volume.gain.linearRampToValueAtTime(0, start + seconds);
  tone.connect(volume).connect(context.destination);
  tone.start(start);
  tone.stop(start + seconds);
}
