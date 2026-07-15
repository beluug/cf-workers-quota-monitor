<p align="center"><img src="docs/images/banner.svg" alt="Monitor de cuota CF" width="100%"></p>

<p align="center"><a href="README.md">简体中文</a> · <a href="README_EN.md">English</a> · <a href="README_RU.md">Русский</a> · <a href="README_IT.md">Italiano</a> · <a href="README_FR.md">Français</a> · <strong>Español</strong> · <a href="README_AR.md">العربية</a></p>

# Monitor de cuota CF v1.2

Una aplicación Android atractiva, segura y totalmente local para controlar la cuota diaria de Cloudflare Workers en varias cuentas.

<p align="center"><img src="docs/images/v1.2-main-en.png" alt="Pantalla principal" width="300"> &nbsp; <img src="docs/images/v1.2-settings-zh.png" alt="Ajustes" width="300"></p>

## Funciones

- Varias cuentas y barras de progreso en una sola pantalla
- Bloqueo opcional con huella, rostro o credencial del dispositivo; desactivado por defecto
- Español, chino, inglés, ruso, italiano, francés y árabe; selección inicial según el sistema
- Actualización opcional en segundo plano cada 15/30 minutos o 1/3/6/12/24 horas; desactivada por defecto
- API Token cifrados con AES-GCM y Android Keystore
- Sin anuncios, analítica, servidor propio ni copias en la nube

Android puede retrasar el trabajo en segundo plano para ahorrar batería. Al abrir la aplicación se actualiza inmediatamente.

## Instalación y configuración

Descarga `CF额度监控-v1.2.0.apk` desde [Releases](../../releases/latest). Requiere Android 8.0 o posterior.

1. En [Cloudflare Dashboard](https://dash.cloudflare.com), abre **Workers & Pages** y copia el **Account ID** de 32 caracteres.
2. Abre **Profile → API Tokens → Create Custom Token**.
3. Concede solo `Account → Account Analytics → Read` y limita el recurso a la cuenta supervisada.
4. Pulsa **＋** en la aplicación y pega el Account ID y el API Token.

No uses una Global API Key ni publiques ningún token.

## Privacidad y licencia

Los tokens y la caché permanecen en el dispositivo; las solicitudes van directamente a `api.cloudflare.com`. Licencia [MIT](LICENSE). Proyecto independiente no afiliado a Cloudflare, Inc. Analytics puede retrasarse y no es el contador oficial de facturación.
