# Approved animation direction and sixth room

The user approved the interactive carnival pose/timing preview on 2026-09-13, then requested a punk room following their uploaded blue-haired tattooed dealer reference, without the background wordmark. This directory contains the new art source; it is not integrated into the released v2.2 APK yet.

## Next implementation requirements
- Replace the current subtle relief animation with expressive 2D cartoon poses for all dealers.
- Dealer reaches onto a continuously visible shoe, takes the top card, pushes it toward the correct hand, releases it, and returns. Card movement must follow the hand contact.
- Player win: dealer angry. Push: dealer smug. Dealer win: mocking laughter. Include occasional idle expressions.
- Sixth room: The Backroom, a dingy punk venue, with blue spiked hair, tattooed arms, cream shirt and leather cuffs. Bare hands replace boxing gloves for dealing. No backdrop lettering from the supplied reference.
- Matching punk card backs, cyan/pink accents and worn pale card faces.
- Slightly flashier atmosphere; complementary background music per room with a persistent off option separate from card/chip effects.
- Preserve current game state and room selection, blackjack rules and card visibility.

## Pose sheets
4 columns by 3 rows. Order: neutral, reach, touch deck, draw inward; push left, push right, angry, angrier; smug, wink, laugh back, laugh forward. Generated with the built-in image tool from the user-supplied character reference. Green backgrounds are intended for runtime chroma key. Confirm per-sheet crop bounds: generated rows are not necessarily exactly equal. Carnival preview uses normalized row boundaries 0, 356/1086, 702/1086, 1; punk sheet requires its own inspection. Add in-between poses before shipping polished motion.
