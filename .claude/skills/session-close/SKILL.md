---
name: session-close
description: Session close for GZR — use when Alex says the goal is done, e.g. "目标已经完成，清理临时文件和环境，如果没有推送就调用推送skill". Cleans up what this task left behind, then ships anything not yet pushed through the commit-push skill.
---

# Session Close Skill

Runs when Alex declares the goal done. Every step runs without an ask; only the `commit-push` skill's commit-message pick waits for him.

1. **Temporary files.** Delete what this task left under `C:\workspace\Dev\Projects\_Temp\guzhenren\`: its task subfolders, `logs\check.log` and `logs\commit_push.log`, `commit-push-plan.json`, once their results are reported. Regenerable output is deleted outright, anything that cannot be regenerated goes to the Recycle Bin, and nothing this task did not create is touched.
2. **Environment.** Stop what this session started and is still running: background shells and monitors, subagents, and any `runServer`, `runClient` or Gradle run the agent launched. Leave IDEA's Gradle daemons, Alex's own processes and the dashboard file alone; the `activate-dashboard` skill clears the board at its next launch.
3. **Unshipped work.** After `git fetch`, check `git status` and `git rev-list --count origin/main..HEAD`. Uncommitted changes or unpushed commits → run the `commit-push` skill. Nothing left → say so.
4. **Report.** What was deleted, what was stopped, and the ship result (SHA, push, CI) or "nothing to push".
