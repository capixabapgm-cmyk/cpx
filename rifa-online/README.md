# Minhas Rifas — pedidos pela internet

## Contas e convites — versão 1.7

Login por e-mail/senha, cadastro com convite de uso único e validade de sete dias, painel administrativo para gerar/cancelar convites e bloquear usuários, backup por conta e Pix individual. [Guia de ativação sem apagar os dados existentes](ATIVAR-CONTAS.md). É necessário executar a migração 0002 e configurar o segredo ADMIN_SETUP_KEY antes de usar o login.

Página móvel e API para o APK Minhas Rifas 1.5. GitHub armazena o código; Cloudflare Workers + D1 hospedam a página e recebem os pedidos. GitHub Pages sozinho não executa a API.

## Estado desta entrega

O serviço de pedidos já foi publicado pelo titular no Cloudflare. A versão 1.7 foi preparada e testada localmente, mas as contas exigem executar a migração 0002 e configurar ADMIN_SETUP_KEY. Nenhum plano pago é ativado pelo projeto.

## Ativar gratuitamente pelo painel

1. Crie sua conta em https://dash.cloudflare.com/sign-up e mantenha o plano **Workers Free**.
2. Em **Storage & databases → D1**, crie um banco chamado `minhas-rifas`. Copie o **Database ID**.
3. No GitHub, abra `rifa-online/wrangler.jsonc` e substitua `SUBSTITUIR_PELO_ID_DO_BANCO` pelo ID do banco. Esse ID não é senha.
4. No console do banco D1, execute os comandos de `rifa-online/migrations/0001_initial.sql`. São dois comandos CREATE TABLE e um CREATE INDEX; execute um por vez, se o console exigir.
5. Em **Workers & Pages → Create application**, conecte o GitHub e selecione o repositório `capixabapgm-cmyk/cpx`. Configure a pasta raiz como **rifa-online**. Use `npm install` como comando de build e `npx wrangler deploy` como comando de deploy. A interface do painel pode variar; o projeto é um **Worker com Static Assets**, não um Pages estático.
6. Finalizada a publicação, copie a URL HTTPS terminada em `.workers.dev`. Abra `/api/health` nesse endereço: deve aparecer `service: minhas-rifas` e `ok: true`.
7. Instale o APK 1.5 por cima do app atual, sem desinstalar. Em **Configurar pedidos pela internet**, cole essa URL e toque em **Verificar e salvar**.
8. Abra uma rifa → **Gerar link para pedidos** → **Compartilhar**. O cliente abre esse link no navegador.

Se o painel pedir um plano pago, não prossiga: este projeto usa apenas os serviços do plano Free. O subdomínio workers.dev dispensa comprar domínio.

### Alternativa em um computador com Node 24

```bash
git clone https://github.com/capixabapgm-cmyk/cpx.git
cd cpx/rifa-online
npm install
npx wrangler login
npx wrangler d1 create minhas-rifas
```

Cole o Database ID retornado em `wrangler.jsonc`, então execute:

```bash
npx wrangler d1 migrations apply DB --remote
npx wrangler deploy
```

## Como funciona

A página inclui nome do destinatário e chave Pix com botão Copiar chave antes e depois do envio do pedido. Destinatário e chave foram fornecidos pelo organizador; o cliente deve conferir o destinatário no banco. Copiar a chave ou enviar um pedido não marca pagamento no app. Valor e disponibilidade são combinados com o organizador.

- O cliente preenche nome e telefone e seleciona até 100 números disponíveis por pedido.
- Enviar registra um pedido **pendente**, sem reservar números nem confirmar venda ou pagamento.
- O aplicativo consulta pedidos automaticamente enquanto aberto e conectado, aproximadamente a cada 30 segundos por rifa publicada. Com várias rifas, elas são consultadas em rodízio.
- Pedidos permanecem no banco mesmo com o celular desconectado. Abra o app ou use **Atualizar pedidos** para recebê-los.
- Aceitar verifica os números no celular e no servidor, cadastra o comprador e marca os números como **não pagos**. Recusar não altera as vendas.
- Vendas manuais e alterações da rifa são publicadas pelo app na próxima sincronização. A disponibilidade pública pode ficar desatualizada enquanto o celular estiver offline; a aprovação verifica conflitos.
- Após marcar pagamento no app, somente os números pagos podem entrar no sorteio. Vendas antigas e novas vendas manuais continuam pagas por padrão.
- Rifas já sorteadas não recebem novas vendas. O fechamento da página é publicado na próxima sincronização.

## Dados e acesso

A página só recebe nome e telefone de quem envia pedido e mostra a disponibilidade dos números. Consultar pedidos ou aprová-los exige uma chave privada por rifa, criada e guardada no celular; o servidor guarda apenas seu hash. A chave não está no link público ou no GitHub. A página pública recebe apenas os números ocupados, nome/descrição da rifa e sua imagem. Na versão 1.7, dados completos de compradores também são salvos no backup privado da conta, sem acesso público.

Este teste usa um celular organizador por rifa. Desinstalar ou limpar o app perde a chave e os dados locais. Nas versões anteriores à 1.7, o banco online guarda apenas pedidos. A versão 1.7, após ativada, também mantém o backup privado por conta. Preserve a instalação atual e exporte seus compradores. Não copie a chave de assinatura do APK ou dados privados para o repositório público.

## Plano gratuito e limites

Workers Free inclui 100 mil chamadas diárias; D1 Free inclui 5 milhões de linhas lidas e 100 mil escritas por dia, com limites de armazenamento. Ao ultrapassar limites Free, operações ficam indisponíveis; o projeto não ativa upgrades. Fontes oficiais:

- https://developers.cloudflare.com/workers/platform/pricing/
- https://developers.cloudflare.com/d1/platform/pricing/

Para o teste, o serviço limita a 200 novos pedidos por hora e 1.000 pedidos pendentes por rifa. O aplicativo lê até 1.200 pedidos por atualização; não é um sistema de escala comercial. Se ampliar o uso, adicione paginação, proteção antiabuso e recuperação da conta do organizador.

## Validação

Com Node 24, execute `npm test`. Os testes usam SQLite real para validar o SQL da API: privacidade, autenticação, envio pendente, reenvio sem duplicação, aprovação, conflito entre pedidos, conflito com venda manual, rejeição, encerramento e revisão de sincronização. A tela no navegador, o APK num aparelho Android e a infraestrutura Cloudflare precisam ser conferidos após ativar a conta.

## Temas por conta (APK 1.9)

`GET/PUT /api/account/theme` exige sessão e aceita apenas green/red/pink/yellow/blue/purple. O servidor usa o usuário da sessão, nunca um ID fornecido pelo cliente. A tabela `account_themes` é criada com CREATE TABLE IF NOT EXISTS no primeiro PUT; getters retornam green quando a tabela ainda não existe. Login/me retornam a cor; a página pública consulta a preferência atual do dono. Sem migração manual ou mudança de segredos. Teste de isolamento, login, validação e atualização pública sem republicação incluído em accounts.test.mjs.
