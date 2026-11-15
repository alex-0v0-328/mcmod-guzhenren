---
name: exit-session
description: Exit session for GZR — use when Alex says the goal is done, e.g. "目标已经完成，清理临时文件和环境，如果没有推送就调用推送skill". Cleans up what this task left behind, then ships anything not yet pushed through the commit-push skill.
---

# Exit Session Skill

Runs when Alex declares the goal done; `<project>` below is `guzhenren` or `guworld`, for each repository the task touched. Every step runs without an ask, the `commit-push` skill included.

1. **Temporary files.** Delete what this task left under `../../Temps/<project>/` (relative to Guzhenren's root): its task subfolders, the `logs/local_checks-<pid>.log` and `logs/commit_push-<pid>.log` its runs printed, `commit-push-plan.json`, once their results are reported. Regenerable output is deleted outright, anything that cannot be regenerated goes to the Recycle Bin (PowerShell: `Add-Type -AssemblyName Microsoft.VisualBasic`, then `[Microsoft.VisualBasic.FileIO.FileSystem]::DeleteFile(<path>, 'OnlyErrorDialogs', 'SendToRecycleBin')`, or `DeleteDirectory` for a folder), and nothing this task did not create is touched.
2. **Environment.** Stop what this session started and is still running: background shells and monitors, subagents, and any `runServer`, `runClient` or Gradle run the agent launched. Leave IDEA's Gradle daemons and Alex's own processes alone.
3. **Unshipped work.** In each repository the task touched, the wiki included, after `git fetch`, check `git status` and `git rev-list --count origin/main..HEAD`. Uncommitted changes or unpushed commits → run the `commit-push` skill. Nothing left → say so.
4. **Report.** What was deleted, what was stopped, and the ship result (SHA, push, CI) or "nothing to push".
