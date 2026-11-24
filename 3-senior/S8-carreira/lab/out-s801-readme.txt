### good portfolio README (PT + EN)
PASS: 0 error(s), 0 warning(s), 0 to review

### bad README
ERROR  missing-section    readme-ruim.md: no section 'o que é'
ERROR  missing-section    readme-ruim.md: no section 'o que (este projeto|ele) demonstra'
ERROR  missing-section    readme-ruim.md: no section '(como testar|testes)'
ERROR  missing-section    readme-ruim.md: no section 'decisões'
ERROR  missing-section    readme-ruim.md: no section 'limitações'
ERROR  missing-section    readme-ruim.md: no section 'licença'
ERROR  no-diagram         readme-ruim.md: no ```mermaid block (GitHub renders Mermaid in Markdown)
ERROR  run-commands       readme-ruim.md: 'Como rodar' has no shell commands in a ```bash block
ERROR  claim-no-evidence  readme-ruim.md:3: 'pronto para produção' without evidence (link) or negation
ERROR  secret             readme-ruim.md:12: password-value in a code block
WARN   placeholder        readme-ruim.md:16: 'TODO'
ERROR  broken-link        readme-ruim.md:18: relative link to 'docs/decisoes.md' does not exist
WARN   no-ci-badge        readme-ruim.md: no CI status badge (actions/workflows/<file>/badge.svg)
ERROR  missing-section    readme-ruim.en.md: no section 'what it is'
ERROR  missing-section    readme-ruim.en.md: no section 'what (this project|it) shows'
ERROR  missing-section    readme-ruim.en.md: no section 'architecture'
ERROR  missing-section    readme-ruim.en.md: no section 'how to run'
ERROR  missing-section    readme-ruim.en.md: no section '(how to test|tests)'
ERROR  missing-section    readme-ruim.en.md: no section 'decisions'
ERROR  missing-section    readme-ruim.en.md: no section '(known )?limitations'
ERROR  missing-section    readme-ruim.en.md: no section 'license'
ERROR  no-diagram         readme-ruim.en.md: no ```mermaid block (GitHub renders Mermaid in Markdown)
WARN   no-ci-badge        readme-ruim.en.md: no CI status badge (actions/workflows/<file>/badge.svg)
ERROR  parity-sections    readme-ruim.md x readme-ruim.en.md: 2 required sections in PT, 0 in EN
WARN   parity-links       readme-ruim.md x readme-ruim.en.md: links only in PT [docs/decisoes.md], only in EN []
FAIL: 21 error(s), 4 warning(s), 0 to review

### mutant 1: the EN version lost its 'Limitations' section
ERROR  missing-section    README.en.md: no section '(known )?limitations'
ERROR  parity-sections    README.md x README.en.md: 8 required sections in PT, 7 in EN
FAIL: 2 error(s), 0 warning(s), 0 to review

### mutant 2: a renamed file, the PT link still points to the old name
ERROR  broken-link        README.md:17: relative link to 'docs/design/recorrentes.md' does not exist
WARN   parity-links       README.md x README.en.md: links only in PT [docs/design/recorrentes.md], only in EN [docs/design/transferencias-recorrentes.md]
FAIL: 1 error(s), 1 warning(s), 0 to review

### mutant 3: 'pronta para produção' in the first paragraph
ERROR  claim-no-evidence  README.md:6: 'pronta para produção' without evidence (link) or negation
FAIL: 1 error(s), 0 warning(s), 0 to review
