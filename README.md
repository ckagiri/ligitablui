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
