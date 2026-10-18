# Corre no Corte — Android 0.6.0

[Baixar o APK atualizado](./Corre-no-Corte-v0.6.apk?raw=true)

Android 8 ou posterior. Arquivo assinado, aproximadamente 23 MB.

Esta atualização acrescenta **Ajustes da imagem**:

- Brilho, contraste, vibração das cores e nitidez com prévia de um quadro.
- Defina uma vez e toque **Aplicar aos próximos vídeos**. A configuração fica salva.
- Os ajustes são aplicados automaticamente antes de salvar o arquivo na pasta
  da galeria; não é necessário editar cada passagem.
- **Zerar ajustes** volta ao neutro. Confirme em **Aplicar aos próximos vídeos**.

Trechos já enfileirados mantêm o preset anterior, inclusive em retomadas.
Vídeos já prontos não são reprocessados. A nitidez da imagem realça contornos;
o modo **Priorizar nitidez** continua controlando o obturador em câmeras compatíveis.

Para liberar o controle manual, o modo de nitidez pode selecionar 30 ou 60 fps.
Desligando a opção, o app volta a tentar 120 fps quando disponível. O suporte
efetivo aos controles no Galaxy A05s precisa ser verificado no aparelho.

Mantém as melhorias de centralização e interpolação do recorte vertical.

Mantém a gravação por **2 segundos reais após a última detecção na área marcada**.
Se o atleta reaparecer, a contagem reinicia. Depois, o app salva e aguarda.

O recorte respeita os limites da imagem original; nas bordas, a centralização
fica limitada à área que a câmera gravou.
A captura a 30 fps agora gera câmera lenta de **2×**, com repetição de quadros;
60 fps gera **2×** e 120 fps gera **4×**. O vídeo final tem até **30 segundos**.
A repetição a 30 fps aumenta a duração, com menor fluidez de movimento.

Instale por cima da versão anterior, **sem desinstalar**, para manter os dados.
Grave um novo trecho para conferir as correções: vídeos já concluídos mantêm
o processamento anterior. Abra o app, permita o acesso à câmera e fixe o celular
na horizontal. Marque a área da pista e toque **Iniciar**. Ao terminar, use
**Parar e finalizar**; confira os arquivos em **Meus vídeos** ou na galeria.

Compilação, assinatura, rastreador, buffer, orientação, centralização, duração e
política de exposição e filtros foram verificados por testes automatizados. Esta versão ainda precisa de confirmação com a
câmera, codecs e GPU do Galaxy A05s; não houve teste físico desta atualização.

O APK solicita somente CAMERA. Modelo e processamento são locais, sem envio
de imagens a servidores. Os originais são preservados na área privada do app.

SHA-256 da versão 0.6:

```text
5051b7ff92a89dea793a01204855b294375e281f3a28a9ebcb55adb309467c62
```

[Versão anterior 0.5](./Corre-no-Corte-v0.5.apk?raw=true) · [Versão 0.4](./Corre-no-Corte-v0.4.apk?raw=true) · [Versão 0.3](./Corre-no-Corte-v0.3.apk?raw=true) · [Versão 0.2](./Corre-no-Corte-v0.2.apk?raw=true) · [Versão 0.1](./Corre-no-Corte-v0.1.apk?raw=true)
