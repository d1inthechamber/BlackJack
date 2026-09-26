# Casino Chaos 3.4

Expands Royal Felt / BlackJack into a five-game, seven-room offline casino. Package ID and development signing key remain unchanged for sideload updates. This is a development APK, not a Play Store production release.

## 3.4 artwork and animation repair

- Packages the user's tattooed hand cutouts, Trump virtual banknote, worn neon alley, and seven-room slot emblem atlas. Source IDs and checksums are in `art/casino-chaos/approved-v34-assets.json`. Packaging only resizes and encodes the supplied images; all existing character artwork is retained.
- Street craps always uses the street setting and displays the current original room character. Tapping the hand shakes and throws the dice. Both wagers appear before the throw. The pot stays centered through the point, the winning hand reaches and grips it, then hand and bills travel together to that player's edge.
- Slots use the approved painted symbols, dark reel windows and continuous reel travel that slows onto the already saved result. No odds or payout changes.
- Craps adapts to a side-by-side landscape layout. Navigation and content share the same safe screen area so controls do not overlap the system bars.
- Wallet, casino state and craps pot are now written in one atomic snapshot. Existing v3.2/v3.3 archives and the craps sidecar are read on migration. Saving or resuming a decided hand never re-awards the payout.
- Solitaire retains stack dragging, draw one/three, undo and hints. Dead-deal checks now consider the cards actually reachable by drawing three and ignore shifting an entire king stack between empty columns.

The build gates include lint, rules and animation tests, migration and payout persistence checks, packaged-image transparency checks, phone screenshots for all seven slot themes and both craps collectors, and folded/unfolded/landscape screenshots.

- Blackjack retains its rules, dealer sprites, sounds, flights, and legacy save migration.
- Slots: one payline, independent 20-stop reels, weights 6/5/4/3/2, triple payouts 5/8/15/30/60 times stake. Exactly two lowest symbols pay 2 times stake. Exhaustive theoretical return 91.925%. Outcomes are saved with the wallet before cosmetic reel animation, so closing cannot replay a wager.
- Solitaire: 52-card Klondike, draw one or three, unlimited recycling, alternating descending tableau sequences, kings in empty columns, ascending same-suit foundations, manual moves from foundations, undo and hints. Win reward 100 chips once per deal; undo disabled after completion.
- Poker: four-seat no-limit Texas Hold'em, rotating 5/10 blinds, full best-five evaluation, side pots, unmatched refunds, split pots and clockwise odd chips. Short all-ins do not reopen action unless the cumulative increase reaches a full raise. Buy-in 100–500 chips, cash out only between hands. Busted bots re-buy between hands. No rake.
- Street Craps: pass-line play against the current room character. A matched pile of fictional U.S.-style bills stays centered through the point, and the winner’s hand visibly collects it after the deciding throw.
- Five bot personalities and noisy gestures. Decisions receive only own cards, public board, betting amounts, player count, and the human's public raise count. Simulated unseen cards never use the actual future deck. The Veteran adjusts to frequent human raises. Room-specific names and animated original vector portraits.
- Shared wallet and all game states use a single atomic serialized archive, migrated from the prior blackjack save. Poker table stack is escrowed outside the lobby wallet. Existing blackjack hand must finish before entering other games. Games pause off-screen and resume on return. Fresh casino reset requires confirmation.
- Common room backgrounds, card backs, music and persistent music switch. New Casino Chaos launcher name and generated icon.

Icon source: built-in image generation; final packaged artwork `app/src/main/res/drawable-nodpi/chaos_icon.webp`. Prompt: dark dingy 1970s Vegas / 90s cartoon launcher mark, bold ivory ace across red/gold chip and slot seven, distressed brass, magenta and turquoise neon, no words or recognizable characters. Source PNG retained in the generating conversation. Resized only for Android density resources.

Rules references used during implementation:
https://www.pokerstars.com/poker/games/rules/
https://www.pokerstars.fr/en/help/articles/poker-rules-master/
https://bicyclecards.com/how-to-play/klondike

Validation: fixed-odds exhaustive payout test, card conservation, solitaire move/undo/recycle checks, hand ranking, layered pots/odd chips, short all-in reopening, bot information boundary, simulated hand chip conservation, full save serialization; emulator tests cover navigation, wagering, cash-out, recreation and screenshots alongside existing blackjack regression tests.

## Green Room and table staging update

Added The Green Room with reference-based masked, bucket-hat champion dealer, twelve poses, cannabis-themed background and card backs, its own original instrumental loop, slot symbols and poker cast. Added independent persistent music/effects/voices/ambience controls on a separate settings screen accessible from the fixed top-right button on every game screen.

Dealer is occluded by a physical felt table and rail, with forearms layered over the surface; larger angled shoe remains visible beside the dealing hand. Removed whole-body idle wink jumps and unnecessary reverse contact pose; slowed laugh cadence. Short recorded human grunt/groan/laugh effects react once to new completed hands and Street Craps results, never loop, and stop when voices are muted. Sources and licenses are documented in `AUDIO-CREDITS.md`.

## 3.3 release update

- Added fixed Games, Rooms, and Settings navigation with animated casino transitions.
- Split Rooms and Settings into separate high-contrast screens with visual-test coverage.
- Added dead-deal detection to solitaire without changing the v3.2 serialized archive shape.
- Decluttered poker opponents, retained the existing room character artwork, and added animated emotional reactions plus recorded human reaction audio.
- Preserved all approved dealer and opponent sprite artwork. Direct legacy celebrity and literary-character labels were replaced with neutral role descriptions only; the characters themselves were not replaced.
- Stores Street Craps in a versioned sidecar so existing v3.2 blackjack/casino saves continue to load unchanged.

Generated Green Room art used the built-in image tool: reference-based 4x3 cel-animated dealer atlas (no text, cards or grid); empty smoky 1970s green/amber lounge; antique gold cannabis-leaf card back. Packaging only resized these images.
