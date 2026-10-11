# Minhas Rifas

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
