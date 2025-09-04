# Discord Notifications Troubleshooting

## Quick Fix Checklist

### 1. ✅ Set the Discord Webhook Secret
You've already done this, but verify:
- Go to **Settings** → **Secrets and variables** → **Actions** → **Secrets**
- Confirm `DISCORD_WEBHOOK` exists

### 2. ❌ Enable Discord Notifications Variable (MISSING!)
**This is what you need to do:**
1. Go to **Settings** → **Secrets and variables** → **Actions**
2. Click on **Variables** tab (not Secrets!)
3. Click **New repository variable**
4. Add:
   - Name: `ENABLE_DISCORD_NOTIFICATIONS`
   - Value: `true`

### 3. ✅ Use Only the Discord Workflow
I've disabled the basic workflow to prevent duplicate builds. Only `pr-apk-build-discord.yml` is now active.

## Testing Your Setup

After adding the variable, create a test PR to verify Discord notifications:
1. The build should start and post a "Build Started" message
2. When complete, you'll get either a success or failure notification

## What Triggers Discord Notifications

The Discord workflow sends notifications at these points:
1. **Build Start** - When the PR build begins
2. **Build Success** - When APK is built successfully
3. **Build Failure** - If the build fails
4. **Comment Posted** - When PR comment is posted (via comment workflow)

## Still Not Working?

Check these:
1. Webhook URL is valid and active in Discord
2. The repository variable name is exactly `ENABLE_DISCORD_NOTIFICATIONS`
3. The value is exactly `true` (lowercase, no quotes)
4. Check the Actions tab for any workflow errors

## Discord Webhook Format
Your webhook should look like:
```
https://discord.com/api/webhooks/[webhook-id]/[webhook-token]
```