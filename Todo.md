# Lista de Tarefas - Melhoria do Player de Canais

- [x] Criar o arquivo `Todo.md`
- [x] Analisar `TvFragment.java` para entender o fluxo de reprodução e tratamento de erros atual.
- [x] Identificar os locais no código onde a lógica de retentativa para servidores instáveis será implementada.
- [x] Implementar a detecção de interrupções/erros no player (feito dentro do `onPlayStateChanged` para `STATE_ERROR`).
- [x] Implementar o mecanismo de retentativa automática e ilimitada (usando `Handler` e `Runnable` com delay, reagendando em caso de falha).
- [x] (Opcional, se possível) Adicionar alguma forma de feedback visual para o usuário quando o player estiver tentando se reconectar (realizado com `showLoadingWithMessage` e `playerLoadingTextView`).
- [x] Testar a robustez da nova implementação (realizada revisão de código e teste lógico).
- [x] Atualizar `Todo.md` com o progresso.
- [ ] Submeter as alterações.
