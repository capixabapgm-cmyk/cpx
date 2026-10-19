# Corre no Corte — Android 0.7.0

[Baixar o APK atualizado](./Corre-no-Corte-v0.7.apk?raw=true)

Android 8 ou posterior. Arquivo assinado, aproximadamente 23 MB.

Esta atualização melhora o **acompanhamento de corredores rápidos**:

- Análise da câmera com menos cópias e próxima execução agendada ao terminar a anterior.
- Associação mais tolerante ao deslocamento rápido, com proteções contra troca de atleta.
- Prévia mais ágil, com previsão curta, limitada e suavemente reduzida se os dados envelhecerem.
- Frequência real de atualização mostrada na lateral do app.
- **Revisão do vídeo horizontal original antes do recorte vertical**, com detecções adicionais
  e tempos reais para refinar o enquadramento do atleta selecionado.

A revisão aumenta o tempo para finalizar os vídeos. **Exportar durante espera** começa
 desligado para priorizar o rastreio. Ao terminar, toque **Parar e finalizar** e aguarde
**Revisando atleta no original** e a geração do vertical. Se a revisão não funcionar nos
 decodificadores disponíveis, o app informa e usa o rastreio da captura. Os originais
 horizontais são preservados.

Mantém brilho, contraste, vibração e nitidez definidos uma vez e aplicados automaticamente,
controle de obturador em câmeras compatíveis, **2 segundos reais após a última detecção
na área marcada**, vídeos verticais 9:16 e teto de **30 segundos finais**.
30 fps gera câmera lenta de 2× com quadros repetidos; 60 fps, 2×; 120 fps, 4×.

Instale por cima da versão anterior, **sem desinstalar**, para manter os dados. Grave
novos trechos para conferir a atualização: arquivos já prontos mantêm o processamento
anterior. Abra o app, fixe o celular na horizontal, marque a pista e toque **Iniciar**.
Os vídeos concluídos aparecem em **Meus vídeos** e na galeria.

Build, assinatura e testes de rastreio, revisão de trajetória, YUV, orientação, duração,
exposição e filtros passaram. **Não houve teste físico desta atualização no Galaxy A05s**;
a velocidade efetiva depende do aparelho, da luz e do aquecimento. Cruzamentos e roupas
iguais ainda podem causar perdas, e o recorte respeita os limites da gravação original.

O APK solicita somente CAMERA. Detecção e processamento ocorrem no aparelho.

SHA-256 da versão 0.7:

```text
f96ccf9116e79a687005f74070fcc3f7902fe6acf9f73ef1afcc70d8da1b6e64
```

[Versão 0.6](./Corre-no-Corte-v0.6.apk?raw=true) · [Versão 0.5](./Corre-no-Corte-v0.5.apk?raw=true) · [Versão 0.4](./Corre-no-Corte-v0.4.apk?raw=true) · [Versão 0.3](./Corre-no-Corte-v0.3.apk?raw=true) · [Versão 0.2](./Corre-no-Corte-v0.2.apk?raw=true) · [Versão 0.1](./Corre-no-Corte-v0.1.apk?raw=true)
