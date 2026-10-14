# Minhas Rifas

## Versão 1.7 — contas separadas

Login por e-mail/senha, cadastro por convite, cache e backup por usuário, Pix por conta e painel administrativo. Instale apenas após ativar as contas no Cloudflare; veja `rifa-online/ATIVAR-CONTAS.md`. O APK mantém pacote e assinatura, versionCode 8. Rifas da versão anterior ficam guardadas no aparelho; o administrador pode importá-las após entrar na conta. Não desinstale nem limpe os dados. Senhas não são guardadas no app; sessões são armazenadas na área privada do aplicativo.

Aplicativo Android nativo, offline, em português. Compatível com Android 8 ou posterior.

- Várias rifas com nome, descrição, imagem e 1 a 10.000 números.
- Tela vertical com rolagem, números vendidos verdes e progresso das vendas.
- Cadastro e edição de nome e telefone opcional por número; opção para liberar uma venda cadastrada por engano.
- Lista de compradores e exportação CSV pelo seletor de arquivos Android.
- Botão de sorteio logo abaixo da imagem, habilitado após vender todos os números.
- Sorteio com SecureRandom, resultado persistido e vendas bloqueadas após o sorteio.
- Dados e imagens no armazenamento privado do app. Não requer internet ou permissão de contatos.

## Instalar

Transfira `Minhas-Rifas.apk` para o celular, abra o arquivo e permita a instalação pelo aplicativo utilizado para abri-lo. Os dados ficam neste celular; desinstalar ou limpar os dados apaga as rifas. Exporte a lista para guardar uma cópia dos compradores. A exportação CSV não restaura rifas.

## Atualização 1.1

Tela compacta: imagem com 110dp de altura, espaçamentos menores, ações lado a lado e grade responsiva com até 8 colunas. Números com 38dp de altura e texto de 12sp. Telefone opcional; o nome continua obrigatório.

Instale `Minhas-Rifas-v1.1.apk` sobre a instalação existente, sem desinstalar nem limpar os dados. O pacote `br.com.minhasrifas`, a chave de assinatura e as preferências `rifas` foram mantidos; versionCode passou de 1 para 2. A compatibilidade da atualização foi verificada pela identidade do pacote, assinatura e versão. A instalação e o uso ainda precisam ser conferidos num aparelho Android.

## Atualização 1.3

Lista compacta de compradores no formato `Nome — 001, 002, 003`, com nome da rifa e total vendido no topo para capturas de tela. O controle Mostrar telefones oculta ou exibe uma linha adicional por comprador e lembra a escolha; inicialmente fica desligado. Telefones permanecem salvos e continuam no CSV e nos comprovantes. Toque numa linha para abrir o comprovante. Os botões de ação ficam no final da lista.

Instale `Minhas-Rifas-v1.3.apk` usando Atualizar, sem desinstalar ou limpar os dados. Mantém pacote, chave de assinatura e armazenamento, com versionCode 4.

## Atualização 1.4

Vendas existentes sem status são migradas para pago; status explícitos de não pago são preservados nas reaberturas. Novos cadastros individuais e em lote têm Pago marcado por padrão. Na lista, toque no status para alterar todos os números do comprador; no número individual, altere a caixa Pago. O comprovante informa os números pagos e não pagos.

O sorteio continua exigindo todos os números vendidos. Participam somente os números pagos, com chance igual por número; não pagos são excluídos. Com nenhum número pago, o sorteio é bloqueado. Resultados antigos permanecem salvos; rifas já sorteadas não permitem alterações de pagamento.

Instale `Minhas-Rifas-v1.4.apk` como atualização. Mantém assinatura, pacote e dados, versionCode 5.

## Compilar

## Atualização 1.6

Botão Pix / copiar chave na rifa e dados Pix no comprovante compartilhado. Destinatário Francisco Claudemir da Silva Andrade, chave 78216834268, conforme fornecidos pelo organizador. Conferir o destinatário no banco; não há confirmação automática, cálculo de valor ou alteração automática de pagamento. Instale `Minhas-Rifas-v1.6.apk` como atualização sem desinstalar; mantém dados, assinatura e pacote, versionCode 7.

## Atualização 1.5

Integração com o serviço `rifa-online` para publicar um link por rifa, receber pedidos pendentes, aceitar como não pagos ou recusar. Configure o endereço HTTPS da hospedagem ativada no Cloudflare. Sem hospedagem configurada, as funções locais continuam disponíveis. O app sincroniza enquanto aberto e conectado; não usa notificações push ou serviço em segundo plano. A sincronização atualiza a disponibilidade e guarda os pedidos para consulta.

Mantém pacote, assinatura e armazenamento, com versionCode 6. Instale `Minhas-Rifas-v1.5.apk` como atualização, sem desinstalar ou limpar os dados. A hospedagem precisa ser ativada antes de gerar links funcionais; veja o README de `rifa-online`.

## Atualização 1.2

Cadastro de vários números disponíveis para um comprador novo ou já cadastrado. A lista agrupa os números da mesma rifa por nome e telefone normalizados, inclusive os registros das versões anteriores. Pessoas diferentes com o mesmo nome e sem telefone precisam de uma identificação diferente no nome para não serem agrupadas.

O comprovante de participação lista rifa, descrição, comprador, telefone se informado, quantidade, números e horário de emissão. Compartilhar abre o seletor Android: escolha WhatsApp e o contato para enviar o texto. O comprovante pode ser consultado novamente na lista de compradores. Não é recibo de pagamento, pois o app não controla pagamentos.

Instale `Minhas-Rifas-v1.2.apk` sobre a instalação atual. Mantém pacote, assinatura, imagens e armazenamento, com versionCode 3. Não desinstale nem limpe os dados.

### Compilação

Execute `bash build.sh` com Java 21 (módulo jdk.compiler), Android SDK plataforma 35 e build-tools 35.0.0. Ajuste ANDROID_SDK_ROOT se necessário. O APK é assinado com uma chave local de desenvolvimento; guarde `build/local-signing.jks` para atualizações da mesma instalação. Não é uma configuração de publicação na Play Store.

## Verificação no celular

1. Crie uma rifa com 2 números e selecione uma imagem.
2. Cadastre compradores nos números; confirme a cor verde e o contador.
3. Feche e reabra o app para verificar os dados.
4. Exporte a lista CSV e confira os nomes e telefones.
5. Com um número disponível, confirme que o sorteio está desabilitado.
6. Venda o último número, sorteie e reabra o resultado. Confirme que não é possível sortear novamente ou editar as vendas.
7. Crie outra rifa e confirme que suas vendas são independentes.

## Versão 1.8 — notificações e aparência

Configurações / cores / notificações permite seis cores (verde, vermelho, rosa, amarelo, azul, roxo). Vendidos permanecem verdes; amarelo usa texto escuro para contraste. Preferências visuais são locais e não alteram as rifas ou backups.

Avisos usam IDs de pedidos com registro persistente por usuário e rifa, evitando repetição e detectando novos pedidos mesmo quando o total de pendentes não muda. O toque abre os pedidos da rifa. Android 13+ pede POST_NOTIFICATIONS; versões anteriores usam o canal de notificações. É possível desativar avisos nas configurações.

JobScheduler consulta em segundo plano com internet, período mínimo de 15 minutos, persistência após reiniciar e sem serviço contínuo. Economia de bateria/Doze pode atrasar a execução; forçar parada impede execução até reabrir. Não é push instantâneo. O job apenas consulta e gera avisos: nunca muda vendas, importa compradores ou grava backup. Usa a sessão e conta atuais e cancela avisos e agendamento ao sair. Pedidos já presentes no cache antes da atualização não geram avisos retroativos.

Atualize por cima da versão 1.7 com a mesma assinatura. Não precisa executar SQL nem mudar segredos no Cloudflare. A publicação atualiza apenas o link de APK dos convites. Compilação, assinatura e testes de detecção de pedidos foram verificados localmente; permissão, toque e execução em segundo plano precisam ser conferidos no aparelho real.

## Versão 1.9 — tema por conta

A cor principal agora vale para botões, cabeçalho, destaques e números vendidos. Disponíveis mantêm fundo claro; amarelo usa texto escuro. A preferência fica na conta online, independente das rifas e das outras contas, e se aplica à página pública sem republicar as rifas. Login em outro aparelho recupera o tema. Mudanças offline ficam pendentes e são enviadas ao reconectar com o aplicativo aberto; uma alteração feita enquanto há uma requisição em andamento não é apagada pela resposta anterior. A escolha local da versão 1.8 é enviada à conta na primeira atualização.

Não é necessário executar SQL: a tabela `account_themes` é criada automaticamente ao salvar a primeira preferência. Nenhuma tabela existente, rifa, venda, convite ou segredo é alterado. Instale 1.9 por cima da versão atual, com a mesma assinatura.

## Versão 1.9.1 — correção de inicialização

Adiciona ACCESS_NETWORK_STATE, exigida ao agendar jobs que dependem de rede em Android recente. Restrições de permissão/estado/agendamento não propagam pela abertura do aplicativo. Preserva pacote, assinatura, dados e temas da versão 1.9. Teste JVM reproduz as exceções de segurança e indisponibilidade do agendador. Não há emulador ou celular conectado para confirmar a falha relatada; validar abertura no aparelho real após atualizar.

## Versão 1.9.2 — cabeçalho sem corte

Imagem de cabeçalho e prévia do editor usam FIT_CENTER, adjustViewBounds e altura variável, respeitando a proporção original. A página pública usa largura responsiva e altura automática. Não altera imagens salvas, rifas, compradores ou contas. Instale por cima da versão atual sem desinstalar.

## Versão 1.9.3 — pedidos pagos por padrão e comprovante

Novos pedidos online aceitos/importados entram como pagos. Alteração manual para não pago continua disponível e pagamentos de vendas já existentes são preservados. Comprovante remove a linha Números não pagos e termina com Obrigado Deus abençoe! Atualize sem desinstalar; pacote e assinatura permanecem iguais.

## Versão 1.9.4 — resolver duplicados e excluir pedidos

Em Pedidos recebidos: Resolver pedidos duplicados compara pedidos ativos que têm números em comum; escolha o pedido mantido e confirme a lista dos removidos. Aceita o escolhido e cancela os demais atomicamente, liberando seus números exclusivos. Mostrar pedidos aceitos permite excluir também um pedido já aceito.

Excluir pedido / liberar números exige conexão e confirmação. Libera apenas vendas vinculadas pelo requestId ao pedido cancelado, preservando vendas manuais e outros compradores. Pedidos recebidos são arquivados como cancelados/recusados, com registro persistente para não voltar após sincronizar outro aparelho. Histórico de rifas já sorteadas fica bloqueado.

A preferência de pagamentos existente é preservada; o novo pedido escolhido entra como pago, conforme a versão 1.9.3. Todos os aparelhos que gerenciam a mesma conta devem usar 1.9.4 ou posterior; versões antigas não podem republicar uma rifa que teve pedido excluído. Banco adiciona automaticamente request_cancellations, sem comandos manuais ou mudança de segredos. Aguarde a publicação do Worker pelo Cloudflare antes de usar os novos botões.

Testes locais cobrem liberação exclusiva, escolha entre aceito/pendente, preservação de venda manual, concorrência, falha com rollback, isolamento por conta, backup antigo e cancelamentos fora da primeira página de pedidos. Compilação e assinatura verificadas; fluxo completo ainda deve ser conferido no celular/Cloudflare real.

## Versão 1.9.5 — excluir cliente em Compradores

Cada cliente tem Excluir cliente / liberar números, com confirmação do nome e números. Vendas manuais são removidas localmente e sincronizadas; pedidos online vinculados são cancelados no servidor antes da liberação. Demais compradores são preservados, inclusive quando um pedido foi dividido/reassociado: seus números ficam como vendas manuais. Exclusão fica bloqueada após o sorteio. Se vários pedidos forem cancelados e ocorrer falha de rede, mostra progresso parcial e permite tentar novamente para os números restantes. Atualize por cima, sem limpar dados.

## Versão 1.9.6 — coordenação entre conta e pedidos

Sincronização de pedidos/publicação e sincronização de conta/backup/tema não começam simultaneamente. O polling alterna as fases para evitar que o backup mude a revisão enquanto a rifa está sendo publicada. A atualização manual de pedidos aguarda a conta terminar, com prazo de 30 segundos. O servidor continua bloqueando publicações e backups desatualizados; o aviso agora descreve mudança de versão sem atribuí-la necessariamente a outro aparelho. Nenhum dado, pagamento ou sorteio é redefinido.

## Versão 1.9.7 — aceitação independente da publicação

Aceitar/recusar consulta o pedido atual e envia a decisão diretamente, sem exigir publicação da rifa ou sucesso do backup antes. Aguarda operações em andamento, resolve a referência atual da rifa após restaurações e trata aceitação repetida como idempotente. O servidor retorna a lista atualizada junto com a decisão; clientes usam GET como fallback para servidores antigos.

Importa os pedidos válidos mesmo quando outro aceito tem conflito com vendas locais; preserva vendas manuais/pagamentos existentes, destaca conflitos e bloqueia publicação da disponibilidade enquanto persistirem. Conflito no backup continua sendo tratado como problema separado e não impede a decisão de um pedido válido. Não faz restauração forçada nem sobrescreve dados de outro aparelho.

Testes JVM validam sequência GET/POST sem dependência de backup, repetição, recusa e isolamento de conflitos. Teste de API reproduz backup com revisão antiga e mostra aceitação válida mantendo bloqueio de publicação antiga. Assinatura e compilação verificadas; conferir fluxo no aparelho real após atualizar.

## Versão 1.9.8 — configurar visual da rifa e rodapé

Na tela da rifa, Configurar permite mostrar/ocultar todos os botões entre a imagem e os números. Configurar permanece visível; informações de vendas e pagamentos continuam disponíveis. Preferência fica por conta/por rifa neste aparelho e é mantida ao reabrir. Rodapé após os números destaca Obrigado Deus abençoe! e os créditos Minhas Rifas - desenvolvido por Francisco Andrade. Página pública também recebe agradecimento e créditos. Não altera aceitação, estoque, pagamentos ou sincronização.

## Versão 1.9.9 — Configurar abre/recolhe diretamente

Configurar alterna o painel de controles logo abaixo do botão, sem abrir diálogo. Primeiro toque mostra sorteio, compradores, editar, pedidos, Pix e adicionar comprador; segundo toque recolhe. Ao abrir a rifa, o painel começa fechado. Informações, números e rodapé continuam visíveis. Preferências antigas de exibição deixam de controlar o painel; dados da rifa e da conta permanecem intactos.

## Versão 1.9.10 — compartilhar link publicado sem sincronizar

Compartilhar link publicado abre o compartilhamento diretamente para rifas já publicadas, sem depender de backup ou sobrescrever disponibilidade. Publicar alterações e atualizar pedidos fica separado para atualizar imagem/descrição/disponibilidade com as verificações de revisão mantidas. Uma rifa ainda não publicada exige a primeira publicação.

Atualizar pedidos e abrir a lista fazem apenas GET/importação local, sem PUT de publicação ou validação da revisão do backup. Preservam aceitação, cancelamentos e pagamento existentes. Um conflito verdadeiro de backup/publicação não é resolvido com restauração forçada nem ocultado: deve ser conciliado separadamente; o link existente continua utilizável.
