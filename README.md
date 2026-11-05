# Basketball Advanced Analytics

A fantasy basketball app: search NBA players, build your team, and (coming soon)
see XGBoost next-game predictions and ask a RAG chatbot for add/drop advice.

## Stack

- **frontend/**: Next.js + Tailwind. Talks directly to Supabase (no backend server).
- **pipeline/**: Python scripts that pull data from stats.nba.com and load it into Supabase.
- **supabase/schema.sql**: the whole database: tables, a season-averages view, and security rules.
- **Supabase**: Postgres database + email/password login.

```
pipeline/seed.py ──► Supabase (Postgres + Auth) ◄── frontend (Next.js)
  (nba_api)
```

## Run it locally

**1. Create the database (one time)**
1. Create a free project at [supabase.com](https://supabase.com).
2. Open the **SQL Editor**, paste in `supabase/schema.sql`, and click **Run**.
3. Optional for dev: **Authentication → Sign In / Providers → Email**: turn off **Confirm email** so sign-up logs you in immediately.

**2. Load NBA data**
```bash
cp .env.example .env               # paste your Session pooler connection string
python3 -m venv .venv && source .venv/bin/activate
pip install -r pipeline/requirements.txt
python pipeline/seed.py            # ~1 minute; use --seasons 1 for current season only
```

**3. Start the app**
```bash
cd frontend
cp .env.example .env.local         # paste your project URL + publishable key
npm install
npm run dev                        # http://localhost:3000
```

## History

The original microservices version (Spring Boot, gRPC, Eureka, Redis, Docker) is preserved
at the `v1-microservices` tag.

## Authors
**Rushil Shah**: [LinkedIn](https://linkedin.com/in/rushilshahh) · [Portfolio](https://rushilshah11.github.io/portfolio/)
**Ashwin Sanjaye**: [LinkedIn](https://www.linkedin.com/in/ashwin-sanjaye/) · [Portfolio](https://ashwinsanjaye.com/)
