<p align="center"><img src="docs/images/banner.svg" alt="Monitor quota CF" width="100%"></p>

<p align="center"><a href="README.md">简体中文</a> · <a href="README_EN.md">English</a> · <a href="README_RU.md">Русский</a> · <strong>Italiano</strong> · <a href="README_FR.md">Français</a> · <a href="README_ES.md">Español</a> · <a href="README_AR.md">العربية</a></p>

# Monitor quota CF v1.2

Un’app Android bella, sicura e completamente locale per controllare la quota giornaliera Cloudflare Workers di più account.

<p align="center"><img src="docs/images/v1.2-main-en.png" alt="Schermata principale" width="300"> &nbsp; <img src="docs/images/v1.2-settings-zh.png" alt="Impostazioni" width="300"></p>

## Funzioni

- Più account e barre di avanzamento nella stessa schermata
- Blocco app facoltativo con impronta, volto o credenziale del dispositivo; disattivato per impostazione predefinita
- Italiano, cinese, inglese, russo, francese, spagnolo e arabo; prima selezione dalla lingua di sistema
- Aggiornamento in background facoltativo ogni 15/30 minuti o 1/3/6/12/24 ore; predefinito disattivato
- API Token cifrati con AES-GCM e Android Keystore
- Nessuna pubblicità, analisi, server proprietario o backup cloud

Android può ritardare le attività in background per risparmiare batteria. L’apertura aggiorna subito i dati.

## Installazione e configurazione

Scarica `CF额度监控-v1.2.0.apk` da [Releases](../../releases/latest). Richiede Android 8.0 o successivo.

1. In [Cloudflare Dashboard](https://dash.cloudflare.com), apri **Workers & Pages** e copia l’**Account ID** di 32 caratteri.
2. Apri **Profile → API Tokens → Create Custom Token**.
3. Concedi solo `Account → Account Analytics → Read` e limita la risorsa all’account monitorato.
4. Tocca **＋** nell’app e incolla Account ID e API Token.

Non usare una Global API Key e non pubblicare mai un token.

## Privacy e licenza

Token e cache restano sul dispositivo; le richieste vanno direttamente a `api.cloudflare.com`. Licenza [MIT](LICENSE). Progetto indipendente, non affiliato a Cloudflare, Inc. I dati Analytics possono essere in ritardo e non sono il contatore ufficiale di fatturazione.
