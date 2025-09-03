# PR APK Build Workflows

This repository includes CI/CD workflows for automatically building APK files on pull requests for easier testing.

## Available Workflows

### 1. Basic PR APK Build (`pr-apk-build.yml`)
- **Trigger**: Runs on all pull requests to master, main, or develop branches
- **Features**:
  - Builds debug APK automatically
  - Uploads APK as a GitHub artifact
  - Comments on PR with build status and download instructions
  - Updates PR status check

### 2. PR APK Build with Discord (`pr-apk-build-discord.yml`)
- **Trigger**: Same as basic workflow
- **Features**: All basic features plus Discord notifications
- **Setup Required**:
  - Set `DISCORD_WEBHOOK` secret in repository settings
  - Set `ENABLE_DISCORD_NOTIFICATIONS` variable to `true`

## How It Works

1. When a PR is created or updated, the workflow automatically starts
2. It sets up the build environment (JDK 17, Android SDK)
3. Builds the debug APK from the `Tetra/` directory
4. Renames the APK with PR number and commit SHA
5. Uploads the APK as an artifact (retained for 7 days)
6. Comments on the PR with:
   - Build status
   - APK size
   - Download instructions
   - Direct link to the workflow run

## Downloading the APK

### From PR Comments
1. Look for the automated comment on your PR
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
The workflow needs the following permissions:
- `contents: read` - To checkout the code
- `pull-requests: write` - To comment on PRs
- `statuses: write` - To update PR status checks

### Environment Variables
- `ENABLE_DISCORD_NOTIFICATIONS`: Set to `true` to enable Discord notifications

## Artifact Naming Convention
APKs are named as: `tetra-pr-{PR_NUMBER}-{SHORT_SHA}.apk`
- Example: `tetra-pr-123-a1b2c3d.apk`

## Retention Policy
- APK artifacts are retained for 7 days
- After 7 days, they are automatically deleted

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