# PR APK Build Workflows

This repository includes CI/CD workflows for automatically building APK files on pull requests for easier testing.

## Available Workflows

### PR APK Build (`pr-apk-build.yml`)
- **Trigger**: Runs on all pull requests to master, main, or develop branches
- **Features**:
  - Builds debug APK automatically
  - Uploads APK as a GitHub artifact
  - Comments on PR with build status and download instructions
  - Updates PR status check
  - Discord notifications (optional)
  
### Discord Setup (Optional)
To enable Discord notifications:
1. Set `DISCORD_WEBHOOK` secret in repository settings
2. Set `ENABLE_DISCORD_NOTIFICATIONS` variable to `true`

Discord will notify you:
- When build starts
- When build succeeds/fails
- When PR comment is posted

## How It Works

1. When a PR is created or updated, the build workflow automatically starts
2. It sets up the build environment (JDK 17, Android SDK)
3. Builds the debug APK from the `Tetra/` directory
4. Renames the APK with PR number and commit SHA
5. Uploads the APK as an artifact (retained for 7 days)
6. A separate comment workflow (`pr-apk-build-comment.yml`) handles:
   - Posting comments on the PR with build status
   - Updating commit statuses
   - Sending Discord notifications (if enabled)
   
**Important**: The workflow uses a two-stage approach to support PRs from forks:
- Stage 1: Build workflow runs with limited permissions (safe for fork PRs)
- Stage 2: Comment workflow runs after build completion with full permissions

## Downloading the APK

### From PR Comments
1. Look for the latest automated comment on your PR (each build creates a new comment)
2. Click on the "workflow artifacts" link
3. Download the APK file

### From Actions Tab
1. Go to the Actions tab in the repository
2. Find your PR's workflow run
3. Scroll to the "Artifacts" section at the bottom
4. Click on the artifact to download

## Configuration

### Secrets Required
- `GITHUB_TOKEN`: Automatically provided by GitHub Actions
- `DISCORD_WEBHOOK` (optional): For Discord notifications

### Permissions Required
The workflows use different permissions for security:

**Build workflows** (`pr-apk-build.yml`, `pr-apk-build-discord.yml`):
- `contents: read` - To checkout and build the code

**Comment workflow** (`pr-apk-build-comment.yml`):
- `contents: read` - To access workflow artifacts
- `pull-requests: write` - To comment on PRs
- `statuses: write` - To update PR status checks
- `actions: read` - To download artifacts from other workflows

### Environment Variables
- `ENABLE_DISCORD_NOTIFICATIONS`: Set to `true` to enable Discord notifications

## Troubleshooting

### PRs from Forks
The workflows are designed to work with PRs from forked repositories. If you see "Resource not accessible by integration" errors, ensure you're using the latest version of the workflows which includes the `pr-apk-build-comment.yml` workflow for handling comments and status updates.

### Missing Comments
If PR comments aren't appearing:
1. Check that `pr-apk-build-comment.yml` exists and is enabled
2. Verify the build workflow completed and uploaded artifacts
3. Check the Actions tab for any errors in the comment workflow

## Artifact Naming Convention
APKs are named as: `tetra-pr-{PR_NUMBER}-{SHORT_SHA}.apk`
- Example: `tetra-pr-123-a1b2c3d.apk`

## Retention Policy
- APK artifacts are retained for 7 days
- After 7 days, they are automatically deleted

## Comment Behavior
- Each build creates a **new comment** on the PR
- Comments are never updated or edited
- This allows you to track the history of all builds
- Each comment includes a build number and timestamp for easy identification
- Latest build comment will be at the bottom of the PR conversation

## Troubleshooting

### Build Failures
- Check the workflow logs in the Actions tab
- Common issues:
  - Missing dependencies
  - Gradle build errors
  - Permission issues

### APK Not Found
- Ensure the build completed successfully
- Check if the artifact upload step completed
- Verify the APK path in the workflow matches your project structure

## Customization

To modify the workflows for your needs:
- Change the target branches in the `on.pull_request.branches` section
- Adjust the retention period in `upload-artifact` action
- Modify the APK naming convention
- Add additional build flavors or product variants