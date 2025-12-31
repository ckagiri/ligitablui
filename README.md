# Ligitabl Prediction League - Stubbed Version 🎯⚽

A **working demo** of the football prediction league with **in-memory stubbed data** - no database required!

## Quick Start (30 seconds!)

```bash
# 1. Run
mvn spring-boot:run

# 2. Open browser
# Visit: http://localhost:8080
```

## Tech Stack

| Technology | Why Used |
|-----------|----------|
| Java 21 | Modern Java with records |
| Spring Boot 3.2 | Web framework |
| Thymeleaf | Server-side templates |
| HTMX 1.9 | Dynamic interactions |
| Tailwind CSS | Styling (CDN) |
| Lombok | Reduce boilerplate |

**What's NOT included**: PostgreSQL, jOOQ, JPA, Docker, JWT, Spring Security

## Key Concepts (Definitions + Why)

These terms show up a lot in this codebase. If they’re new, this is the mental model.

### Thymeleaf

**Definition:** A server-side templating engine. Spring MVC renders HTML by combining a template (in `src/main/resources/templates/`) with a `Model` from a controller.

**Why we chose it here:**

- Keeps the app **HTML-first** (simple to reason about for CRUD-ish pages)
- Tight integration with Spring MVC
- Great fit when the server already “knows” what to render

**When to consider alternatives:**

- You need a full client SPA with complex routing/state → React/Vue/Svelte
- You want a different server-side approach → Mustache, FreeMarker, JSP (less common in modern Spring)

### HTMX

**Definition:** A small library that lets HTML elements make HTTP requests (GET/POST/PUT/DELETE) using attributes like `hx-get` and then **swap** the server response (often HTML) into the page.

**Why we chose it here:**

- Delivers “AJAX-like” UX while staying **server-rendered**
- Avoids building/maintaining a full JSON API just to update parts of the page
- Works nicely with Thymeleaf fragments (`template :: fragment`)

**When to consider alternatives:**

- Heavy client-side interactivity and offline-like UX → SPA frameworks
- You want a typed contract-first frontend/backend boundary → JSON APIs + fetch/React Query/etc.
- You need real-time updates at scale → WebSockets/SSE (HTMX can integrate, but it’s not its main focus)

### Alpine.js

**Definition:** A small “JavaScript behavior” layer for HTML. It adds local state (`x-data`), event handlers (`@click`), conditional rendering (`x-show`), loops (`x-for`), etc.

**Why we chose it here:**

- Great for **sprinkling** interactivity onto server-rendered pages
- Less code/overhead than a full SPA framework
- Fits well with HTMX: HTMX swaps HTML; Alpine makes that HTML interactive

**When to consider alternatives:**

- Large client-side state management needs → React/Vue/Svelte
- Component library + complex UI reuse across many views → SPA frameworks or Web Components

### How They Work Together

- Thymeleaf renders HTML (full pages and fragments)
- HTMX swaps fragments into the page for partial updates
- Alpine provides client-side interactivity inside those rendered fragments

## How HTMX Works Here (Spring MVC + Thymeleaf)

This project uses **HTMX** to do “AJAX-like” partial page updates **without writing custom fetch/XHR code**.

### The Core Idea

- The browser sends a normal HTTP request, but HTMX adds headers (notably `HX-Request: true`).
- The Spring controller detects that header.
- For **normal requests**, the controller returns a full Thymeleaf page.
- For **HTMX requests**, the controller returns a **Thymeleaf fragment** only.

That lets one URL serve both:
- Full page load
- Partial updates (swap only a section of the DOM)

### Controller Pattern (full page vs fragment)

In controllers, you’ll commonly see logic like:

- Read `HX-Request` header
- If present: return `template :: fragmentName`
- Else: return `template`

Example (pattern):

```java
@GetMapping("/some/page")
public String page(
		@RequestHeader(value = "HX-Request", required = false) String hxRequest,
		Model model
) {
	// populate model...

	if (hxRequest != null && !hxRequest.isBlank()) {
		return "some/template :: fragment";
	}

	return "some/template";
}
```

### Template Pattern (hx-get + hx-target + hx-swap)

On the HTML side, HTMX attributes declare the request and where the response goes:

- `hx-get="/url"` (or `hx-post`, `hx-put`)
- `hx-target="#someId"` (the element to replace)
- `hx-swap="innerHTML|outerHTML"` (how replacement happens)

Common rules of thumb:

- Use `hx-swap="outerHTML"` when the server returns the *entire wrapper element* you want to replace.
- Use `hx-swap="innerHTML"` when the server returns only the *contents* of the target.

### “AJAX” Without JSON

HTMX does not require JSON APIs. In this repo it mostly swaps **HTML fragments** rendered server-side.
That means:

- Debugging is often as simple as “inspect the HTML response”.
- A `500` frequently means a **Thymeleaf exception** (template parse, missing model field, etc.).

## Where Alpine.js Fits

HTMX and Alpine solve different problems:

- **HTMX**: “server-driven UI updates” (fetch HTML fragments, swap them into the page)
- **Alpine.js**: “client-side UI behavior” inside a rendered page/fragment (state, click handlers, conditional UI, small interactions)

In this project, Alpine is used for interactive behavior on the predictions UI (e.g., managing local state for swapping/reordering predictions) while Spring + Thymeleaf remain the source of truth for what HTML is rendered.

### Key Rule: Don’t Mix Thymeleaf Inline JS Into `x-data`

Avoid putting Thymeleaf inline expressions like `[[${...}]]` directly inside Alpine attributes such as `x-data="..."`.
It’s easy to generate invalid JavaScript/HTML and cause runtime errors.

Preferred pattern:

- Render server data as **JSON** into `data-*` attributes (or a `<script type="application/json">` block)
- Initialize Alpine with a function/factory that reads from the DOM

This keeps:

- Thymeleaf responsible for HTML + data serialization
- Alpine responsible for behavior using valid JS

### HTMX + Alpine Together (Re-initialization After Swaps)

When HTMX swaps in new HTML, any Alpine behavior in that new subtree must be initialized.
Typical approach:

- Listen for `htmx:afterSwap`
- Initialize Alpine on the swapped element (or let Alpine “discover” it if you’re using Alpine’s init hooks)

Symptom if you forget this:

- The new HTML appears, but buttons/handlers don’t work, or Alpine state is missing.

### Example: Thymeleaf → JSON → Alpine (safe data handoff)

**Controller** renders data into the model (often as JSON):

```java
@GetMapping("/predictions/me")
public String myPredictions(Model model) throws Exception {
	var predictions = dataService.getMyPrediction();

	model.addAttribute("predictions", predictions);
	model.addAttribute("predictionsJson", objectMapper.writeValueAsString(predictions));
	model.addAttribute("canSwap", true);

	return "predictions/me";
}
```

**Template** puts JSON into `data-*` attributes and initializes Alpine via a factory:

```html
<section
	id="prediction-page"
	x-data="Ligitabl.predictionPage($el)"
	th:attr="
		data-predictions=${predictionsJson},
		data-can-swap=${canSwap}
	"
>
	<!-- normal Thymeleaf-rendered HTML here -->
</section>
```

**JavaScript** reads from `dataset` and returns Alpine state/actions:

```js
window.Ligitabl = window.Ligitabl || {};

window.Ligitabl.predictionPage = ($el) => {
	const predictions = JSON.parse($el.dataset.predictions || '[]');
	const canSwap = ($el.dataset.canSwap === 'true');

	return {
		predictions,
		canSwap,

		swapTeams(a, b) {
			if (!this.canSwap) return;
			// update local state only; server persistence can be HTMX/POST
		},
	};
};
```

This approach avoids fragile template/JS mixing and keeps Alpine expressions valid.

## Spring + Thymeleaf: How HTML Rendering Works

At a high level, Spring Boot is serving HTML like this:

1. **Client** requests a URL (normal navigation or HTMX request).
2. Embedded **Tomcat** receives the request.
3. Spring MVC’s **DispatcherServlet** routes the request to the right controller method.
4. The controller:
	 - fetches data (here: in-memory service stubs)
	 - puts values into the **Model**
	 - returns a view name (e.g., `"predictions/me"` or `"predictions/me :: predictionPage"`)
5. A **ViewResolver** picks Thymeleaf to render the view.
6. **Thymeleaf** loads templates from `src/main/resources/templates/`:
	 - full pages for normal navigation
	 - fragments for HTMX partial swaps (`template :: fragmentName`)
7. The rendered HTML is returned to the browser.

Why this matters for debugging:

- A `500` during an HTMX request is usually the same as a `500` on a full page load — check server logs for the Thymeleaf/Spring exception.
- `template :: fragment` rendering can fail if the fragment expects model attributes that only the full-page path sets.

## Debugging & Troubleshooting HTMX

### 1) Use the Browser Network Tab

In Chrome/Edge/Firefox DevTools:

- Open **Network** → click the HTMX request
- Confirm:
	- **Status** is `200`
	- **Response** is the fragment HTML you expect
	- **Request Headers** include `HX-Request: true`

If you see `200` but the UI doesn’t change:

- Check `hx-target` exists and matches exactly.
- Check `hx-swap` matches what the server returned (wrapper vs inner markup).

### 2) Watch the Server Logs (Template Errors)

Most “mysterious HTMX failures” are just normal server errors:

- `TemplateInputException` → Thymeleaf couldn’t parse/render a template
- `SpelEvaluationException` → template referenced a field that doesn’t exist on the model/DTO

If a request returns `500`, scroll the terminal output to the **root cause** exception.

### 3) Common Symptoms & Fixes

- **Navbar/layout duplicated after clicking a button**
	- Cause: HTMX swapped a *full page* response into a small target.
	- Fix: return a fragment when `HX-Request` is present, and/or swap the correct wrapper element.

- **HTMX request returns HTML that includes `<html>` / `<body>` tags**
	- Cause: returning a full template (or a fragment file that contains full-document markup).
	- Fix: return `template :: fragment` and keep fragments as fragment-only markup.

- **Alpine/JS breaks after an HTMX swap**
	- Cause: swapped-in HTML isn’t initialized.
	- Fix: re-initialize on `htmx:afterSwap` (this repo does this for Alpine components).

- **Template renders on full load, but HTMX swap fails**
	- Cause: fragment path/name mismatch, or the fragment expects different model attributes.
	- Fix: verify fragment name, and compare model attributes set for full vs HTMX responses.

### 4) Quick CLI Checks

Use curl to see what the server is returning:

```bash
# Full page
curl -i http://localhost:8080/predictions/me

# HTMX-style request (should return fragment)
curl -i -H "HX-Request: true" http://localhost:8080/predictions/me
```

### 5) Enable Extra HTMX Logging (Optional)

You can temporarily add in the browser console:

```js
document.body.addEventListener('htmx:responseError', (e) => console.log('HTMX error', e));
document.body.addEventListener('htmx:afterSwap', (e) => console.log('HTMX swapped', e.target));
```


## Common Issues

### Port 8080 in use?
```bash
# Change in application.properties
server.port=8081
```

### Maven build fails?
```bash
# Clean and rebuild
mvn clean install
```

### Swap doesn't work?
- Check browser console for errors
- Make sure JavaScript is enabled
- Try hard refresh (Ctrl+Shift+R)

## API Endpoints

All endpoints work with stub data:

```
GET  /                                  # Home page
GET  /leaderboard?phase=Q2              # Leaderboard
GET  /seasons/{id}/standings            # Standings table
GET  /seasons/{id}/matches              # Matches list
GET  /predictions/me                    # My predictions
POST /predictions/swap                  # Make swap (HTMX)
GET  /predictions/me/swap-status        # Check cooldown
GET  /users/{id}/predictions            # Other user predictions
```

## Learning Resources

- **HTMX**: https://htmx.org/docs/
- **Thymeleaf**: https://www.thymeleaf.org/doc/tutorials/3.1/usingthymeleaf.html
- **Spring Boot**: https://spring.io/guides
