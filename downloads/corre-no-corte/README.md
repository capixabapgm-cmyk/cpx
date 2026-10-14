# Corre no Corte — Android 0.4.0

[Baixar o APK atualizado](./Corre-no-Corte-v0.4.apk?raw=true)

Android 8 ou posterior. Arquivo assinado, aproximadamente 23 MB.

Esta atualização melhora a centralização do atleta no vídeo vertical:

- Suaviza o recorte preservando o centro em movimentos uniformes, mesmo com
  detecções em intervalos irregulares.
- Interpola pausas curtas de detecção, sem preencher a trajetória com caixas antigas.
- Cada trecho começa usando a posição do próprio atleta, sem aproveitar pontos
  de outra pessoa que passou antes.
- Continua acompanhando detecções fora da área marcada durante os 2 segundos finais.

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

Compilação, assinatura, rastreador, buffer, orientação, centralização e duração
foram verificados por testes automatizados. Esta versão ainda precisa de confirmação com a
câmera, codecs e GPU do Galaxy A05s; não houve teste físico desta atualização.

O APK solicita somente CAMERA. Modelo e processamento são locais, sem envio
de imagens a servidores. Os originais são preservados na área privada do app.

SHA-256 da versão 0.4:

```text
6b51a22c6afd9305b03329f03f614b1c1556053dc6bf518b0d016077b0dc85dd
```

[Versão anterior 0.3](./Corre-no-Corte-v0.3.apk?raw=true) · [Versão 0.2](./Corre-no-Corte-v0.2.apk?raw=true) · [Versão 0.1](./Corre-no-Corte-v0.1.apk?raw=true)
