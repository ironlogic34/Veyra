# Veyra design-system foundation

## Purpose

This document records the shared UI foundations without deciding the final appearance of Veyra's screens.

The phone UI and tablet landscape UI are separate, intentionally designed experiences. Shared tokens are layout building blocks; they must not force the two layouts into the same composition.

## Implemented tokens

### Spacing

`VeyraSpacing` provides a 4dp-based incremental scale from 4dp to 48dp. It is intended for consistent small gaps and padding values.

These values do not define the spacing of any particular screen. The approved phone and tablet specifications take precedence.

### Motion

`VeyraMotion` exposes semantic duration references:

- Quick: 160ms
- Standard: 300ms
- Emphasized: 440ms

These are starting points informed by the motion reference study. They are not mandatory durations for every animation. Spring motion, gesture response, interruption behavior, and reduced-motion behavior still need component-level decisions.

## Deliberately not finalized

- Brand colour palette and light/dark schemes
- Typography family, scale, and weights
- Corner radii and component shapes
- Elevation, blur, glass material, and fallback behavior
- Haptic feedback rules
- Reduced-motion and accessibility behavior
- Phone-specific and tablet-specific component measurements

Do not infer these from Material defaults or from LastWave. The final design references and user specifications must determine them.

## Implementation rules

1. Prefer semantic tokens over scattered numeric values.
2. Keep screen-specific geometry close to its screen/component specification.
3. Do not create a shared component merely because two screens contain a visually similar control; confirm that behavior and layout are actually shared.
4. Motion must remain interruptible and usable with touch input.
5. Glass and blur effects require a legible fallback on devices where backdrop effects are expensive or unsupported.
6. Validate on the target phone and tablet rather than relying only on previews.

## Next design-system work

After the final palette and screen references are transferred, define colour and typography tokens, then implement shared primitives one at a time. The floating navigation and player controls should be implemented only after their interaction requirements are mapped.
