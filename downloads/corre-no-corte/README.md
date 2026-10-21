# Corre no Corte — Android 0.11.0

[Baixar o APK atualizado](./Corre-no-Corte-v0.11.apk?raw=true)

Android 8 ou posterior. APK assinado, aproximadamente 23 MB.
Instale por cima da versão anterior, **sem desinstalar**, para manter os dados.

Esta versão adiciona **Modo vertical** e mantém **Iniciar / Parar e finalizar**
fixo no topo da área de controles, fora da rolagem, nos dois modos.

1. Com o monitoramento parado, toque **Modo vertical**. A tela passa para retrato;
   a preferência fica salva. O operador enquadra e acompanha pelo tripé.
2. Toque **Iniciar** para armar a detecção. Uma pessoa em movimento começa a gravação.
3. Depois de começar, a gravação continua enquanto houver pessoa visível,
   **mesmo parada**. Sem pessoa por **2 segundos**, encerra e espera outra passagem.
4. Use **Parar e finalizar** para encerrar manualmente e processar a fila.
   Trechos longos são divididos em arquivos de até **30 segundos finais**.

O modo vertical preserva o quadro inteiro, sem recentralizar o atleta ou fazer
segunda análise da trajetória. A saída pode chegar a **1080 × 1920** quando
captura e encoder permitirem; HD usa **720 × 1280**. Fontes menores mantêm seu
limite de detalhe e proporções diferentes recebem barras. Não recupera corpo
fora da cena. O horizontal conserva recorte móvel e revisão do original.

A detecção de movimento compensa translação e exposição da cena para reduzir
disparos pelo giro do tripé. Fundo liso, parallax, luz local e oclusões ainda
podem impedir ou confundir a detecção; valide nas condições reais de uso.

Mantém câmera lenta, filtros, galeria externa e prioridade para retomar a captura.
Exportar durante espera continua opcional e depende dos recursos anunciados
pelo aparelho; com um encoder, a fila aguarda parar a captura.

Build, assinatura e testes automatizados passaram: 124 sequências sintéticas de
movimento, 144 verificações de gravação e 4020 de enquadramento, além das
regressões anteriores. **Não houve teste físico desta versão nos Galaxy.**
O APK solicita somente CAMERA e processa as imagens no celular.

SHA-256 da versão 0.11:

```text
6f9576e28eeff3b41776507cbfbc55acfd4d5f7ad9242c05e0364fb65bc59a8a
```

[Versão 0.10](./Corre-no-Corte-v0.10.apk?raw=true) · [Versão 0.9](./Corre-no-Corte-v0.9.apk?raw=true) · [Versão 0.8](./Corre-no-Corte-v0.8.apk?raw=true) · [Versão 0.7](./Corre-no-Corte-v0.7.apk?raw=true) · [Versão 0.6](./Corre-no-Corte-v0.6.apk?raw=true) · [Versão 0.5](./Corre-no-Corte-v0.5.apk?raw=true) · [Versão 0.4](./Corre-no-Corte-v0.4.apk?raw=true) · [Versão 0.3](./Corre-no-Corte-v0.3.apk?raw=true) · [Versão 0.2](./Corre-no-Corte-v0.2.apk?raw=true) · [Versão 0.1](./Corre-no-Corte-v0.1.apk?raw=true)
