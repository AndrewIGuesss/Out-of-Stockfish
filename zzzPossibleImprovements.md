# Added Features #
| Feature | What it is | Gain | Difficulty |
|---|---|---|---|
| **Transposition table** | Remember positions already searched. | 2-4×, 10×+ in endgames | 3 |
| **Null move pruning** | "If I skip my turn and I'm *still* winning, this position is so good I can cut it off early." | ~2-3×, one of the biggest | 2 |
| **Late move reductions (LMR)** | Search moves late in the ordered list at reduced depth, and only re-search if they surprise you. | ~2-5× with good ordering | 3 |
| **Principal variation search** | Search the first move fully and the rest with a tiny "prove it's worse" window. | 10-20% | 2 |
| **Delta pruning** | In quiescence, skip captures that can't possibly matter. | 10-20% | 1 |

# What Stockfish Has That My Engine Doesn't

Rough estimates only. Real numbers vary a lot by engine and position.

**Difficulty:** 1 = an evening, 5 = a major project or rewrite.

## Board and move generation (raw speed)

| Feature | What it is | Gain | Difficulty |
|---|---|---|---|
| **Bitboards** | Store the board as twelve 64-bit `long`s (one per piece type/color) instead of a `Piece[8][8]`. Moves and attacks get calculated with bit operations. | 10-50× raw speed | 5 (basically a rewrite) |
| **Magic bitboards** | A lookup trick for instant rook/bishop/queen attacks, built on top of bitboards. | Part of the above | 4 |
| **Make/unmake** | Change one board and undo, instead of `new Board()` at every node. | 2-5× | 3 |
| **Legal move generation** | Handle pins and checks during generation, instead of pseudo-legal + copy + `inCheck` for every move. | 1.5-3× | 4 |
| **Incremental Zobrist hashing** | Update the position hash with a couple of XORs per move, instead of rebuilding a string. | Removes the key-building cost | 2 |

## Search (searching smarter, not just faster)

| Feature | What it is | Gain | Difficulty |
|---|---|---|---|
| **Aspiration windows** | Start each iteration with a narrow window around last depth's score. | 5-10% | 2 |
| **Futility / reverse futility pruning** | Near the leaves, skip moves that can't possibly raise alpha. | 20-50% | 2 |
| **Static exchange evaluation (SEE)** | Work out whether a capture sequence on a square wins or loses material, without searching. Used for ordering and pruning. | 10-30% | 3 |
| **History + counter-move + continuation history** | Smarter quiet-move ordering, going beyond killers. | 10-30% combined | 2 |
| **Singular extensions** | Search deeper when one move is way better than all the others. | Plays better | 4 |
| **Lazy SMP** | Multiple threads sharing the transposition table. | 1.5-3× | 4 |
| **Time management** | Spend more time on hard moves and less on obvious ones, instead of a flat time per move. | Plays better | 2 |

## Evaluation (judging positions better)

| Feature | What it is | Gain | Difficulty |
|---|---|---|---|
| **NNUE** | A small neural network replaces the hand-written eval, efficiently updated each move. Stockfish's biggest jump in recent years. | Huge (hundreds of Elo) | 5 (needs training data and a training setup) |
| **Tapered eval** | Blend middlegame and endgame scores smoothly based on remaining material, instead of a hard switch at 1300. Removes weird eval jumps when a piece gets traded. | Plays better | 2 |
| **Automated tuning (Texel tuning)** | Tune all eval numbers automatically against thousands of game results, instead of guessing. | Plays a lot better | 3 |
| **More eval terms** | Mobility, king attack zones, outposts, rooks on open files, threats, space. | Plays better | 2 each |

## Other

| Feature | What it is | Gain | Difficulty |
|---|---|---|---|
| **Endgame tablebases** | Precomputed perfect play for positions with 7 or fewer pieces. | Perfect endgames | 3 |
| **UCI protocol** | A standard text interface that lets the engine plug into chess GUIs and play other engines automatically. | Testing | 2 |
| **Massive testing** | Every change gets tested over tens of thousands of games before it's accepted. | Why everything else works | Needs UCI first |
