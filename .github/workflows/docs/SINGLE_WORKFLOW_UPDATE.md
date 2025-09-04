# Single Workflow Update Summary

## Changes Made

### 1. Consolidated Workflows
- **Removed**: `pr-apk-build-discord.yml` (separate Discord workflow)
- **Removed**: `pr-apk-build.yml.disabled` (old disabled workflow)
- **Created**: Single `pr-apk-build.yml` that includes Discord support

### 2. New Workflow Features
The single `pr-apk-build.yml` now:
- Builds the APK for all PRs
- Sends Discord notifications (if enabled):
  - When build starts
  - When build succeeds
  - When build fails
- Saves artifacts for the comment workflow
- Works seamlessly with the comment handler

### 3. How It Works
```
PR Created/Updated
    ↓
pr-apk-build.yml runs
    ├── Discord: "Build Started" (if enabled)
    ├── Builds APK
    ├── Uploads APK artifact
    ├── Saves build info
    └── Discord: "Success/Fail" (if enabled)
    ↓
pr-apk-build-comment.yml triggers
    ├── Downloads build info
    ├── Posts PR comment
    ├── Updates commit status
    └── Discord: "Comment Posted" (if enabled)
```

### 4. Discord Configuration
Discord notifications are optional and controlled by:
- `DISCORD_WEBHOOK` secret (webhook URL)
- `ENABLE_DISCORD_NOTIFICATIONS` variable (set to `true`)

If these aren't set, the workflow runs normally without Discord notifications.

### 5. Benefits
- Single workflow to maintain
- Discord notifications are optional
- PR comments still work for fork PRs (via comment handler)
- No duplicate builds
- Clear separation of concerns