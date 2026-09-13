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
