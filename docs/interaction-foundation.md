# Veyra interaction foundation

## Press response

The first shared interaction primitive is `Modifier.veyraPressScale`.

- Observes an `InteractionSource` supplied by the component.
- Animates scale while the source reports a pressed state.
- Defaults to 0.96x.
- Provides 0.98x (subtle) and 0.88x (expressive) semantic references.
- Uses a spring with damping ratio 0.5 and stiffness 300, based on the interaction reference study.

The modifier does not create click behavior, change semantics, add haptics, or decide a component's hit area. The caller remains responsible for accessible click semantics and passing the same interaction source to the clickable element.

Example:

```kotlin
val interactionSource = remember { MutableInteractionSource() }

Box(
    modifier = Modifier
        .veyraPressScale(interactionSource)
        .clickable(
            interactionSource = interactionSource,
            indication = null,
            onClick = onClick
        )
)
```

## Boundaries

- Do not apply press scaling to every clickable by default.
- Select the scale per component specification.
- Keep haptic behavior explicit and component-specific until its rules are approved.
- Do not implement floating navigation, draggable player controls, or glass effects as generic primitives before their individual behavior is mapped.
