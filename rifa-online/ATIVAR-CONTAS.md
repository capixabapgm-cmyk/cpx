# Ativar contas e convites sem apagar as rifas existentes

A atualização adiciona contas individuais e backups online ao banco atual `minhas-rifas`. As páginas de pedidos continuam funcionando antes da ativação. Login exige concluir os passos abaixo. Não instale o APK 1.7 antes de ativar sua conta administrativa; a versão nova pede login.

## 1. Novas tabelas

No console D1 do banco existente, execute os comandos de [migrations/0002_accounts.sql](migrations/0002_accounts.sql) **uma única vez, na ordem do arquivo**. São cinco CREATE TABLE, três CREATE INDEX e três ALTER TABLE. Não execute a migração inicial novamente nem exclua as tabelas atuais. Esta migração adiciona tabelas e colunas, mantendo pedidos e rifas existentes.

## 2. Código de ativação privado

Abra o Worker `minhas-rifas-pedidos` → Settings / Configurações → Variables and Secrets / Variáveis e segredos → Add / Adicionar. Cadastre um **Secret** chamado `ADMIN_SETUP_KEY`, com um valor aleatório de pelo menos 32 caracteres. Use o gerador de senhas do seu gerenciador para criar um código longo e guarde-o. Não coloque esse valor no GitHub nem envie pelo chat ou WhatsApp. Não altere nem exclua esse segredo após ativar; ele protege o armazenamento das senhas.

O e-mail de administrador já está configurado como `franciscoandrade.photo@gmail.com` no código (`ADMIN_EMAIL`). Salve e publique a configuração.

## 3. Ative sua conta

Abra https://minhas-rifas-pedidos.franciscoandrade-photo.workers.dev/admin.html → **Primeira ativação do administrador**.

Informe nome, seu e-mail configurado, uma senha nova com pelo menos 12 caracteres e o código privado `ADMIN_SETUP_KEY`. Toque em Ativar minha conta. Você define a senha nessa página; não precisa enviá-la a ninguém. A ativação inicial só permite o e-mail configurado e um único administrador.

## 4. Atualize e importe suas rifas

Instale `Minhas-Rifas-v1.7.apk` por cima da instalação anterior. Não desinstale nem limpe os dados. Entre com o mesmo e-mail e senha da sua conta administrativa. Abra **Minha conta → Importar rifas da versão anterior**. Os dados originais permanecem no aparelho. Depois toque em **Sincronizar meus dados** e confira a confirmação de gravação online.

O backup inclui rifas, compradores, telefones, pagamentos, resultado do sorteio, imagem reduzida e configurações de pedidos. Cada conta tem cache privado e revisões de sincronização. Se houver conflito com outro aparelho, os dados locais são mantidos; restaure a nuvem somente após conferir. A restauração explícita também guarda uma cópia local anterior.

## 5. Convidar alguém

No painel, informe o e-mail do convidado, se quiser restringir o cadastro a ele, e toque em Gerar convite. Você recebe uma mensagem com o link do APK 1.7 e o código, pronta para compartilhar no WhatsApp. O convite vale sete dias e permite somente um cadastro. O código só aparece na criação; se perder a mensagem, cancele o convite e gere outro.

A pessoa instala o APK → Criar conta por convite → nome, e-mail, senha e código. A conta nova começa sem suas rifas e sem sua chave Pix. Em Minha conta / Pix, cada organizador informa seu destinatário e sua chave.

Você pode cancelar convites não usados e bloquear usuários no painel. O painel lista contas e convites; não mostra compradores de outros organizadores.

## Limites deste teste

- Não envia mensagens de convite automaticamente: abre compartilhamento ou copia a mensagem para você enviar.
- Não envia e-mail de verificação ou recuperação de senha. Guarde as senhas; recuperação por e-mail precisa de uma integração própria.
- Sessões duram 30 dias; bloqueio e logout impedem acesso online. Um aparelho offline ainda pode acessar o cache da própria conta já autenticada.
- Sincronização funciona com o app aberto. Não há notificações push nem processamento contínuo em segundo plano.
- Backup limitado a 100 rifas e aproximadamente 1,8 MB por envio, para respeitar limites do banco. Se ultrapassar, os dados locais ficam preservados e o serviço retorna erro; não considere o backup concluído sem a confirmação.
- Backups de contas diferentes são isolados no servidor. Administrador não tem endpoint para consultar backups de outros usuários.
- Testes locais passaram; ativação, login, importação e restauração num celular ainda precisam ser conferidos no Cloudflare real.
