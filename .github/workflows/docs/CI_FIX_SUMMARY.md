# CI Fix Summary - Fork PR Permissions Issue

## Problem
The CI was failing with "Resource not accessible by integration" errors when PRs were created from forked repositories. This happened because GitHub Actions restricts permissions for workflows triggered by PRs from forks for security reasons.

## Root Cause
When a PR comes from a fork, the `GITHUB_TOKEN` has limited permissions, even if you explicitly request `pull-requests: write` and `statuses: write` permissions in the workflow. This prevents the workflow from:
- Commenting on PRs
- Updating commit statuses

## Solution Implemented
We've implemented a two-workflow approach to fix this issue:

### 1. **Build Workflows** (Modified)
- `pr-apk-build.yml` - Basic APK build
- `pr-apk-build-discord.yml` - APK build with Discord notifications

Changes made:
- Removed PR comment and status update steps
- Reduced permissions to only `contents: read`
- Added steps to save build outputs as artifacts
- Preserved Discord notifications in the Discord variant

### 2. **Comment Handler Workflow** (New)
- `pr-apk-build-comment.yml` - Handles PR comments and status updates

Features:
- Triggered by `workflow_run` event when build workflows complete
- Runs in the base repository context with full permissions
- Downloads build outputs from the build workflow
- Posts PR comments with build results
- Updates commit statuses
- Sends Discord notifications (for Discord workflow builds)

## How It Works Now

1. **PR Created/Updated** → Build workflow runs with limited permissions
2. **Build Completes** → Uploads APK and build metadata as artifacts
3. **Comment Workflow Triggers** → Downloads metadata and posts results

This approach ensures that:
- Build workflows can run safely on PR code from forks
- Comments and status updates work for all PRs (including forks)
- No security compromises are made

## Testing
The workflows are now ready to test. Create a PR from a fork to verify that:
1. The build runs successfully
2. PR comments are posted
3. Commit status is updated
4. Discord notifications work (if enabled)

## Files Changed
- Modified: `.github/workflows/pr-apk-build.yml`
- Modified: `.github/workflows/pr-apk-build-discord.yml`
- Created: `.github/workflows/pr-apk-build-comment.yml`
- Updated: `.github/workflows/docs/PR_APK_BUILD_README.md`