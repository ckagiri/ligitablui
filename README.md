# Ligitabl Prediction League - Stubbed Version 🎯⚽

A **working demo** of the football prediction league with **in-memory stubbed data** - no database required!

## What's This?

This is a simplified version of the full project that lets you see all the HTMX interactions working **immediately** without setting up PostgreSQL, jOOQ, or any database. Perfect for:

- 🚀 **Quick Demo**: See HTMX in action in 30 seconds
- 🎨 **Frontend Development**: Work on UI/UX without backend complexity
- 📚 **Learning**: Understand HTMX patterns with working examples
- 🧪 **Prototyping**: Test ideas quickly

## Key Features Working

✅ **HTMX Swap Animation** - Smooth 400ms cubic-bezier transitions  
✅ **Phase Switcher** - Dynamic leaderboard updates  
✅ **Swap Cooldown** - 24-hour timer that actually works  
✅ **Live Leaderboard** - 10 players with realistic data  
✅ **Standings Table** - 20 Premier League teams  
✅ **Matches View** - Finished and upcoming matches  
✅ **My Predictions** - Fully functional team swapping  

## Quick Start (30 seconds!)

```bash
# 1. Extract
tar -xzf ligitabl-stub.tar.gz
cd ligitabl-stub

# 2. Run
mvn spring-boot:run

# 3. Open browser
# Visit: http://localhost:8080
```

That's it! No Docker, no database, no setup. Just run and go! 🎉

## What You Can Do

### 1. View Leaderboard
- Go to http://localhost:8080/leaderboard
- **Switch phases** using dropdown (FS, Q1-Q4, H1-H2)
- Watch the smooth HTMX phase transitions

### 2. Make Predictions & Swaps
- Go to http://localhost:8080/predictions/me
- **Select two teams** from dropdowns
- **Click "Confirm Swap"** - watch the smooth animation!
- Try swapping again immediately - you'll see the cooldown timer

### 3. View Standings
- Go to http://localhost:8080/seasons/current/standings
- See full Premier League table with stats

### 4. View Matches
- Go to http://localhost:8080/seasons/current/matches
- See finished scores and upcoming fixtures

## How Swapping Works

The swap functionality demonstrates the core game mechanic:

1. **First swap**: Works immediately
2. **Second swap**: Shows cooldown - "Next swap available in 24h"
3. **Wait simulation**: In the stub, last swap is set to 25 hours ago, so you can swap immediately
4. **After swap**: Cooldown activates for real (24 hours)

To test cooldown:
```java
// In InMemoryDataService.java, change this line:
private Instant lastSwapTime = Instant.now().minus(25, ChronoUnit.HOURS); // Can swap now
// to:
private Instant lastSwapTime = Instant.now().minus(1, ChronoUnit.HOURS);  // Must wait 23h
```

## HTMX Patterns Demonstrated

### 1. Partial Page Updates
```html
<!-- Phase switcher updates only table content -->
<select hx-get="/leaderboard"
        hx-trigger="change"
        hx-target="#leaderboard-content"
        hx-swap="innerHTML swap:300ms">
```

### 2. Form Submission with Animation
```html
<!-- Swap form updates prediction table with smooth animation -->
<form hx-post="/predictions/swap"
      hx-target="#prediction-table"
      hx-swap="outerHTML swap:400ms">
```

### 3. Progressive Enhancement
All forms work without JavaScript! HTMX enhances existing HTML.

## Project Structure

```
ligitabl-stub/
├── pom.xml                          # Simplified - no jOOQ, no DB
├── src/main/
│   ├── java/com/ligitabl/
│   │   ├── PredictionLeagueApplication.java
│   │   ├── controller/              # Controllers with stubbed responses
│   │   │   ├── PublicController.java
│   │   │   └── PlayerController.java
│   │   ├── service/
│   │   │   └── InMemoryDataService.java  # All stub data here!
│   │   ├── domain/
│   │   │   └── Team.java
│   │   └── dto/
│   │       └── Responses.java       # Response DTOs
│   └── resources/
│       ├── application.properties   # Minimal config
│       ├── templates/               # Thymeleaf + HTMX
│       └── static/css/
│           └── main.css             # All animations
```

## Stub Data

### InMemoryDataService.java

This class contains all fake data:

- **20 Premier League teams** with codes and names
- **10 leaderboard entries** with realistic scores
- **20 standings rows** with match statistics
- **5 matches** (3 finished, 2 upcoming)
- **Your prediction** (swappable in-memory)
- **Swap cooldown timer** (actually works!)

### Customizing Data

Want to change team names or scores? Edit `InMemoryDataService.java`:

```java
// Add your own teams
private List<Team> initializeTeams() {
    return List.of(
        new Team("ABC", "Your Team", "/images/crest.png"),
        // ... more teams
    );
}

// Change leaderboard
public List<LeaderboardEntry> getLeaderboard(String phase) {
    return List.of(
        new LeaderboardEntry(1, "Your Name", 2000, 50, 10, 200, 5),
        // ... more entries
    );
}
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

## Next Steps

### 1. Explore the Code

Start with these files to understand the architecture:

1. `InMemoryDataService.java` - All data
2. `PlayerController.java` - Swap logic
3. `predictions/me.html` - HTMX swap form
4. `fragments/prediction-table.html` - Reusable fragment

### 2. Modify & Experiment

Try these exercises:

- **Add a new team**: Edit `initializeTeams()`
- **Change swap cooldown**: Edit `getSwapStatus()` to use 1 hour instead of 24
- **Add new phase**: Add "M1" (Month 1) to phase selector
- **Style changes**: Edit `main.css` for different animations

### 3. Upgrade to Full Version

Ready for the real deal?

1. Get the full version with PostgreSQL and jOOQ
2. Migrate your UI changes
3. Implement real repositories
4. Add authentication
5. Connect to Football-Data.org API

## Differences from Full Version

| Feature | Stub Version | Full Version |
|---------|-------------|--------------|
| Database | In-memory | PostgreSQL |
| Data Access | Direct objects | jOOQ (type-safe SQL) |
| Authentication | None | JWT + Spring Security |
| Team Data | Hardcoded | Database |
| Swap Persistence | Session only | Database |
| Setup Time | 30 seconds | 5 minutes |

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
- **Design Doc**: [Original Specification](https://gist.github.com/ckagiri/fb94ff87d6f2590e3e891d608317f7f7)

## Contributing

Found a bug or want to improve something?

1. Fork the repo
2. Make your changes
3. Test locally
4. Submit a PR

## Support

Questions or issues?

- Check the code comments
- Review the main README (full version)
- Open an issue on GitHub

---

**Built for rapid prototyping and learning HTMX! 🚀**

Enjoy playing with the stubbed version, and when you're ready, upgrade to the full version with real persistence and authentication!
