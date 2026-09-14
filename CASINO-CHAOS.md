# Casino Chaos 3.0

Expands Royal Felt / BlackJack into a four-game, six-room offline casino. Package ID and development signing key remain unchanged for sideload updates. This is a development APK, not a Play Store production release.

- Blackjack retains its rules, dealer sprites, sounds, flights, and legacy save migration.
- Slots: one payline, independent 20-stop reels, weights 6/5/4/3/2, triple payouts 5/8/15/30/60 times stake. Exactly two lowest symbols pay 2 times stake. Exhaustive theoretical return 91.925%. Outcomes are saved with the wallet before cosmetic reel animation, so closing cannot replay a wager.
- Solitaire: 52-card Klondike, draw one or three, unlimited recycling, alternating descending tableau sequences, kings in empty columns, ascending same-suit foundations, manual moves from foundations, undo and hints. Win reward 100 chips once per deal; undo disabled after completion.
- Poker: four-seat no-limit Texas Hold'em, rotating 5/10 blinds, full best-five evaluation, side pots, unmatched refunds, split pots and clockwise odd chips. Short all-ins do not reopen action unless the cumulative increase reaches a full raise. Buy-in 100–500 chips, cash out only between hands. Busted bots re-buy between hands. No rake.
- Five bot personalities and noisy gestures. Decisions receive only own cards, public board, betting amounts, player count, and the human's public raise count. Simulated unseen cards never use the actual future deck. The Veteran adjusts to frequent human raises. Room-specific names and animated original vector portraits.
- Shared wallet and all game states use a single atomic serialized archive, migrated from the prior blackjack save. Poker table stack is escrowed outside the lobby wallet. Existing blackjack hand must finish before entering other games. Games pause off-screen and resume on return. Fresh casino reset requires confirmation.
- Common room backgrounds, card backs, music and persistent music switch. New Casino Chaos launcher name and generated icon.

Icon source: built-in image generation; final packaged artwork `app/src/main/res/drawable-nodpi/chaos_icon.webp`. Prompt: dark dingy 1970s Vegas / 90s cartoon launcher mark, bold ivory ace across red/gold chip and slot seven, distressed brass, magenta and turquoise neon, no words or recognizable characters. Source PNG retained in the generating conversation. Resized only for Android density resources.

Rules references used during implementation:
https://www.pokerstars.com/poker/games/rules/
https://www.pokerstars.fr/en/help/articles/poker-rules-master/
https://bicyclecards.com/how-to-play/klondike

Validation: fixed-odds exhaustive payout test, card conservation, solitaire move/undo/recycle checks, hand ranking, layered pots/odd chips, short all-in reopening, bot information boundary, simulated hand chip conservation, full save serialization; emulator tests cover navigation, wagering, cash-out, recreation and screenshots alongside existing blackjack regression tests.
