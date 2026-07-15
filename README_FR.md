<p align="center"><img src="docs/images/banner.svg" alt="Suivi du quota CF" width="100%"></p>

<p align="center"><a href="README.md">简体中文</a> · <a href="README_EN.md">English</a> · <a href="README_RU.md">Русский</a> · <a href="README_IT.md">Italiano</a> · <strong>Français</strong> · <a href="README_ES.md">Español</a> · <a href="README_AR.md">العربية</a></p>

# Suivi du quota CF v1.2

Une belle application Android, sûre et entièrement locale, pour suivre le quota quotidien Cloudflare Workers de plusieurs comptes.

<p align="center"><img src="docs/images/v1.2-main-en.png" alt="Écran principal" width="300"> &nbsp; <img src="docs/images/v1.2-settings-zh.png" alt="Paramètres" width="300"></p>

## Fonctionnalités

- Plusieurs comptes et barres de progression sur le même écran
- Verrouillage facultatif par empreinte, visage ou code de l’appareil ; désactivé par défaut
- Français, chinois, anglais, russe, italien, espagnol et arabe ; langue système au premier lancement
- Actualisation facultative en arrière-plan : 15/30 minutes ou 1/3/6/12/24 heures ; désactivée par défaut
- API Token chiffrés par AES-GCM et Android Keystore
- Sans publicité, analytique, serveur propriétaire ni sauvegarde cloud

Android peut retarder les tâches en arrière-plan pour économiser la batterie. L’ouverture actualise toujours immédiatement.

## Installation et configuration

Téléchargez `CF额度监控-v1.2.0.apk` depuis [Releases](../../releases/latest). Android 8.0 ou plus récent est requis.

1. Dans [Cloudflare Dashboard](https://dash.cloudflare.com), ouvrez **Workers & Pages** et copiez l’**Account ID** de 32 caractères.
2. Ouvrez **Profile → API Tokens → Create Custom Token**.
3. Accordez uniquement `Account → Account Analytics → Read` et limitez la ressource au compte suivi.
4. Touchez **＋** dans l’application et collez l’Account ID et l’API Token.

N’utilisez jamais de Global API Key et ne publiez pas de jeton.

## Confidentialité et licence

Les jetons et le cache restent sur l’appareil ; les requêtes vont directement à `api.cloudflare.com`. Licence [MIT](LICENSE). Projet indépendant non affilié à Cloudflare, Inc. Analytics peut être en retard et n’est pas le compteur officiel de facturation.
