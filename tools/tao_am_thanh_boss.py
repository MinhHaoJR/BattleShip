"""Tổng hợp âm thanh cho 3 boss mới (không dùng mẫu thu âm nào, chỉ sóng + nhiễu).

    python tools/tao_am_thanh_boss.py

Ghi WAV 22050 Hz, 16-bit, mono vào src/main/resources/audio/ (quy cách trong docs/boss-template.md).
Cần numpy.
"""
from pathlib import Path
import sys
import wave

import numpy as np

SR = 22050
OUT = Path(__file__).resolve().parent.parent / "src/main/resources/audio"
rng = np.random.default_rng(42)


def t_axis(sec):
    return np.arange(int(SR * sec)) / SR


def osc(freq, kind="sine"):
    """freq: mảng tần số theo thời gian (cho phép trượt cao độ)."""
    ph = 2 * np.pi * np.cumsum(freq) / SR
    if kind == "saw":
        return 2 * ((ph / (2 * np.pi)) % 1.0) - 1
    if kind == "square":
        return np.sign(np.sin(ph))
    return np.sin(ph)


def noise(n):
    return rng.uniform(-1, 1, n)


def band(x, lo, hi):
    """Lọc dải tần bằng FFT (mép mềm)."""
    spec = np.fft.rfft(x)
    f = np.fft.rfftfreq(len(x), 1 / SR)
    mask = 1 / (1 + (lo / np.maximum(f, 1)) ** 4) / (1 + (f / hi) ** 4)
    return np.fft.irfft(spec * mask, len(x))


def env(n, a=0.02, d=0.3, sustain=0.7, r=0.4):
    """Bao hình ADSR theo tỉ lệ độ dài."""
    t = np.linspace(0, 1, n)
    e = np.ones(n) * sustain
    e[t < a] = t[t < a] / a
    m = (t >= a) & (t < a + d)
    e[m] = 1 - (1 - sustain) * (t[m] - a) / d
    m = t > 1 - r
    e[m] = sustain * (1 - t[m]) / r
    return e


def drive(x, k=2.5):
    return np.tanh(x * k) / np.tanh(k)


def echo(x, delay=0.11, fb=0.35, taps=4):
    out = x.copy()
    d = int(SR * delay)
    for i in range(1, taps + 1):
        out[d * i:] += x[: len(x) - d * i] * fb ** i
    return out


def boom(sec, f0=70, f1=28):
    t = t_axis(sec)
    f = f0 * (f1 / f0) ** (t / sec)
    return (osc(f) * 0.9 + band(noise(len(t)), 30, 400) * 0.6) * np.exp(-t * 3.2)


def roar(sec, f0, f1, grit, formants, shimmer=0.0, screech=0.0):
    t = t_axis(sec)
    glide = f0 + (f1 - f0) * np.sin(np.pi * t / sec) ** 0.6
    vib = 1 + 0.03 * np.sin(2 * np.pi * 6.5 * t)
    body = osc(glide * vib, "saw") * 0.7 + osc(glide * 0.5 * vib, "saw") * 0.5
    body += band(noise(len(t)), 80, 2600) * grit
    voiced = sum(band(body, lo, hi) * g for lo, hi, g in formants)
    if shimmer:
        voiced += sum(osc(np.full(len(t), f)) for f in (1760, 2217, 2637)) * shimmer * (0.5 + 0.5 * np.sin(2 * np.pi * 9 * t))
    if screech:
        voiced += band(osc(glide * 4.2 * vib, "square"), 1200, 5000) * screech
    return echo(drive(voiced * env(len(t), 0.06, 0.25, 0.85, 0.35)), 0.13, 0.3)


def norm(x, peak=0.89):
    x = x - np.mean(x)
    m = np.max(np.abs(x)) or 1
    return x / m * peak


def save(name, x):
    data = (norm(x) * 32767).astype("<i2")
    with wave.open(str(OUT / name), "wb") as w:
        w.setnchannels(1)
        w.setsampwidth(2)
        w.setframerate(SR)
        w.writeframes(data.tobytes())
    print("ghi", name, f"{len(x) / SR:.2f}s")


def pad(x, sec):
    n = int(SR * sec)
    return np.pad(x, (0, max(0, n - len(x))))[:n]


def mix(*parts, sec):
    return sum(pad(p, sec) for p in parts)


def delayed(x, sec):
    return np.concatenate([np.zeros(int(SR * sec)), x])


# ---------- kiểu âm cho từng kiểu chiêu ----------

def charge_whine(sec, f0=220, f1=1400):
    t = t_axis(sec)
    return osc(f0 * (f1 / f0) ** (t / sec), "saw") * env(len(t), 0.1, 0.1, 0.8, 0.05) * 0.5


def beam_buzz(sec, f=110, bright=1.0):
    t = t_axis(sec)
    x = osc(np.full(len(t), f), "saw") + osc(np.full(len(t), f * 1.01), "saw") + band(noise(len(t)), 800, 6000) * 0.6 * bright
    return drive(x * env(len(t), 0.02, 0.2, 0.9, 0.3), 3)


def chimes(sec, n=6, base=1400):
    out = np.zeros(int(SR * sec))
    for i in range(n):
        start = int(SR * (0.08 + i * sec * 0.11))
        if start >= len(out):
            break
        tt = t_axis(0.5)
        f = base * (1.0 + 0.19 * (i % 4))
        ping = (osc(np.full(len(tt), f)) + 0.5 * osc(np.full(len(tt), f * 2.76))) * np.exp(-tt * 9)
        end = min(len(out), start + len(ping))
        out[start:end] += ping[: end - start]
    return out


def shatter(sec):
    t = t_axis(sec)
    return band(noise(len(t)), 2500, 9000) * np.exp(-t * 7) + chimes(sec, 4, 2600) * 0.4


def whoosh(sec, lfo=1.5):
    t = t_axis(sec)
    n = band(noise(len(t)), 200, 1800)
    return n * (0.4 + 0.6 * np.abs(np.sin(2 * np.pi * lfo * t))) * env(len(t), 0.15, 0.2, 0.8, 0.3)


def chatter(sec, rate=38):
    t = t_axis(sec)
    clicks = (rng.random(len(t)) < rate / SR).astype(float)
    k = np.exp(-np.arange(int(SR * 0.012)) / (SR * 0.002))
    x = np.convolve(clicks, k, "same")
    return band(x, 1500, 7000) * 6 * env(len(t), 0.05, 0.2, 0.9, 0.2) + band(noise(len(t)), 200, 900) * 0.15


def hiss(sec):
    t = t_axis(sec)
    return band(noise(len(t)), 300, 3500) * env(len(t), 0.3, 0.2, 0.8, 0.4) + band(noise(len(t)), 40, 160) * 0.8 * env(len(t), 0.2, 0.2, 0.7, 0.4)


def slash(sec):
    t = t_axis(sec)
    sweep = osc(3000 * (400 / 3000) ** (t / sec), "saw") * np.exp(-t * 5)
    return band(noise(len(t)), 1500, 8000) * np.exp(-t * 9) + band(sweep, 300, 6000) * 0.6


def wash(sec):
    t = t_axis(sec)
    return band(noise(len(t)), 150, 2500) * np.sin(np.pi * t / sec) ** 2 + band(noise(len(t)), 40, 200) * np.sin(np.pi * t / sec) * 0.5


def bubbles(sec):
    out = band(noise(int(SR * sec)), 40, 220) * 0.8
    for _ in range(int(sec * 14)):
        s = int(rng.uniform(0, sec - 0.15) * SR)
        tt = t_axis(0.12)
        f = rng.uniform(140, 420)
        b = osc(f * (1 + tt * 6)) * np.exp(-tt * 30)
        out[s:s + len(b)] += b * 0.6
    return out


def chord_shimmer(sec, freqs=(523, 659, 784, 1047)):
    t = t_axis(sec)
    x = sum(osc(np.full(len(t), f)) * (1 + 0.3 * np.sin(2 * np.pi * (5 + i) * t)) for i, f in enumerate(freqs))
    return x * env(len(t), 0.25, 0.2, 0.8, 0.35) * 0.4


FX = {
    "BEAM": lambda s: mix(charge_whine(s * 0.4), delayed(beam_buzz(s * 0.6, 95), s * 0.38), sec=s),
    "SPIRAL_BEAM": lambda s: mix(charge_whine(s * 0.4, 180, 1800), delayed(beam_buzz(s * 0.6, 130) * (1 + 0.5 * np.sin(np.linspace(0, 60, int(SR * s * 0.6)))), s * 0.38), sec=s),
    "CRYSTAL_RAIN": lambda s: mix(chimes(s, 7, 1500), delayed(shatter(s * 0.5) * 0.7, s * 0.5), sec=s),
    "CRYSTAL_PILLARS": lambda s: mix(charge_whine(s * 0.5, 90, 600) * 0.6, delayed(shatter(s * 0.5), s * 0.5), delayed(boom(0.6) * 0.6, s * 0.5), sec=s),
    "GRAVITY_TORNADO": lambda s: mix(whoosh(s, 2.5), band(charge_whine(s, 60, 30), 20, 300), sec=s),
    "SWARM": lambda s: chatter(s),
    "MIST": lambda s: hiss(s),
    "HORN_SLASH": lambda s: mix(charge_whine(s * 0.25, 600, 2400) * 0.5, delayed(slash(s * 0.7), s * 0.25), sec=s),
    # Godzilla Ultima: âm riêng từng form
    "GILL_SPRAY": lambda s: mix(hiss(s) * 0.8, band(charge_whine(s, 300, 160), 200, 2500) * 0.5, sec=s),
    "FIN_WAKE": lambda s: mix(wash(s), slash(s * 0.6) * 0.5, sec=s),
    "GEYSER": lambda s: mix(delayed(boom(0.9, 55, 30), s * 0.4), delayed(wash(s * 0.6), s * 0.4), sec=s),
    "CHARGE_TRAIL": lambda s: mix(*[delayed(boom(0.35, 80, 40) * 0.7, s * 0.12 * i) for i in range(6)], sec=s),
    "LAVA_GEYSER": lambda s: mix(bubbles(s), delayed(boom(0.8, 60, 28), s * 0.55), sec=s),
    "SCALE_SHRAPNEL": lambda s: mix(whoosh(s * 0.7, 6), delayed(chatter(s * 0.4, 90), s * 0.55), sec=s),
    "CLAW_MARKS": lambda s: mix(*[delayed(slash(0.5), 0.25 + 0.09 * i) for i in range(3)], sec=s),
    "SONIC_ROAR": lambda s: mix(roar(s, 90, 160, 0.4, [(200, 900, 1.0), (1200, 3000, 0.5)]), chord_shimmer(s, (1319, 1568, 1976)) * 0.3, sec=s),
    "SPINE_LANCES": lambda s: mix(charge_whine(s * 0.3, 800, 2400) * 0.4, *[delayed(shatter(0.4) * 0.5, s * 0.45 + 0.05 * i) for i in range(4)], sec=s),
    "CRIMSON_BREATH": lambda s: mix(charge_whine(s * 0.25, 100, 500) * 0.5, delayed(drive(beam_buzz(s * 0.75, 70, 0.6) + band(noise(int(SR * s * 0.75)), 60, 600), 4), s * 0.22), sec=s),
    "RED_PILLARS": lambda s: mix(*[delayed(boom(0.5, 120, 40) * 0.6 + band(noise(int(SR * 0.5)), 1500, 6000) * np.exp(-t_axis(0.5) * 8) * 0.4, s * 0.4 + 0.07 * i) for i in range(5)], sec=s),
    "SPINE_CHARGE": lambda s: mix(*[delayed(osc(np.full(int(SR * 0.18), 300 + 90 * i)) * np.exp(-t_axis(0.18) * 14) * 0.5, 0.09 * i) for i in range(9)], sec=s),
    "RED_SHOCKWAVE": lambda s: mix(delayed(roar(1.4, 60, 110, 0.6, [(80, 500, 1.2), (600, 1800, 0.6)]), s * 0.2), delayed(boom(1.2, 70, 20) * 1.3, s * 0.55), sec=s),
    "NOVA": lambda s: mix(charge_whine(s * 0.5, 120, 900) * 0.4, delayed(boom(s * 0.6, 90, 25) * 1.4, s * 0.45), delayed(shatter(0.6) * 0.4, s * 0.45), sec=s),
    # ★ của từng form Ultima
    "WHIRLPOOL": lambda s: mix(whoosh(s, 3.5) * 0.8, wash(s) * 0.6, band(charge_whine(s, 140, 60), 40, 400) * 0.6, sec=s),
    "STEAM_JETS": lambda s: mix(hiss(s), delayed(hiss(s * 0.8) * 0.8, 0.18), band(noise(int(SR * s)), 3000, 8000) * env(int(SR * s)) * 0.3, sec=s),
    "MAGMA_RING": lambda s: mix(band(drive(noise(int(SR * s)), 3), 30, 240) * env(int(SR * s), 0.3, 0.3, 0.6, 0.4) * 0.8,
                                delayed(bubbles(s * 0.5), s * 0.5), delayed(boom(0.8, 55, 25), s * 0.55), sec=s),
    "STOMP_TRAIL": lambda s: mix(*[delayed(boom(0.3, 95, 35) * 0.8 + bubbles(0.3) * 0.3, s * 0.1 + 0.12 * i) for i in range(7)], sec=s),
    "ICE_BEAM": lambda s: mix(charge_whine(s * 0.35, 900, 3200) * 0.5, delayed(beam_buzz(s * 0.65, 180, 1.4) * 0.7, s * 0.33),
                              delayed(chimes(s * 0.5, 9, 2200) * 0.6, s * 0.6), sec=s),
    "ICE_SPIKES": lambda s: mix(*[delayed(shatter(0.35) * 0.6 + chimes(0.35, 2, 1800 + 120 * i) * 0.4, s * 0.3 + 0.08 * i) for i in range(8)], sec=s),
    "TAIL_ARC": lambda s: mix(whoosh(s * 0.7, 1.2), delayed(boom(0.6, 75, 30), s * 0.55), delayed(wash(0.6) * 0.6, s * 0.55), sec=s),
    "BLOOD_SPIRAL": lambda s: mix(charge_whine(s * 0.4, 120, 900) * 0.5,
                                  delayed(drive(beam_buzz(s * 0.6, 85, 0.8) * (1 + 0.6 * np.sin(np.linspace(0, 70, int(SR * s * 0.6)))), 3), s * 0.38),
                                  delayed(boom(0.7, 80, 30) * 0.9, s * 0.8), sec=s),
}

SKILLS = {
    "SG_CRYSTAL_MISSILE": ["CRYSTAL_RAIN"],
    "SG_GRAVITY_TORNADO": ["GRAVITY_TORNADO"],
    "SG_CORONA_BEAM": ["SPIRAL_BEAM"],
    "SG_CRYSTAL_FORTRESS": ["CRYSTAL_PILLARS"],
    "SG_COSMIC_NOVA": ["CRYSTAL_RAIN", "NOVA"],
    "DS_MICRO_OXYGEN": ["BEAM"],
    "DS_HORN_SLASH": ["HORN_SLASH"],
    "DS_SWARM_SPLIT": ["SWARM"],
    "DS_OXYGEN_MIST": ["MIST"],
    "DS_DESTRUCTION_OMEGA": ["MIST", "NOVA"],
    "UL1_GILL_SPRAY": ["GILL_SPRAY"],
    "UL1_FIN_DIVE": ["FIN_WAKE"],
    "UL1_FLUKE_GEYSER": ["GEYSER"],
    "UL2_RAMPAGE_CHARGE": ["CHARGE_TRAIL"],
    "UL2_LAVA_VENTS": ["LAVA_GEYSER"],
    "UL2_SCALE_SHRAPNEL": ["SCALE_SHRAPNEL"],
    "UL3_CLAW_REND": ["CLAW_MARKS"],
    "UL3_SONIC_ROAR": ["SONIC_ROAR"],
    "UL3_SPINE_LANCE": ["SPINE_LANCES"],
    "UL4_CRIMSON_BREATH": ["CRIMSON_BREATH"],
    "UL4_SKYFIRE": ["RED_PILLARS"],
    "UL4_FINAL_ROAR": ["SPINE_CHARGE", "RED_SHOCKWAVE"],
    "UL1_RIPTIDE_COIL": ["WHIRLPOOL"],
    "UL1_SCALD_TORRENT": ["STEAM_JETS"],
    "UL2_MAGMA_QUAKE": ["MAGMA_RING"],
    "UL2_MOLTEN_STAMPEDE": ["STOMP_TRAIL"],
    "UL3_GLACIER_BREATH": ["ICE_BEAM"],
    "UL3_FROST_CROWN": ["ICE_SPIKES"],
    "UL4_TAIL_CRUSH": ["TAIL_ARC"],
    "UL4_BLOOD_SPIRAL": ["BLOOD_SPIRAL"],
}


def skill_pov(body, beam, tail):
    """Âm cảnh POV 3 màn như Godzilla (2.6s): tụ lực dâng dần -> lặng chân không -> phóng chiêu -> nổ slow-mo."""
    sec = 2.6
    rise = mix(body(1.05), charge_whine(1.05, 120, 1600) * 0.5, sec=1.05) * np.linspace(0.3, 1.0, int(SR * 1.05))
    return mix(rise, delayed(beam(0.75), 1.12), delayed(tail(0.9), 1.72), delayed(boom(0.9, 70, 22) * 1.2, 1.75), sec=sec)


POVS = {
    # SpaceGodzilla: chuông tinh thể dâng lên, tia xoắn, vỡ tinh thể
    "spacegodzilla": lambda: skill_pov(lambda s: chimes(s, 10, 1300), lambda s: FX["SPIRAL_BEAM"](s), lambda s: shatter(s)),
    # Destoroyah: tiếng rít + lõi ngực ù, tia oxy, sương rã phân tử
    "destoroyah": lambda: skill_pov(lambda s: roar(s, 240, 420, 0.8, [(400, 1500, 0.8), (1800, 4200, 0.9)], screech=0.6) * 0.5,
                                    lambda s: FX["BEAM"](s), lambda s: hiss(s)),
    # Ultima: gầm trầm nhiều lớp, hơi thở huyết hạch, xung kích
    "ultima": lambda: skill_pov(lambda s: roar(s, 70, 120, 0.6, [(90, 500, 1.2), (600, 1600, 0.7)]) * 0.6,
                                lambda s: FX["CRIMSON_BREATH"](s), lambda s: FX["RED_SHOCKWAVE"](s)),
}

ROARS = {
    # SpaceGodzilla: trầm, có lớp ánh kim tinh thể
    "spacegodzilla": dict(f0=95, f1=150, grit=0.5, formants=[(150, 700, 1.0), (900, 2200, 0.6)], shimmer=0.25),
    # Destoroyah: rít cao và gắt
    "destoroyah": dict(f0=240, f1=420, grit=0.8, formants=[(400, 1500, 0.8), (1800, 4200, 0.9)], screech=0.6),
    # Ultima: rất trầm, nhiều lớp
    "ultima": dict(f0=70, f1=120, grit=0.6, formants=[(90, 500, 1.2), (600, 1600, 0.7), (2000, 3800, 0.3)], shimmer=0.1),
}


def main():
    OUT.mkdir(parents=True, exist_ok=True)
    for name, kw in ROARS.items():
        if len(sys.argv) > 1:
            break
        r = roar(2.1, **kw)
        save(f"sfx_titan_{name}_roar.wav", r)
        pov = mix(band(noise(int(SR * 4.8)), 25, 120) * np.linspace(0.2, 1.0, int(SR * 4.8)) * 0.6,
                  delayed(boom(1.6) * 1.2, 0.6), delayed(r, 2.0), delayed(boom(1.2, 60, 22), 4.1), sec=4.8)
        save(f"sfx_boss_pov_{name}.wav", pov)
    only = set(sys.argv[1:])            # vd. python tools/tao_am_thanh_boss.py UL  -> chỉ tạo âm có chứa "UL"
    for key, kinds in SKILLS.items():
        if only and not any(o in key for o in only):
            continue
        sec = 2.0 if len(kinds) > 1 else 1.6
        save(f"sfx_titan_{key.lower()}.wav", mix(*[FX[k](sec) for k in kinds], sec=sec))
    for name, fn in POVS.items():
        if only and not any(o in "POV_" + name.upper() for o in only):
            continue
        save(f"sfx_titan_skill_pov_{name}.wav", fn())


if __name__ == "__main__":
    main()
