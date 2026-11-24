# A cobertura que mentia (projeto de estudo PayFlow)
Temas: crescimento, iniciativa, perseverança

**Situação:** No PayFlow, meu projeto de estudo, as regras de datas das transferências recorrentes tinham 100% de cobertura de linhas e ramos no relatório do JaCoCo.
**Tarefa:** Eu queria saber se esses testes realmente pegariam um erro nas regras de dinheiro antes de chamar a funcionalidade de pronta.
**Ação:** Rodei o PIT, a ferramenta de teste de mutação, e li cada mutante sobrevivente em vez de olhar só o número. Percebi que os testes executavam o código sem verificar o resultado nas fronteiras: dia 31, fevereiro, valor zero. Escrevi testes de valor-limite para cada fronteira, confirmei que cada um falhava com o mutante correspondente e registrei no CONTRIBUTING a regra de exigir escore de mutação nos pacotes de dinheiro.
**Resultado:** A suíte antiga matava 6 de 18 mutantes; a nova matou 18 de 18. Aprendi a desconfiar de métrica que dá para cumprir sem verificar nada, e hoje eu começo a revisão de testes pelos mutantes sobreviventes.
