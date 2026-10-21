# Putting Mini HRMS online (free)

```
 Vercel (frontend)  ──calls──►  Render (backend, Docker)  ──►  Neon (PostgreSQL)
 mini-hrms.vercel.app           mini-hrms-api.onrender.com
                 ▲                        ▲
                 └──── both build from your GitHub repo ────┘
```

Do the steps in this order: **GitHub → Neon → Render → Vercel → back to Render.**
Keep a notepad open: you'll copy a few values between sites.

---

## Step 1: Put the code on GitHub

1. Create an account at https://github.com if you don't have one.
2. Click **+** (top right) → **New repository**.
   - Name: `mini-hrms`
   - Public
   - **Don't** tick "Add a README" (we already have one)
   - Click **Create repository**.
3. Upload the code. Pick **one** way:

   **Easy way (browser):** on the new repo page, click **"uploading an existing file"**. Open the unzipped `mini-hrms` folder, select **everything inside it** (`backend`, `frontend`, `README.md`, and so on, not the `mini-hrms` folder itself), and drag it into the browser. Click **Commit changes**.

   **Proper way (Git, recommended for your resume):** install Git from https://git-scm.com. Open the `mini-hrms` folder in VS Code, open **Terminal → New Terminal**, and run (replace `YOUR-USERNAME`):
   ```
   git init
   git add .
   git commit -m "Mini HRMS: Spring Boot + React + PostgreSQL"
   git branch -M main
   git remote add origin https://github.com/YOUR-USERNAME/mini-hrms.git
   git push -u origin main
   ```
   A browser window may ask you to sign in to GitHub. Allow it.

4. Refresh the repo page. You should see the `backend` and `frontend` folders at the top level.

---

## Step 2: Create the database on Neon

1. Sign up at https://neon.tech (you can sign in with GitHub).
2. Create a project: name it `mini-hrms`, Postgres version 16, and pick the region closest to India (Singapore, if offered).
3. On the dashboard, click **Connect**. You'll see a connection string like:
   ```
   postgresql://neondb_owner:AbC123xyz@ep-cool-name-123456.ap-southeast-1.aws.neon.tech/neondb?sslmode=require
   ```
   It's made of these parts:
   ```
   postgresql://  USER  :  PASSWORD  @  HOST  /  DATABASE  ?sslmode=require
   ```
4. Write these three values in your notepad:
   ```
   DB_URL      = jdbc:postgresql://HOST/DATABASE?sslmode=require
   DB_USER     = USER
   DB_PASSWORD = PASSWORD
   ```
   Example:
   `DB_URL = jdbc:postgresql://ep-cool-name-123456.ap-southeast-1.aws.neon.tech/neondb?sslmode=require`

   Note that `DB_URL` starts with **`jdbc:postgresql://`** and has **no user or password** inside it.

---

## Step 3: Deploy the backend on Render

1. Sign up at https://render.com with your GitHub account.
2. Click **New +** → **Web Service** → connect your GitHub, then pick the `mini-hrms` repo.
3. Fill in:
   - **Name:** `mini-hrms-api`
   - **Region:** Singapore
   - **Branch:** `main`
   - **Root Directory:** `backend`  ← important
   - **Language / Runtime:** `Docker`
   - **Instance Type:** `Free`
4. Under **Environment Variables**, add:

   | Key | Value |
   |---|---|
   | `DB_URL` | from your notepad |
   | `DB_USER` | from your notepad |
   | `DB_PASSWORD` | from your notepad |
   | `JWT_SECRET` | click **Generate** (or type any random text of 40+ characters) |
   | `SEED_DEMO_DATA` | `true` |
   | `CORS_ALLOWED_ORIGINS` | `http://localhost:5173` (we'll change this in step 5) |

5. Under **Advanced**, set **Health Check Path** to `/api/health`.
6. Click **Create Web Service**. Render now builds your Docker image, which takes 5–10 minutes. Watch the logs.
   - **Success** looks like a log line containing `Started MiniHrmsApplication`, and the status turns **Live**.
   - **If it fails,** copy the red error lines and send them to Claude.
7. Copy your backend address from the top of the page, e.g. `https://mini-hrms-api.onrender.com`.
   Test it: open `https://mini-hrms-api.onrender.com/api/health` in your browser. You should see `{"status":"UP",...}`.

---

## Step 4: Deploy the frontend on Vercel

1. Sign up at https://vercel.com with your GitHub account.
2. **Add New… → Project** → import the `mini-hrms` repo.
3. Fill in:
   - **Root Directory:** click **Edit** → choose `frontend`  ← important
   - **Framework Preset:** Vite (usually detected automatically)
   - **Environment Variables:**
     - Key: `VITE_API_URL`
     - Value: your Render address, e.g. `https://mini-hrms-api.onrender.com` (no `/` at the end)
4. Click **Deploy** (about 1 minute).
5. Copy your site address, e.g. `https://mini-hrms-girindra.vercel.app`.

---

## Step 5: Allow the frontend to call the backend (CORS)

1. Go back to Render → your service → **Environment**.
2. Change `CORS_ALLOWED_ORIGINS` to your Vercel address, with no `/` at the end:
   ```
   https://mini-hrms-girindra.vercel.app
   ```
3. Click **Save Changes**. Render redeploys automatically (a few minutes).

---

## Step 6: Test it

Open your Vercel address and log in with `admin@demo.com` / `password123`.

| Problem | Cause and fix |
|---|---|
| "Cannot reach the server" on the first try | The free Render server was asleep. Wait 30–60 seconds and try again. |
| Login does nothing; the browser console says "CORS" | `CORS_ALLOWED_ORIGINS` doesn't exactly match your Vercel address. Check `https://`, spelling, and no trailing `/`. |
| Render log: "Connection refused" or "password authentication failed" | Recheck `DB_URL`, `DB_USER` and `DB_PASSWORD` against Neon. |
| Login works, but pages show 404 errors | `VITE_API_URL` is wrong on Vercel. Fix it, then **Redeploy** (Vercel only reads it at build time). |

---

## After it works

- **Put both links on your resume:** the live site and the GitHub repo.
- **Free-tier note:** Render sleeps after about 15 minutes of no traffic. Open the site once a minute or two before you share it or start an interview.
- **Auto-deploy:** every `git push` to `main` rebuilds both sites automatically.
- **Reset the demo data:** delete the Neon database and create a new one, or point `DB_URL` at a fresh one. The app recreates the tables and demo users on startup.
- **Interview line:** "The backend runs as a Docker container on Render, the database is managed PostgreSQL on Neon, and the frontend is a static build on Vercel. Secrets live in environment variables, never in Git, and CORS is restricted to my frontend's domain."
