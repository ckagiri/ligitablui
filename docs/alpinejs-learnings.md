# Alpine.js Learnings

Practical lessons learned while building and refactoring Alpine.js components in LigiPredictor.

---

## 1. Component Composition with `Object.assign`

When multiple Alpine components share logic (e.g., guest prediction and auth prediction), extract a shared base factory and compose:

```js
// Shared base
window.Ligitabl._predictionBase = function(parsed) {
    return {
        teams: [],
        selectedTeam: null,
        // ... shared methods
        isDirty(teamCode) { ... },
        _performSwap(teamCode, usePreSwapAnimation) { ... },
        _selectTeam(teamCode) { ... },
        reset() { ... },
    };
};

// Auth component — extends base
window.Ligitabl.predictionPage = function(el) {
    const base = Ligitabl._predictionBase(parsed);
    return Object.assign(base, {
        canSwap,
        // auth-specific methods override or extend base
        teamClick(teamCode) { ... },
        submitChanges() { ... },
    });
};

// Guest component — extends base with different behavior
window.Ligitabl.guestPredictionPage = function(el) {
    const base = Ligitabl._predictionBase(parsed);
    return Object.assign(base, {
        alwaysHoverable: true,      // override base default
        teamClick(teamCode) { ... },
        reset() {                   // extend base reset
            this.teams = JSON.parse(JSON.stringify(this.originalTeams));
            this.selectedTeam = null;
            this.clearLocalStorage();  // guest-specific addition
        },
    });
};
```

**Key points:**
- `Object.assign` merges left-to-right, so the second argument's properties override the base.
- Methods in the merged object share the same `this` context, so base methods can reference properties added by the extending component.
- Prefix internal/shared methods with `_` (e.g., `_performSwap`, `_selectTeam`) to distinguish them from public API methods.

---

## 2. Initializing Components from Data Attributes

Pass server data via HTML `data-*` attributes and read them in the component factory:

```html
<div x-data="Ligitabl.guestPredictionPage($el)"
     data-predictions='[{"position":1,"teamCode":"MCI",...}]'
     data-current-standings='{"MCI":1,"ARS":2}'>
```

```js
window.Ligitabl.guestPredictionPage = function(el) {
    const parsed = Ligitabl._parseDataAttributes(el);
    // parsed.predictions, parsed.currentStandings, etc.
};
```

**`$el` is critical** — it gives the factory function a reference to the DOM element so it can read `dataset` properties. Without it, the component has no way to receive server data.

---

## 3. `x-data` Evaluation and Script Load Order

Alpine.js with `defer` auto-initializes on `DOMContentLoaded`. It walks the DOM and evaluates every `x-data` attribute as a JavaScript expression. If `x-data="Ligitabl.guestPredictionPage($el)"` is encountered before `ligitabl.js` has loaded, you get a `ReferenceError` or `SyntaxError`.

**Rule:** When using external JS files that define Alpine component factories, the `<script>` for those files must appear **before** the Alpine `<script>` tag, and both should use `defer`:

```html
<!-- ligitabl.js defines Ligitabl.* — must load first -->
<script defer src="/js/ligitabl.js"></script>

<!-- Alpine evaluates x-data attributes — must load second -->
<script defer src="https://cdn.jsdelivr.net/npm/alpinejs@3.x.x/dist/cdn.min.js"></script>
```

`defer` scripts execute in document order, so this guarantees `Ligitabl` exists before Alpine initializes.

---

## 4. Re-initializing Alpine After HTMX Swaps

When HTMX swaps in new HTML that contains `x-data`, Alpine doesn't automatically initialize it. You need to manually tell Alpine to walk the new DOM:

```js
document.body.addEventListener('htmx:afterSwap', (event) => {
    if (!window.Alpine || !event.detail?.target) return;
    const target = event.detail.target;
    if (!target.querySelector || !target.querySelector('[x-data]')) return;
    window.Alpine.initTree(target);
});
```

Without this, any HTMX-loaded content with Alpine components will be inert.

---

## 5. DOM Manipulation Alongside Alpine Reactivity

Alpine manages reactivity through its data model, but sometimes you need direct DOM manipulation for animations. The pattern used in this project:

```js
_performSwap(teamCode, usePreSwapAnimation) {
    // Get DOM references for animation
    const row1 = document.querySelector(`[data-team-code='${team1Code}']`);
    const row2 = document.querySelector(`[data-team-code='${team2Code}']`);

    // Add CSS class for visual effect
    if (row1) row1.classList.add('swapping');
    if (row2) row2.classList.add('swapping');

    // Clean up after animation
    setTimeout(() => {
        if (row1) row1.classList.remove('swapping');
        if (row2) row2.classList.remove('swapping');
    }, 600);

    // Update Alpine's reactive data (causes re-render)
    const temp = this.teams[index1];
    this.teams[index1] = this.teams[index2];
    this.teams[index2] = temp;
    this.teams.forEach((team, idx) => (team.position = idx + 1));
}
```

**Key insight:** `querySelector` returns the DOM element at the time of the call. After Alpine re-renders (due to the array swap), those references may point to moved elements. Place DOM manipulation (adding CSS classes) *before* the data mutation, or use `$nextTick` for post-render DOM work:

```js
this.$nextTick(() => {
    const row = document.querySelector(`[data-team-code='${teamCode}']`);
    if (row) {
        row.classList.add('selected-pulse');
        setTimeout(() => row.classList.remove('selected-pulse'), 300);
    }
});
```

---

## 6. `x-for` and `:key` for List Rendering

```html
<template x-for="team in teams" :key="team.code">
    <tr :data-team-code="team.code">
        <td x-text="team.name"></td>
    </tr>
</template>
```

- `:key` must be a unique, stable identifier. Using `team.code` (not array index) ensures Alpine correctly tracks elements across reorders.
- `x-for` must be on a `<template>` tag with a single direct child element.

---

## 7. Mixing Thymeleaf and Alpine — The Boundary

Thymeleaf runs server-side; Alpine runs client-side. They can coexist but shouldn't overlap:

| What you want | Use |
|---|---|
| Conditional rendering at page load (SEO, server logic) | `th:if`, `th:unless` |
| Conditional rendering based on user interaction | `x-show`, `x-if` |
| Server data in HTML | `th:attr`, `th:text`, data attributes |
| Dynamic values in Alpine expressions | Alpine component properties (not `[[${...}]]`) |

**The mistake to avoid:** Using Thymeleaf inline expressions `[[${...}]]` inside Alpine attributes like `:class`. Thymeleaf does not process these inside HTML attributes — they'll be output as literal text and break Alpine's JavaScript evaluation.

---

## 8. `x-show` vs `x-if`

- `x-show` toggles `display: none`. The element stays in the DOM. Use for frequent toggling (show/hide columns, tabs).
- `x-if` removes/adds the element from the DOM entirely. Must be on a `<template>` tag. Use for sections that are rarely shown.

In this project, `x-show` is used for the comparison columns (standings, fixtures, points) because users toggle them frequently:

```html
<th x-show="showStandings" class="...">Actual</th>
<td x-show="showStandings" x-text="getActualPosition(team.code)"></td>
```

---

## 9. Alpine + localStorage Pattern

For guest users whose data persists in the browser:

```js
init() {
    const saved = loadSavedPrediction();
    if (saved) {
        this.teams = saved.map((t, idx) => ({ ...t, position: idx + 1 }));
        this.hasLocalChanges = true;
    } else {
        this.teams = Ligitabl._mapServerPredictions(serverPredictions);
    }
    // originalTeams always reflects server state (for dirty checking)
    this.originalTeams = Ligitabl._mapServerPredictions(serverPredictions);
},

teamClick(teamCode) {
    // ... perform swap ...
    this.saveToLocalStorage();  // persist after every change
},

reset() {
    this.teams = JSON.parse(JSON.stringify(this.originalTeams));
    this.selectedTeam = null;
    this.clearLocalStorage();
}
```

**Pattern:** `originalTeams` always comes from the server (baseline for dirty checking). `teams` is the working copy that may come from localStorage on load. Every mutation saves to localStorage. Reset clears both the working copy and localStorage.
