# Cheatsheet — Git (J0.04 e J0.05)

| Objetivo | Comando |
|---|---|
| Iniciar repositório | `git init` |
| Ver estado | `git status` |
| Preparar / commitar | `git add <arq>` · `git commit -m "feat: ..."` |
| Histórico | `git log --oneline --graph` |
| Criar e trocar de branch | `git switch -c <branch>` · `git switch <branch>` |
| Juntar | `git merge <branch>` · `git rebase <base>` |
| Abortar merge/rebase | `git merge --abort` · `git rebase --abort` |
| Descartar alteração / tirar do staging | `git restore <arq>` · `git restore --staged <arq>` |
| Corrigir último commit (não publicado) | `git commit --amend` |
| Desfazer commit publicado | `git revert <hash>` |
| Guardar trabalho | `git stash` · `git stash pop` |
| Achar commit "perdido" | `git reflog` |
| Remoto | `git remote add origin <url>` · `git push -u origin <branch>` · `git fetch` · `git pull --rebase` |
| Tag | `git tag payflow-J0` · `git push origin payflow-J0` |
| Parar de rastrear (já commitado) | `git rm -r --cached target/` |

Tipos de Conventional Commits: `feat`, `fix`, `docs`, `refactor`, `test`, `build`, `ci`, `chore` (`!` ou `BREAKING CHANGE:` = mudança incompatível).
