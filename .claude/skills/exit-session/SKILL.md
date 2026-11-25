---
name: exit-session
description: Exit session for GZR — use when Alex says the goal is done or asks to end the session, e.g. "目标已经完成，清理临时文件和环境，如果没有推送就调用推送skill" or "结束会话". Cleans up what this task left behind, ships anything not yet pushed through the commit-push skill, then archives this session.
---

> **Trigger**: Alex declares the goal done or asks to end the session.
> **Scope**: Every touched repo (`<project>` = `guzhenren` / `guworld`, plus `wiki`).
> **Execution**: Run every step straight through without asking, including `commit-push` and archive.

## Flow & Gates

| Stage | Action | Gate / Skip Rule |
| --- | --- | --- |
| Cleanup temp | Under `../../Temps/<project>/`, delete only this task's subfolders (incl. commit plan),<br>its `logs/local_checks-<pid>.log` & `logs/commit_push-<pid>.log`, and `ship/failed-<pid>/`<br>Delete regenerable files outright; send non-regenerable files to Recycle Bin via PowerShell:<br>`Add-Type -AssemblyName Microsoft.VisualBasic;`<br>`[Microsoft.VisualBasic.FileIO.FileSystem]::DeleteFile(<p>, 'OnlyErrorDialogs', 'SendToRecycleBin')`<br>(`DeleteDirectory` for folders) | Leave other tasks' files and the rest of `ship/`<br>(slots, verified records, partner exports) untouched |
| Stop env | Stop background shells, monitors, subagents,<br>and `runServer`/`runClient`/Gradle processes launched by this session | Never touch IDEA's Gradle daemons or Alex's own processes |
| Ship work | In every touched repo (incl. wiki), run `git fetch`, `git status`,<br>`git rev-list --count origin/main..HEAD`<br>If uncommitted changes or unpushed commits exist → run `commit-push` skill | Report `"nothing to push"` if clean |
| Report | Report deleted items, stopped processes, and ship result (SHA, push, CI, or nothing to push)<br>in `wrap_up` recap and closing message | All cleanup and ship outcomes explicitly stated |
| Archive | Last call after closing message: `mcp__ccd_session_mgmt__archive_session` (`session_id: "self"`) | Skip (and state why) if ship failed/rejected/CI red without green revert,<br>a spawned side session is still running, or Alex's question is unanswered<br>Outside Claude desktop app (Codex/opencode/CLI): skip silently |
