# Quick Setup Guide for PR APK Builds

## Basic Setup
The `pr-apk-build.yml` workflow is already configured and ready to use!

**No additional setup required!** The workflow will:
- Build APK on every PR
- Post results as PR comments
- Upload APK as downloadable artifact
- Update PR status checks

## Optional: Enable Discord Notifications
To get Discord notifications for your builds:

1. **Add Discord Webhook**:
   - Go to Settings → Secrets and variables → Actions
   - Add secret: `DISCORD_WEBHOOK` with your Discord webhook URL

2. **Enable Discord Notifications**:
   - Go to Settings → Secrets and variables → Actions → Variables
   - Add variable: `ENABLE_DISCORD_NOTIFICATIONS` = `true`

That's it! Discord will now notify you when:
- Build starts
- Build succeeds or fails  
- PR comment is posted

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