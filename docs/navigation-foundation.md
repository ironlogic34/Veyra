# Veyra floating navigation foundation

## Implemented

`VeyraNavigationDock` is a configurable Compose navigation component.

- Destination IDs, labels, and icon composables are supplied by the caller.
- The selected destination is controlled by `selectedId`.
- Selection is reported through `onDestinationSelected`.
- Only the selected destination displays its label.
- The selected item animates its content size and label visibility.
- Duplicate destination IDs are rejected.

## Deliberate limits of this first pass

This is the functional selection foundation, not the final Veyra navigation design.

- It does not decide the actual destination list.
- It does not force phone and tablet to use the same navigation arrangement.
- It does not yet implement glass/backdrop blur.
- It does not yet implement slide-across gesture selection.
- Final dimensions, colors, shadows, and haptics remain open.

The component accepts styling parameters so the approved phone and tablet specifications can tune it independently. The destination owner remains responsible for navigation state and back-stack behavior.
