# Corre no Corte — Android 0.8.0

[Baixar o APK atualizado](./Corre-no-Corte-v0.8.apk?raw=true)

Android 8 ou posterior. Arquivo assinado, aproximadamente 23 MB.

Esta atualização adiciona **Modo da câmera** para comparar resoluções e taxas
 de quadros no A05s, A16 e A17:

- Com o monitoramento parado, abra **Modo da câmera**, selecione uma opção e toque **Aplicar**.
- A lista mostra as combinações de resolução/FPS anunciadas para o aplicativo e
  compatíveis com um encoder AVC. Prioriza Full HD, HD e VGA, com modos alternativos quando necessário.
- Teste **1280 × 720** e **1920 × 1080** a 30 ou 60 fps, e **120 fps** quando aparecer.
- A escolha fica salva e tem prioridade sobre a seleção automática de **Priorizar nitidez**.
- Em 120 fps, o obturador é automático; a preferência de nitidez continua salva.
- **Automático** mantém a seleção anterior e suas tentativas de recuperação.
- Se um modo explícito for recusado ao abrir, o app avisa. Escolha outro modo ou
  **Automático**; não há troca silenciosa de resolução/FPS.

A lista varia entre aparelhos. O seletor não força 120 fps se a câmera não liberar
essa combinação para o app. Um modo anunciado ainda depende da aceitação do driver.

Instale por cima da versão anterior, **sem desinstalar**, para manter os dados.
Confira a resolução/FPS abertos na lateral. Grave novos trechos e use
**Parar e finalizar** para gerar os vídeos verticais. Os arquivos já concluídos
mantêm seu processamento original.

Mantém rastreio rápido, revisão do atleta no original, ajustes automáticos de
brilho/contraste/vibração/nitidez e os **2 segundos reais após a última detecção
na área marcada**. O vídeo final continua vertical, 720 × 1280 a 30 fps, com até
**30 segundos finais**: captura30fps→lenta2× com repetição;60fps→2×;120fps→4×.
A revisão antes do recorte aumenta o tempo de processamento e preserva os originais.

Build, assinatura e testes passaram, incluindo **199 verificações da política de modos**.
Ainda não houve teste físico desta atualização: a lista real, a abertura e o
 desempenho contínuo devem ser conferidos no aparelho.

O APK solicita somente CAMERA. Detecção e processamento ocorrem no celular.

SHA-256 da versão 0.8:

```text
097ed93b2d6e5010ae9c64ebf6b45357981b8aaa7f9814dd6da931aadfb7cc3b
```

[Versão 0.7](./Corre-no-Corte-v0.7.apk?raw=true) · [Versão 0.6](./Corre-no-Corte-v0.6.apk?raw=true) · [Versão 0.5](./Corre-no-Corte-v0.5.apk?raw=true) · [Versão 0.4](./Corre-no-Corte-v0.4.apk?raw=true) · [Versão 0.3](./Corre-no-Corte-v0.3.apk?raw=true) · [Versão 0.2](./Corre-no-Corte-v0.2.apk?raw=true) · [Versão 0.1](./Corre-no-Corte-v0.1.apk?raw=true)
