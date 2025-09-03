# Quick Setup Guide for PR APK Builds

## Option 1: Basic Setup (Recommended)
Use the `pr-apk-build.yml` workflow for automatic APK builds on PRs.

**No additional setup required!** The workflow will:
- Build APK on every PR
- Post results as PR comments
- Upload APK as downloadable artifact

## Option 2: Setup with Discord Notifications
If you want Discord notifications, use `pr-apk-build-discord.yml` instead:

1. **Delete or disable** `pr-apk-build.yml` (rename to `.yml.disabled`)
2. **Keep** `pr-apk-build-discord.yml` active
3. **Add Discord Webhook**:
   - Go to Settings → Secrets and variables → Actions
   - Add secret: `DISCORD_WEBHOOK` with your Discord webhook URL
4. **Enable Discord Notifications**:
   - Go to Settings → Secrets and variables → Actions → Variables
   - Add variable: `ENABLE_DISCORD_NOTIFICATIONS` = `true`

## Choosing a Workflow
- Use **only one** workflow at a time to avoid duplicate builds
- The basic workflow is sufficient for most use cases
- Discord workflow is useful for team notifications

## Testing the Setup
1. Create a test PR with any small change
2. Watch the Actions tab for the build progress
3. Check the PR comments for the build result
4. Download and test the APK

## Disable/Enable Workflows
To disable a workflow temporarily:
```bash
# Disable
mv .github/workflows/pr-apk-build.yml .github/workflows/pr-apk-build.yml.disabled

# Enable
mv .github/workflows/pr-apk-build.yml.disabled .github/workflows/pr-apk-build.yml
```