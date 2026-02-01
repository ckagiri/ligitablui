# Tailwind CSS Learnings

Practical lessons learned while building and refactoring the LigiPredictor UI with Tailwind CSS.

---

## 1. Responsive Design with Breakpoint Prefixes

Tailwind is mobile-first. Unprefixed utilities apply to all screens; prefixed utilities apply at that breakpoint and up:

```html
<!-- Stack on mobile, side-by-side on sm+ -->
<div class="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-2 sm:gap-0">
```

```html
<!-- Hidden on mobile, visible as table-cell on sm+ -->
<th class="hidden sm:table-cell px-3 py-3 text-right ...">Actual</th>
```

**Common pattern for mobile/desktop text variants:**
```html
<span class="hidden sm:inline">Compare with current standings</span>
<span class="sm:hidden">Show actual positions</span>
```

This gives you shorter labels on mobile and descriptive ones on desktop without JavaScript.

---

## 2. Sticky Positioning for Frozen Table Columns

For tables that scroll horizontally with a frozen first column:

```css
.sticky-col {
    position: sticky;
    left: 0;
    z-index: 10;
    background: inherit;
}
```

Combined with Tailwind's `overflow-x-auto` on the table container:

```html
<div class="overflow-x-auto">
    <table class="min-w-full">
        <td class="sticky-col">...</td>  <!-- Stays visible -->
        <td>...</td>                      <!-- Scrolls -->
    </table>
</div>
```

**Gotcha:** `position: sticky` requires the parent to have a defined overflow context. Make sure no ancestor has `overflow: hidden` that would prevent sticky behavior.

---

## 3. Table Layout with `table-fixed`

```html
<table class="min-w-full table-fixed">
```

`table-fixed` makes column widths respect the `width` values you set (via `w-16`, `w-20`, etc.) rather than auto-sizing based on content. Useful for prediction tables where you want consistent column widths regardless of team name length.

```html
<th class="... w-16">Pos</th>           <!-- Narrow, fixed -->
<th class="...">Team</th>               <!-- Flexible, takes remaining space -->
<th class="... w-20">Actual</th>        <!-- Narrow, fixed -->
```

---

## 4. Combining Tailwind with Dynamic Alpine Classes

When Alpine controls visibility or styling, use `:class` alongside static Tailwind classes:

```html
<tr :class="{
        'cursor-pointer hover:bg-gray-50': alwaysHoverable || canSwap,
        'no-hover': !alwaysHoverable && !canSwap,
        'bg-blue-50': isSelected(team.code),
        'bg-amber-50': isDirty(team.code) && !isSelected(team.code),
        'transition-all duration-200': true
    }"
    class="prediction-row">
```

**Key points:**
- Static classes go in `class="..."`.
- Dynamic/conditional classes go in `:class="{...}"`.
- Alpine merges both — it doesn't replace the static `class` attribute.
- Use `true` as the condition for classes that should always apply but you want grouped with the dynamic ones for readability.

---

## 5. Color Semantics for UI States

Consistent color usage across the app:

| State | Color | Example |
|---|---|---|
| Selected/active | `bg-blue-50`, `text-blue-600` | Selected team row |
| Modified/dirty | `bg-amber-50` | Changed prediction position |
| Success/positive | `text-green-600` | Position moved up, perfect prediction |
| Error/negative | `text-red-600` | Position moved down |
| Neutral/info | `text-gray-500`, `text-gray-600` | Labels, secondary text |
| Open/available | `text-green-600` | Round state "Open" |
| Locked | `text-yellow-600` | Round state "Locked" |
| Completed | `text-blue-600` | Round state "Finalized" |

---

## 6. Inline Badges and Tags

Small status indicators using inline-flex:

```html
<span class="inline-flex items-center px-1.5 py-0.5 bg-gray-100 rounded text-[11px] font-medium text-gray-600">
    <span x-show="!fixture.isHome">@</span>
    <span x-text="fixture.opponent"></span>
</span>
```

- `inline-flex items-center` keeps the badge compact and vertically centered.
- `px-1.5 py-0.5` gives minimal padding.
- `rounded` for pill shape.
- `text-[11px]` uses Tailwind's arbitrary value syntax for sizes not in the default scale.

---

## 7. Transition and Animation Utilities

For smooth state changes:

```html
<tr class="transition-all duration-200">
```

For the swap animation, custom CSS classes are toggled via JavaScript:

```css
.swapping {
    animation: swapHighlight 0.6s ease;
}

.pre-swapping {
    background-color: rgb(254 243 199);  /* amber-100 equivalent */
    transition: background-color 0.08s ease;
}

@keyframes swapHighlight {
    0% { background-color: rgb(254 249 195); }
    100% { background-color: transparent; }
}
```

**Pattern:** Use Tailwind's built-in `transition-*` and `duration-*` for simple hover/state transitions. Use custom CSS + JavaScript class toggling for multi-step animations that Tailwind can't express declaratively.

---

## 8. Hiding Elements Until Alpine Initializes

Prevent flash of unstyled content (FOUC) with `x-cloak`:

```html
<style>
    [x-cloak] { display: none !important; }
</style>

<div x-cloak x-show="someCondition">
    <!-- Hidden until Alpine initializes and evaluates someCondition -->
</div>
```

Without this, elements controlled by `x-show` flash visible for a moment before Alpine hides them.

---

## 9. Form Controls Styled with Tailwind

Checkbox styling that matches the design system:

```html
<input type="checkbox"
       x-model="showStandings"
       class="rounded border-gray-300 text-indigo-600 focus:ring-indigo-500">
```

- `rounded` — rounded checkbox corners.
- `border-gray-300` — subtle border.
- `text-indigo-600` — check mark color when checked.
- `focus:ring-indigo-500` — focus ring color for accessibility.

Requires `@tailwindcss/forms` plugin for full form styling support.

---

## 10. Card Layout Pattern

Consistent card structure used across the app:

```html
<div class="bg-white rounded-lg shadow-sm mb-6">
    <!-- Card header with border -->
    <div class="px-6 py-4 border-b border-gray-200">
        <h2 class="text-lg font-semibold text-gray-900">Title</h2>
    </div>
    <!-- Card body -->
    <div class="px-6 py-4">
        ...
    </div>
</div>
```

- `rounded-lg shadow-sm` — subtle card elevation.
- `border-b border-gray-200` — separator between header and body.
- Consistent `px-6 py-4` padding throughout.

---

## 11. HTMX Loading States

Tailwind classes for HTMX request indicators:

```css
.htmx-swapping {
    opacity: 0;
    transition: opacity 300ms ease-out;
}

.htmx-settling {
    opacity: 1;
    transition: opacity 300ms ease-in;
}

.htmx-request .htmx-indicator {
    display: inline-block;
}

.htmx-indicator {
    display: none;
}
```

HTMX automatically adds/removes these classes during requests, giving you smooth fade transitions between old and new content without any JavaScript.
