# PhotoEdit UI Design Standard

This document is the project-wide source of truth for interaction design, mobile layout, usability, and the relationship between UI and business behavior. Read it before changing any screen or user flow.

## 1. Product experience principles

- **User feel comes first:** the app should feel predictable, responsive, calm, and easy to operate with one hand. Prefer familiar Android interaction patterns over clever but unfamiliar controls.
- **Make the next action obvious:** users should understand where they are, what they can do, what will happen after tapping, and how to recover if something fails.
- **Reduce friction:** avoid unnecessary screens, repeated confirmation, hidden controls, excessive typing, and settings that interrupt the main task.
- **Give immediate feedback:** every meaningful action should respond with a visible state change, progress indication, result, or clear explanation. Prevent duplicate submissions while work is in progress.
- **Respect user control:** never start a generation, upload, export, paid API request, deletion, or other consequential operation without an explicit user action.
- **Keep trust:** explain permissions, network use, API charges, quality tradeoffs, destructive actions, and data retention at the point where they matter. Never imply an estimate is an exact charge.

## 2. Mobile-first layout

- Design for portrait phones first, including narrow devices and common Android screen sizes.
- Respect status/navigation bars, display cutouts, gesture navigation, safe drawing insets, and keyboard/IME resizing.
- Primary actions must remain reachable and must not be obscured by the keyboard, bottom navigation, dialogs, or system bars.
- Use vertical scrolling for content that exceeds the viewport. Avoid horizontal scrolling for essential actions and settings.
- Do not place several long chips, tabs, or buttons in a fixed horizontal Row where they can be clipped. Prefer adaptive wrapping, FlowRow, deliberate multiple rows, or a vertically grouped selector.
- Keep touch targets comfortably tappable (prefer at least 48dp where practical), provide sufficient spacing, and avoid tightly packed controls.
- Check long Chinese and English labels, model IDs, error messages, small screens, large font settings, and display scaling.
- Avoid fixed heights for text-heavy regions. Use adaptive constraints and scrolling so content can grow.
- Keep the main task and primary CTA visually prominent; secondary actions should not compete with it.

## 3. Interaction feel and feedback

- Use familiar Material 3 patterns and the existing PhotoEdit theme/components.
- Provide clear pressed, selected, disabled, loading, success, and error states.
- Disable or guard repeated taps during non-repeatable work; do not make the UI appear frozen.
- Show progress for operations that take noticeable time. For longer operations, communicate what is happening and preserve user input.
- Preserve drafts and selections when safe if a request fails. Do not silently discard user-entered content.
- Make retry behavior explicit. Avoid automatic retries that could duplicate paid requests or destructive actions.
- Use concise, plain-language Chinese labels. Avoid technical jargon unless it is accompanied by a useful explanation.
- Provide confirmation only for consequential or irreversible actions; do not over-confirm routine navigation.

## 4. UI must reflect business logic

- Design the flow from the user's task and domain rules, not from the visual component alone. Before changing a screen, map the task's steps, inputs, constraints, side effects, and success/failure outcomes.
- The UI must expose the choices users need, and must not expose choices that the business layer cannot actually honor.
- Show the currently active provider/model/format/quality/destination when those choices affect the result, cost, or compatibility.
- Explain dependencies and unavailable states at the point of use (for example, missing API key, no network, quota exhausted, unsupported file, permission denied).
- Distinguish estimated cost from actual usage and actual balance. Never invent prices, quota, progress, or success.
- For paid or network operations, show the selected route and meaningful cost information before execution when available; clearly state when price or balance cannot be verified.
- Keep validation and business rules in domain/use-case/ViewModel layers, not in Composables. UI should render state and emit user intents.
- Model state transitions explicitly: idle, validating, loading/submitting, success, recoverable error, and unavailable/disabled as relevant.
- Avoid controls whose labels or appearance promise behavior that the implementation does not provide.
- Preserve important context across navigation and errors. Avoid losing a selected image, prompt draft, model, or export settings unexpectedly.

## 5. Information hierarchy and visual consistency

- Maintain consistent typography roles, spacing, color semantics, shapes, elevations, and icon usage.
- Prefer the existing Material 3 theme and reusable components. If a new pattern is needed, make it reusable and document it here.
- Use cards and dividers only when they clarify grouping; avoid wrapping every small item in a card.
- Keep headings concise and metadata visually secondary. Use color to communicate meaning, not decoration alone.
- Use icons with text for less familiar actions. Do not rely on color alone to communicate status.
- Keep settings organized into understandable groups, with current values visible and explanations close to advanced options.

## 6. Accessibility and localization

- Use meaningful content descriptions for informative icons/images and hide purely decorative elements from accessibility where appropriate.
- Support font scaling and adequate contrast. Do not truncate essential labels or status messages.
- Ensure controls have clear accessible names and logical focus order.
- Use localized, concise wording. Avoid hard-coded assumptions about a particular device size or language length.

## 7. Screen review checklist

Before completing a UI change, review:

1. User goal and end-to-end task flow: is the next step obvious?
2. Business rules: do choices, validation, pricing, permissions, and side effects match actual behavior?
3. Phone layout: narrow and typical widths, portrait, insets, keyboard, dialogs, and scrolling.
4. Reachability: primary and secondary actions are visible and tappable.
5. States: empty, loading, success, error, selected, disabled, and long-content states.
6. Resilience: failure preserves safe user input and provides a clear recovery path.
7. Accessibility: touch target size, font scaling, contrast, semantics, and focus order.
8. Consistency: reuse theme/components and align with existing screens.
9. Tests/validation: run available UI/domain tests or previews when relevant; if device validation is not performed, state that clearly.

## 8. Change management

- Read this document, AGENTS.md, PROJECT.md, and the relevant screen/theme/components before UI work.
- Update this document when a reusable interaction or design-system rule changes.
- Record each meaningful UI change and validation status in PROGRESS.md in the same session; update PROJECT.md when architecture, user-visible behavior, or product-wide constraints change.
- Never record secrets, account identifiers, personal data, or private operational details in this document.
