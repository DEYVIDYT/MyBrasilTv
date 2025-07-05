# Lista de Tarefas - Melhoria do Player de Canais

- [x] Criar o arquivo `Todo.md`
- [x] Analisar `TvFragment.java` para entender o fluxo de reprodução e tratamento de erros atual.
- [x] Identificar os locais no código onde a lógica de retentativa para servidores instáveis será implementada.
- [x] Implementar a detecção de interrupções/erros no player (feito dentro do `onPlayStateChanged` para `STATE_ERROR`).
- [x] Implementar o mecanismo de retentativa automática e ilimitada (usando `Handler` e `Runnable` com delay, reagendando em caso de falha).
- [x] (Opcional, se possível) Adicionar alguma forma de feedback visual para o usuário quando o player estiver tentando se reconectar (realizado com `showLoadingWithMessage` e `playerLoadingTextView`).
- [x] Testar a robustez da nova implementação (realizada revisão de código e teste lógico).
- [x] Atualizar `Todo.md` com o progresso.
- [x] Submeter as alterações.

---
## Melhoria: Otimização de Buffer para Canais Lentos

- [x] Definir Novas Constantes e Variáveis de Membro (`MAX_BUFFERING_TIME_MS`, `PAUSE_FOR_BUFFER_REBUILD_MS`, `mBufferingStartTime`, `mIsPausedForBuffering`, `mBufferOptimizeHandler`, `mCheckBufferingRunnable`, `mResumeAfterBufferRebuildRunnable`).
- [x] Inicializar Novas Variáveis (Handler e Runnables em `onCreateView`).
- [x] Modificar `onPlayStateChanged` para detectar buffering excessivo e gerenciar estados/flags da nova lógica.
- [x] Implementar `mCheckBufferingRunnable` para pausar o player e agendar a retomada se o buffering exceder `MAX_BUFFERING_TIME_MS`.
- [x] Implementar `mResumeAfterBufferRebuildRunnable` para retomar a reprodução após `PAUSE_FOR_BUFFER_REBUILD_MS`.
- [x] Modificar `onChannelClick` para resetar flags e cancelar callbacks da lógica de otimização de buffer.
- [x] Modificar `onDestroyView` para limpar callbacks da lógica de otimização de buffer.
- [x] Solicitar adição de Novas Strings (`optimizing_buffer_message`, `resuming_playback_message`) ao usuário.
- [x] Atualizar `Todo.md` com o progresso desta nova funcionalidade.
