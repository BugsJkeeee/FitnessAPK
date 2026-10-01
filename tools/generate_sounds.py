#!/usr/bin/env python3
"""
Синтезирует библиотеку звуков таймера (20 штук, 4 группы) в app/src/main/res/raw.

Звуки создаются математически, без сторонних записей, поэтому ограничений
по лицензии нет. Запуск: python3 tools/generate_sounds.py
"""
import math
import os
import random
import struct
import wave

RATE = 22050
OUT = os.path.join(os.path.dirname(__file__), "..", "app", "src", "main", "res", "raw")
random.seed(7)


def silence(sec):
    return [0.0] * int(RATE * sec)


def tone(freq, sec, amp=1.0, decay=0.0, attack=0.005, release=0.02, wave_fn=math.sin, harmonics=None):
    n = int(RATE * sec)
    out = []
    harmonics = harmonics or [(1, 1.0)]
    for i in range(n):
        t = i / RATE
        v = sum(a * wave_fn(2 * math.pi * freq * h * t) for h, a in harmonics)
        env = min(1.0, t / attack) if attack > 0 else 1.0
        env *= math.exp(-decay * t)
        env *= min(1.0, (sec - t) / release) if release > 0 else 1.0
        out.append(amp * v * env)
    return out


def bell(partials, sec, amp=1.0, strike=0.0):
    """Колокол или гонг: негармонические призвуки с разным затуханием."""
    n = int(RATE * sec)
    out = []
    for i in range(n):
        t = i / RATE
        v = sum(a * math.exp(-d * t) * math.sin(2 * math.pi * f * t) for f, a, d in partials)
        if strike and t < 0.03:
            v += strike * (random.random() * 2 - 1) * (1 - t / 0.03)
        attack = min(1.0, t / 0.003)
        out.append(amp * v * attack)
    return out


def sweep(f0, f1, sec, amp=1.0):
    n = int(RATE * sec)
    out, phase = [], 0.0
    for i in range(n):
        t = i / RATE
        f = f0 + (f1 - f0) * t / sec
        phase += 2 * math.pi * f / RATE
        env = min(1.0, t / 0.01) * min(1.0, (sec - t) / 0.05)
        out.append(amp * math.sin(phase) * env)
    return out


def saw(x):
    x = (x / (2 * math.pi)) % 1.0
    return 2 * x - 1


def square(x):
    return 1.0 if math.sin(x) >= 0 else -1.0


def mix(*tracks):
    n = max(len(t) for t in tracks)
    return [sum(t[i] for t in tracks if i < len(t)) for i in range(n)]


def concat(*parts):
    out = []
    for p in parts:
        out += p
    return out


def lowpass(samples, alpha=0.2):
    out, prev = [], 0.0
    for s in samples:
        prev = prev + alpha * (s - prev)
        out.append(prev)
    return out


def write(name, samples):
    peak = max(abs(s) for s in samples) or 1.0
    data = b"".join(struct.pack("<h", int(max(-1, min(1, s / peak * 0.9)) * 32767)) for s in samples)
    with wave.open(os.path.join(OUT, name + ".wav"), "wb") as w:
        w.setnchannels(1)
        w.setsampwidth(2)
        w.setframerate(RATE)
        w.writeframes(data)


GONG = [(220, 1.0, 1.2), (440 * 1.21, 0.6, 1.8), (880 * 1.03, 0.45, 2.5), (1210, 0.3, 3.5), (1690, 0.2, 4.5), (2350, 0.12, 6)]
BELL = [(880, 1.0, 3.0), (880 * 2.76, 0.5, 5.0), (880 * 5.4, 0.25, 8.0), (880 * 0.5, 0.3, 2.0)]


def air_horn(sec):
    n = int(RATE * sec)
    out = []
    for i in range(n):
        t = i / RATE
        bend = 1 + 0.03 * math.exp(-8 * t)
        v = sum(saw(2 * math.pi * f * bend * t) * a for f, a in [(415, 1.0), (523, 0.8), (622, 0.6)])
        env = min(1.0, t / 0.04) * min(1.0, (sec - t) / 0.1)
        out.append(v * env * (0.9 + 0.1 * math.sin(2 * math.pi * 7 * t)))
    return lowpass(out, 0.35)


def whistle(sec):
    n = int(RATE * sec)
    out = []
    for i in range(n):
        t = i / RATE
        trill = 0.6 + 0.4 * math.sin(2 * math.pi * 28 * t)
        v = math.sin(2 * math.pi * 3100 * t + 0.4 * math.sin(2 * math.pi * 28 * t)) * trill
        v += 0.15 * (random.random() * 2 - 1)
        env = min(1.0, t / 0.02) * min(1.0, (sec - t) / 0.08)
        out.append(v * env)
    return out


def brass(freq, sec):
    return lowpass(tone(freq, sec, wave_fn=saw, attack=0.03, release=0.08), 0.25)


def generate():
    os.makedirs(OUT, exist_ok=True)

    # --- Отсчёт 3-2-1 ---
    write("count_beep", tone(880, 0.15, release=0.03))
    write("count_beep_high", tone(1320, 0.12, harmonics=[(1, 1.0), (3, 0.25)], release=0.03))
    write("count_click", mix(tone(2000, 0.03, decay=80, release=0.005),
                             [(random.random() * 2 - 1) * math.exp(-i / 60) * 0.5 for i in range(600)]))
    write("count_wood", mix(tone(800, 0.12, decay=45), tone(1600, 0.12, amp=0.5, decay=60)))
    write("count_digital", concat(tone(1000, 0.06, wave_fn=square, amp=0.4, release=0.005), silence(0.04),
                                  tone(1000, 0.06, wave_fn=square, amp=0.4, release=0.005)))

    # --- Начало работы ---
    write("work_boxing_gong", bell(GONG, 2.8, strike=0.6))
    write("work_air_horn", air_horn(1.3))
    write("work_whistle", whistle(0.9))
    write("work_race_start", tone(1760, 0.7, harmonics=[(1, 1.0), (2, 0.2)], release=0.05))
    write("work_bell", concat(*[bell(BELL, 0.18) for _ in range(6)], bell(BELL, 0.8)))
    write("work_buzzer", lowpass(mix(tone(220, 0.9, wave_fn=square), tone(330, 0.9, wave_fn=square, amp=0.6)), 0.3))

    # --- Начало отдыха ---
    write("rest_chime", mix(tone(660, 0.9, decay=4), tone(990, 0.9, amp=0.5, decay=5)))
    write("rest_ding_dong", concat(tone(784, 0.45, decay=4, release=0.05), tone(622, 0.8, decay=4)))
    write("rest_soft_beep", tone(520, 0.35, attack=0.05, release=0.12))
    write("rest_down", sweep(900, 480, 0.35))

    # --- Конец тренировки ---
    write("finish_triple_gong", concat(bell(GONG, 0.6, strike=0.6), bell(GONG, 0.6, strike=0.6), bell(GONG, 2.5, strike=0.6)))
    write("finish_fanfare", concat(brass(523, 0.18), brass(659, 0.18), brass(784, 0.18),
                                   mix(brass(1047, 1.0), brass(784, 1.0), brass(659, 1.0))))
    write("finish_long_horn", air_horn(2.5))
    write("finish_victory", concat(*[bell([(f, 1.0, 4.0), (f * 2.0, 0.3, 6.0)], 0.2) for f in (523, 659, 784)],
                                   mix(*[bell([(f, 0.7, 2.0), (f * 2.0, 0.2, 4.0)], 1.6) for f in (523, 659, 784, 1047)])))
    write("finish_bells", concat(*[bell(BELL, 0.45) for _ in range(2)], bell(BELL, 1.6)))


if __name__ == "__main__":
    generate()
