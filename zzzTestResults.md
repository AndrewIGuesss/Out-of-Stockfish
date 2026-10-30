## All matches: fastchess, 10+0.1, UHO_Lichess_4852_v1.epd, paired openings. ##
### Elo = new vs old. Ptnml = pentanomial (pairs scoring 0, 0.5, 1, 1.5, 2). ###

| # | Date | Change | Baseline | Type | Games | Elo | Result |
|---|------|--------|----------|------|-------|-----|--------|
| 1 | Oct 8 | Tapered eval | pre-tapered | SPRT [0, 10] | 442 | +74.21 ± 27.41 | accepted |
| 2 | Oct 9 | Refactor (piece values in enum, move gen helpers, constants, time check every 1024 nodes) | tapered | bench only | n/a | n/a | bench identical, ~12-15% faster |
| 3 | Oct 13 | Reverse futility pruning (90/depth, max depth 6) | refactor | fixed 200 | 200 | +83.20 ± 41.78 | accepted |
| 4 | Oct 13 | Futility pruning (125/depth, max depth 3) | RFP | SPRT [0, 10] | 1654 | +20.61 ± 12.86 | accepted |
| 5 | Oct 14 | History aging (halve instead of reset) | FP | SPRT [0, 10] | stopped after 3900 | −6 ± 9 | rejected |
| 6 | Oct 14 | King safety v1 (pawn shield 12/6, open file −10/−20, middlegame only) | FP | SPRT [0, 10] | 1702 | +22.28 ± 14.13 | accepted |
| 7 | Oct 14 | King safety v2 (attack zone, CPW weights N/B 20 R 40 Q 80) | KS v1 | SPRT [0, 10] | 3062 | −5.79 ± 10.72 | rejected |
| 8 | Oct 15 | make/unmake |KS v1 | SPRT [0,10] | 828 | +37.91 ± 18.60 | accepted |
| 9 | Oct 19 | Mobility (N 4, B 5, R 2/4, Q 1/2 per safe square, mg/eg) | make/unmake | SPRT [0, 10] | 1080 | +33.56 ± 18.20 | accepted |
| 10 | Oct 19 | Zobrist en passant key | mobility | SPRT [-5, 0] | 1462 | +10.71 ± 12.45 | stopped early (LLR ~1.1), merged |
| 11 | Oct 19 | LMP v1 (depth ≤ 3, 3 + d²) | qsearch ep/promo | SPRT [0, 10] | 398 | −24.48 ± 28.11 | stopped (LLR ~−1.2), rejected |
| 12 | Oct 20 | LMP v2 (depth ≤ 2, 12 + 4d²) | qsearch ep/promo | SPRT [0, 10] | 7442 | +8.03 ± 6.48 | accepted |
| 13 | Oct 20 | LMR table (0.75 + ln(d)·ln(m)/2.25, clamped to depth ≥ 1) | LMP v2 + PV | SPRT [0, 10] | 3362 | +12.82 ± 9.60 | accepted |
| 14 | Oct 20 | Time management v1 (soft = rem/30 + 0.75·inc, hard = 3×soft capped at rem/4, finish started depth) | LMR table | SPRT [0, 10] | 280 | +128.38 ± 34.76 | accepted (early stop, likely overestimated) |
| 15 | Oct 21 | SEE (incl. en passant), qsearch skips captures with SEE < 0 | time mgmt | SPRT [0, 10] | 1222 | +29.35 ± 16.41 | accepted |
| 16 | Oct 22 | SEE move ordering (SEE < 0 captures after quiets, BAD_CAPTURE_BASE + MVV-LVA; qsearch SEE check folded into ordering) | SEE | SPRT [0, 10] | 1068 | +29.02 ± 15.77 | accepted |
| 17 | Oct 22 | Pawn precompute (per-file pawn bitmasks; doubled/isolated/passed + king open files use masks instead of board scans) | SEE ordering | bench only | n/a | n/a | bench identical (158358), ~12% faster (~130k → ~148k NPS) |
| 18 | Oct 22 | Mobility v2a: safe squares (skip squares attacked by enemy pawns, per-file pawn attack masks) | pawn precompute | SPRT [0, 10] | 1496 | −15.57 ± 14.32 | rejected (H0) |
| 19 | Oct 23 | Fix passed pawn precedence (ahead mask now applies to all 3 files, was only the right-hand one) | pawn precompute | SPRT [-5, 0] | 11504 | +2.99 ± 5.15 | accepted (H1) |
| 20 | Oct 23 | Mobility v2b: log-curve mobility tables (a·ln(n+1) − b, fit to v1 at the 25th/75th percentile counts) | passed pawn fix + UCI fixes | SPRT [0, 10] | 3952 | −3.34 ± 8.97 | rejected (H0) |
| 21 | Oct 27 | Texel tuning (params array refactor + 548 values tuned on 725k quiet-labeled positions, 81 rounds, error 0.0630 → 0.0594) | main | SPRT [0, 10] | 336 | +105.50 ± 33.99 | accepted (early stop, likely overestimated) |
| 22 | Oct 27 | SEE shortcut (skip see() when the victim is worth ≥ the attacker) + mobility ray lengths (sliders loop a precomputed step count, no inBounds) | Texel tuning | bench only | n/a | n/a | bench identical (172373), no measurable NPS change (~272.7k vs ~272.9k) |
| 23 | Oct 27 | Mobility safe squares retry (texel-retuned mobility weights, only rook mg changed 3 → 4; tuner error 0.059403 vs main 0.059381) | SEE shortcut + ray lengths | SPRT [0, 10] | 2050 | ≈ −10.5 ± 12.7 | rejected (H0, rebuilt from config.json) |
| 24 | Oct 28 | Threats (pawn attacks on N/B/R/Q, minor attacks on R/Q, rook attacks on Q; counted in mobilityCount; texel-tuned, error 0.059381 → 0.059165) | safe squares retry rejected | SPRT [0, 10] | 1854 | ≈ +20.3 ± 13.1 | accepted (H1, rebuilt from config.json) |
| 25 | 2026-10-28 | Allocation-free movegen (int moves, per-ply buffers, sort removed) | main e414944 (threats) | speed | 270 | +141.11 ± 37.18 | H1 (LLR 2.96, [0, 10], early stop) |
| 26 | 2026-10-29 | Hot-path cleanup + PV-node fix (LMP/FP/RFP use entry window), UCI mate scores, SAN matching fix | main (moves-remake merged) | correctness + speed | 2730 | +18.85 ± 10.36 | H1 (LLR 2.96, [-5, 0]) |