# Comment Workflow Troubleshooting

## Issue: Build succeeded but no PR comment appears

### Common Causes

1. **Comment workflow didn't trigger**
   - Check the Actions tab for "PR APK Build - Comment Handler" runs
   - If none exist, the workflow wasn't triggered

2. **Artifact download failed**
   - The comment workflow needs to download artifacts from the build workflow
   - Check the comment workflow logs for download errors

3. **Permissions issue**
   - The comment workflow needs proper permissions to post comments
   - This should be automatically handled by the workflow configuration

### How to Debug

1. **Check if comment workflow ran**:
   - Go to Actions tab
   - Look for "PR APK Build - Comment Handler" workflow runs
   - Click on the run to see logs

2. **Check artifact names**:
   - Build workflow creates: `build-outputs-{run-id}`
   - Comment workflow downloads: `build-outputs-{run-id}`
   - These must match exactly

3. **Manual trigger** (for testing):
   - You can manually re-run the comment workflow from the Actions tab
   - Find the comment workflow run and click "Re-run all jobs"

### Recent Fix Applied

We've updated the comment workflow to:
- Use `dawidd6/action-download-artifact@v2` for cross-workflow artifact downloads
- Add debugging information to help troubleshoot issues
- Handle cases where artifacts might be missing
- Improve error handling and logging

### Next Steps

After these fixes, new PR builds should correctly trigger comments. For the current PR:
1. Wait for the next commit/push to trigger a new build
2. Or manually re-run the "PR APK Build with Discord" workflow
3. The comment workflow should automatically trigger after the build completes