# Dealer, version 2.1

The original painted dealer is rendered as an articulated textured relief in OpenGL ES.
The mesh has anatomical depth and independently weighted neck and arms. It is designed
for the fixed table camera and small movements, not for 360-degree viewing. The original
face, hair, glasses, suit, shirt and waistcoat are preserved in the existing artwork.

A full character that can turn sideways or walk around would still need a complete
sculpt, side/back textures, and a full character rig. This build does not claim that.

The checked-in `app/debug-signing.p12` is an intentionally public development signing
key, using the standard Android debug password. It is for this virtual-chip test APK,
not production releases. Keeping it stable permits future sideloaded updates to retain
local saved games. Versions before 2.1 used different ephemeral build keys and require
uninstalling before installing 2.1.

Cards now originate from the skinned palm position, travel to the measured destination,
and become visible in their hand on arrival. Duplicate ranks/suits from separate decks
are tracked by identity. Split-card flights run sequentially, and action controls wait
until cards land. Bankroll is shown alongside the bet controls.

## Room sets (2.2)
Five selectable rooms preserve the same saved hand and bank. Each new room has a background, themed card backs and paper/border colors, and its own articulated painted dealer. The user approved the 2D art direction. New dealer textures use a green-screen key in the fragment shader; Vegas retains its original alpha texture. The room preference is stored separately from the game save.

Carnival follows the supplied costume photo and text-free card character reference. Pharaoh’s Palace uses Tutankhamun, Ironworks uses Ebenezer Scrooge, West Coast ’94 uses Eazy-E.
