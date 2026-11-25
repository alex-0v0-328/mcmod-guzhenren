---
name: exit-session
description: Exit session for GZR — use when Alex says the goal is done or asks to end the session, e.g. "目标已经完成，清理临时文件和环境，如果没有推送就调用推送skill" or "结束会话". Cleans up what this task left behind, ships anything not yet pushed through the commit-push skill, then archives this session.
---

# Exit Session Skill

Runs when Alex declares the goal done; `<project>` below is `guzhenren` or `guworld`, for each repository the task touched. Every step runs without an ask, the `commit-push` skill and the archive included.

1. **Temporary files.** Delete what this task left under `../../Temps/<project>/` (relative to Guzhenren's root), leaving untouched anything this task did not create:
   - Delete this task's subfolders (the commit plan lives in one).
   - Delete `logs/local_checks-<pid>.log` and `logs/commit_push-<pid>.log` printed by its runs.
   - Delete any `ship/failed-<pid>/` export a failed verification of its own kept, once results are reported.
   - Leave the rest of `ship/` (slots, verification records, partner exports) alone — it belongs to every session and cleans itself.
   - Delete regenerable output outright.
   - Send anything that cannot be regenerated to the Recycle Bin (PowerShell: `Add-Type -AssemblyName Microsoft.VisualBasic`, then `[Microsoft.VisualBasic.FileIO.FileSystem]::DeleteFile(<path>, 'OnlyErrorDialogs', 'SendToRecycleBin')`, or `DeleteDirectory` for a folder).

2. **Environment.** Stop what this session started and is still running: background shells and monitors, subagents, and any `runServer`, `runClient`, or Gradle run the agent launched. Leave IDEA's Gradle daemons and Alex's own processes alone.

3. **Unshipped work.** In each repository the task touched (the wiki included), run `git fetch`, then check `git status` and `git rev-list --count origin/main..HEAD`.
   - Uncommitted changes or unpushed commits → run the `commit-push` skill.
   - Nothing left → say so.

4. **Report.** State what was deleted, what was stopped, and the ship result (SHA, push, CI) or "nothing to push", in the `wrap_up` recap and the closing message.

5. **Archive.** Last call of the turn, after the closing message: `mcp__ccd_session_mgmt__archive_session` with `session_id: "self"`. The conversation ends there; the app also archives side sessions that have finished.
   - Skip it, and say why in the report, when something still needs this session:
     - a ship did not land: verification failed, the push was rejected, or CI is red with the revert not yet green;
     - a side session this session started is still working;
     - Alex asked a question this turn has not answered.
   - Outside the Claude desktop app (Codex, opencode, the CLI) the archive tool does not exist; skip the step without a note.
