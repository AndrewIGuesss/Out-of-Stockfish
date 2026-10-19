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