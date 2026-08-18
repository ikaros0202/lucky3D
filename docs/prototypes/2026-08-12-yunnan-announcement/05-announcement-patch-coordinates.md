# Local crystal announcement patch coordinates

- Base image (the only source): `app/src/main/res/drawable-nodpi/home_crystal_content_shell_tall.png`, 904 x 1740 px, RGB.
- Licensed cleanup crop: `x=160..743, y=930..1149` (584 x 220 px). This crop contains the old two capsule rows and is the only area allowed to be replaced.
- Imagegen cleanup source: `05-imagegen-announcement-cleanup-raw.png`. Its center texture is resized into the licensed cleanup crop to remove the old capsule borders and glyph.
- Unified panel source: `05-imagegen-announcement-panel-raw.png`, center crop `x=70..1974, y=88..624`, resized to 523 x 182 px.
- Panel placement in the base image: top-left `(182,941)`, exclusive bottom-right `(705,1123)`; feathered alpha edge about 4 px.
- Composite order: feathered cleanup texture in the licensed crop first, then the feathered unified panel at `(182,941)`.
- All pixels outside `x=160..743, y=930..1149` remain byte-for-byte from the base image. No spheres, attribute panel, platform, toolbar, or navigation artwork was redrawn.
