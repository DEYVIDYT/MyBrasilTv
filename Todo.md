# Lista de Tarefas para Correção do EPG

- [x] Investigar `TvFragment.java` para entender como os dados do EPG são carregados e exibidos.
- [x] Examinar `DataManager.java` para entender como os dados do EPG são gerenciados e fornecidos.
- [x] Examinar `XmltvEpgService.java` e `EpgService.java` para entender a análise e o cache do EPG.
- [x] Adicionar log detalhado ao `XmltvEpgService.java` para depurar a busca e análise do EPG.
- [x] Verificar o método `updateChannelProgram` do `ChannelAdapter.java`.
- [x] Depurar por que "(Recebendo programação)" é exibido em vez dos dados reais do programa (requer que o usuário execute e forneça logs).
- [x] Ajustar `SimpleDateFormat` e lógica de tempo em `XmltvEpgService.java` para garantir a correta identificação do programa atual.
- [x] Adicionar fallback para o título do programa no `Channel` se o EPG não for encontrado.
- [x] Ajustar o padrão `SimpleDateFormat` em `XmltvEpgService.java` para `yyyyMMddHHmmss'Z'` para lidar com o 'Z' literal.
- [x] Modificar `XmltvEpgService.fetchCurrentPrograms` e `parseXmltvForCurrentPrograms` para aceitar `List<Channel> allChannels`.
- [x] Implementar lógica de mapeamento robusta em `parseXmltvForCurrentPrograms` para associar XMLTV `channel` IDs/nomes aos `Channel` objects do aplicativo.
- [x] Atualizar chamadas para `fetchCurrentPrograms` no `DataManager` e no `startContinuousUpdate` para passar a lista `allChannels`.
- [x] Refinar a lógica de mapeamento em `parseXmltvForCurrentPrograms` e adicionar logs para canais não mapeados.
- [x] Remover o método `mapChannelIdToStreamId` do `XmltvEpgService.java`.
- [ ] **Adicionar tratamento de erro mais robusto e logs detalhados para parsing de XMLTV em `XmltvEpgService.java`.**
- [ ] **Garantir que o cache do EPG seja limpo ou atualizado corretamente.**
- [ ] Implementar correção para exibir os dados do EPG corretamente.
- [ ] Verificar a correção.
