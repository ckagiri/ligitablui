# Thymeleaf Learnings

Practical lessons learned while building and refactoring the LigiPredictor templates.

---

## 1. Parameterized Fragments

Fragments can accept parameters, making them reusable across pages.

```html
<!-- Definition -->
<div th:fragment="round-nav(baseUrl, viewingRound, maxRound, htmxTarget, htmxSwap, showJumpToCurrent, dismissBanner)">
    ...
</div>

<!-- Usage -->
<div th:replace="~{fragments/round-navigation :: round-nav(
    '/seasons/current/standings', ${currentRound}, ${latestRound},
    'body', 'outerHTML', false, false)}">
</div>
```

**Key points:**
- Parameters are positional and must match the fragment signature exactly.
- You can pass Thymeleaf expressions (`${currentRound}`), string literals (`'body'`), and boolean literals (`false`).
- Fragment parameters are resolved at render time and can be used with any `th:*` attribute inside the fragment.

---

## 2. `th:replace` vs `th:insert`

- `th:replace` — Replaces the host tag entirely with the fragment content.
- `th:insert` — Inserts the fragment content *inside* the host tag (host tag is preserved).

In practice, `th:replace` is used more often because you want the fragment's root element to be the actual element in the DOM.

---

## 3. Conditional Attributes with `th:attrappend`

To conditionally add a data attribute (not just its value, but the attribute itself):

```html
<button th:attrappend="data-dismiss-results-banner=${dismissBanner ? 'true' : null}">
```

When the expression evaluates to `null`, Thymeleaf does **not** render the attribute at all. This is the correct way to conditionally include HTML attributes.

**Alternative approach using `th:attr`:**
```html
<div th:attr="data-always-hoverable=${alwaysHoverable}">
```
This always renders the attribute but sets its value from the expression.

---

## 4. Inline Expressions `[[${...}]]` — Where They Work and Don't

Thymeleaf's inline expression syntax `[[${variable}]]` is designed for use inside **text content** (between HTML tags) or inside `th:inline="javascript"` / `th:inline="text"` blocks.

**They DO NOT work inside HTML attributes** like `:class`, `x-data`, or any other attribute — even if it looks like it should. The expression will be output literally as `[[${variable}]]`.

```html
<!-- BROKEN: Thymeleaf does NOT process [[${...}]] inside attributes -->
<tr :class="{ 'hoverable': [[${alwaysHoverable}]] || canSwap }">
<!-- Rendered output: [[${alwaysHoverable}]] (literal text, causes JS error) -->

<!-- CORRECT: Use a data attribute + read it from JS/Alpine -->
<div th:attr="data-always-hoverable=${alwaysHoverable}">
```

**Even better:** If using Alpine.js, make the value a property of the Alpine component so the template references a plain JS variable — no Thymeleaf needed in the `:class` expression at all.

This was a real bug we hit: the `[[${alwaysHoverable}]]` inside a `:class` attribute was not resolved by Thymeleaf, and Alpine tried to parse it as JavaScript, causing `Uncaught SyntaxError: Unexpected token '{'`.

---

## 5. `th:if` and `th:unless` for Conditional Blocks

Wrap sections in `th:if` to conditionally render:

```html
<div th:if="${!isGuest && !isUserNotFound}">
    <!-- Only rendered for authenticated users -->
</div>
```

**Gotcha:** If you have a parent `th:if` condition and inner elements also have `th:if`, you can simplify the inner ones. For example:

```html
<!-- Outer wrapper already gates on !isCurrentRound -->
<div th:if="${!isCurrentRound}">
    <!-- No need to repeat !isCurrentRound here -->
    <tr th:if="${hasRoundResult}">...</tr>
</div>
```

---

## 6. Layout Dialect (`layout:decorate`, `layout:fragment`)

The Thymeleaf Layout Dialect allows a base template with named slots:

```html
<!-- layout/base.html -->
<div layout:fragment="content">
    <!-- Page content goes here -->
</div>

<!-- Child page (e.g., predictions.html) -->
<html layout:decorate="~{layout/base}">
<body>
    <div layout:fragment="content">
        <!-- This replaces the slot in base.html -->
    </div>
</body>
</html>
```

---

## 7. Static Resources and `th:src` / `th:href`

Use Thymeleaf's URL syntax for static resources to get context-path-aware URLs:

```html
<script th:src="@{/js/ligitabl.js}"></script>
<link rel="stylesheet" th:href="@{/dist/css/main.css}">
```

**Important:** If your `WebMvcConfigurer` has custom `addResourceHandlers`, you must explicitly register each directory. Spring Boot's default static resource serving (`classpath:/static/`) is overridden when you define custom handlers.

```java
// If you have this, you MUST register /js/** explicitly
registry.addResourceHandler("/css/**")
        .addResourceLocations("classpath:/static/css/");
registry.addResourceHandler("/js/**")        // Don't forget this!
        .addResourceLocations("classpath:/static/js/");
```

---

## 8. HTMX Fragment Rendering

When using HTMX with Thymeleaf, you can define named fragments that HTMX requests return:

```html
<!-- In predictions.html -->
<div id="prediction-page" th:fragment="predictionPage">
    ...
</div>
```

The controller can return just the fragment for HTMX requests (partial page updates) or the full page for regular requests. Use `hx-get`, `hx-target`, and `hx-swap` to wire it up:

```html
<button hx-get="/predictions/user/me?round=5"
        hx-target="#prediction-page"
        hx-swap="outerHTML swap:300ms show:window:top">
```

---

## 9. Thymeleaf + JSON Data Attributes

To pass server data to JavaScript components, serialize to JSON and put it in data attributes:

```html
<div th:attr="data-predictions=${predictionsJson},
              data-current-standings=${currentStandingsJson}">
```

Thymeleaf will HTML-encode the JSON (e.g., `"` becomes `&quot;`), and the browser automatically decodes it when you read `el.dataset.predictions`. No manual unescaping needed.
