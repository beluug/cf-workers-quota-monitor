using System.Globalization;
using System.Windows;

namespace CFQuotaMonitor.Windows.Services;

public sealed class LocalizationService
{
    public static readonly string[] SupportedTags = { "", "zh", "en", "ru", "it", "fr", "es", "ar" };
    public static readonly string[] LanguageNames = { "跟随系统 / System", "中文", "English", "Русский", "Italiano", "Français", "Español", "العربية" };

    private static string[] A(params string[] values) => values;
    private readonly Dictionary<string, string[]> _strings = new()
    {
        ["app_name"] = A("CF 额度监控", "CF Quota Monitor", "Монитор квоты CF", "Monitor quota CF", "Suivi des quotas CF", "Monitor de cuota CF", "مراقب حصة CF"),
        ["app_subtitle"] = A("Cloudflare Workers 免费额度 · Windows", "Cloudflare Workers free quota · Windows", "Бесплатная квота Cloudflare Workers · Windows", "Quota gratuita Cloudflare Workers · Windows", "Quota gratuite Cloudflare Workers · Windows", "Cuota gratuita de Cloudflare Workers · Windows", "حصة Cloudflare Workers المجانية · Windows"),
        ["nav_dashboard"] = A("仪表盘", "Dashboard", "Панель", "Dashboard", "Tableau de bord", "Panel", "لوحة المعلومات"),
        ["nav_accounts"] = A("账户", "Accounts", "Аккаунты", "Account", "Comptes", "Cuentas", "الحسابات"),
        ["nav_transfer"] = A("导入与导出", "Import & export", "Импорт и экспорт", "Importa ed esporta", "Importer et exporter", "Importar y exportar", "الاستيراد والتصدير"),
        ["nav_settings"] = A("设置", "Settings", "Настройки", "Impostazioni", "Paramètres", "Ajustes", "الإعدادات"),
        ["nav_help"] = A("配置教程", "Setup guide", "Руководство", "Guida", "Guide", "Guía", "دليل الإعداد"),
        ["dashboard_title"] = A("Workers 用量仪表盘", "Workers usage dashboard", "Использование Workers", "Utilizzo Workers", "Utilisation de Workers", "Uso de Workers", "استخدام Workers"),
        ["dashboard_desc"] = A("汇总查看所有 Cloudflare 账户的今日请求量", "See today's request usage across all Cloudflare accounts", "Сегодняшние запросы по всем аккаунтам Cloudflare", "Richieste di oggi per tutti gli account Cloudflare", "Requêtes du jour pour tous les comptes Cloudflare", "Solicitudes de hoy de todas las cuentas Cloudflare", "طلبات اليوم لجميع حسابات Cloudflare"),
        ["refresh"] = A("刷新全部", "Refresh all", "Обновить все", "Aggiorna tutto", "Tout actualiser", "Actualizar todo", "تحديث الكل"),
        ["refreshing"] = A("正在刷新…", "Refreshing…", "Обновление…", "Aggiornamento…", "Actualisation…", "Actualizando…", "جارٍ التحديث…"),
        ["add_account"] = A("添加账户", "Add account", "Добавить аккаунт", "Aggiungi account", "Ajouter un compte", "Añadir cuenta", "إضافة حساب"),
        ["today_overview"] = A("今日概览", "TODAY'S OVERVIEW", "СЕГОДНЯ", "RIEPILOGO DI OGGI", "APERÇU DU JOUR", "RESUMEN DE HOY", "نظرة عامة لليوم"),
        ["used_total"] = A("今日已用", "Used today", "Использовано", "Usato oggi", "Utilisé aujourd'hui", "Usado hoy", "المستخدم اليوم"),
        ["remaining_total"] = A("估算剩余", "Estimated remaining", "Осталось", "Residuo stimato", "Restant estimé", "Restante estimado", "المتبقي التقديري"),
        ["account_count"] = A("账户数量", "Accounts", "Аккаунты", "Account", "Comptes", "Cuentas", "الحسابات"),
        ["reset_time"] = A("UTC 00:00 重置", "Resets at UTC 00:00", "Сброс в 00:00 UTC", "Ripristino alle 00:00 UTC", "Réinitialisation à 00:00 UTC", "Reinicio a las 00:00 UTC", "إعادة الضبط 00:00 UTC"),
        ["account_usage"] = A("账户用量", "Account usage", "Использование аккаунтов", "Utilizzo account", "Utilisation des comptes", "Uso de cuentas", "استخدام الحسابات"),
        ["used"] = A("已用", "Used", "Использовано", "Usato", "Utilisé", "Usado", "المستخدم"),
        ["remaining"] = A("剩余", "Remaining", "Осталось", "Rimanente", "Restant", "Restante", "المتبقي"),
        ["daily_limit"] = A("每日额度", "Daily limit", "Дневной лимит", "Limite giornaliero", "Limite quotidienne", "Límite diario", "الحد اليومي"),
        ["updated"] = A("更新于 {0}", "Updated {0}", "Обновлено {0}", "Aggiornato {0}", "Mis à jour {0}", "Actualizado {0}", "تم التحديث {0}"),
        ["never_updated"] = A("尚未获取用量", "No usage data yet", "Данных пока нет", "Nessun dato", "Aucune donnée", "Sin datos todavía", "لا توجد بيانات بعد"),
        ["edit"] = A("编辑", "Edit", "Изменить", "Modifica", "Modifier", "Editar", "تعديل"),
        ["delete"] = A("删除", "Delete", "Удалить", "Elimina", "Supprimer", "Eliminar", "حذف"),
        ["empty_title"] = A("添加第一个 Cloudflare 账户", "Add your first Cloudflare account", "Добавьте первый аккаунт Cloudflare", "Aggiungi il primo account Cloudflare", "Ajoutez votre premier compte Cloudflare", "Añade tu primera cuenta Cloudflare", "أضف أول حساب Cloudflare"),
        ["empty_desc"] = A("只需 Account ID 和只读 API Token，Token 使用 Windows 加密保护。", "Only an Account ID and read-only API Token are needed. Windows encrypts the token.", "Нужны Account ID и API Token только для чтения. Токен шифруется Windows.", "Servono Account ID e API Token di sola lettura. Windows cifra il token.", "Un Account ID et un API Token en lecture seule suffisent. Windows chiffre le jeton.", "Solo necesitas Account ID y un API Token de lectura. Windows cifra el token.", "يلزم Account ID ورمز API للقراءة فقط. يقوم Windows بتشفير الرمز."),
        ["accounts_title"] = A("Cloudflare 账户", "Cloudflare accounts", "Аккаунты Cloudflare", "Account Cloudflare", "Comptes Cloudflare", "Cuentas Cloudflare", "حسابات Cloudflare"),
        ["accounts_desc"] = A("添加、编辑或删除需要监控的账户", "Add, edit, or remove accounts to monitor", "Добавляйте и изменяйте аккаунты", "Aggiungi o modifica gli account", "Ajoutez ou modifiez les comptes", "Añade o edita cuentas", "أضف الحسابات أو عدّلها"),
        ["transfer_title"] = A("跨平台导入与导出", "Cross-platform import & export", "Кроссплатформенный перенос", "Trasferimento multipiattaforma", "Transfert multiplateforme", "Transferencia multiplataforma", "نقل عبر المنصات"),
        ["transfer_desc"] = A("加密备份可在 Android、iOS、Windows 和 macOS 之间使用", "Encrypted backups work across Android, iOS, Windows, and macOS", "Зашифрованные копии работают на Android, iOS, Windows и macOS", "I backup cifrati funzionano su Android, iOS, Windows e macOS", "Les sauvegardes chiffrées fonctionnent sur Android, iOS, Windows et macOS", "Las copias cifradas funcionan en Android, iOS, Windows y macOS", "تعمل النسخ المشفرة على Android وiOS وWindows وmacOS"),
        ["export_title"] = A("导出已保存账户", "Export saved accounts", "Экспорт аккаунтов", "Esporta account", "Exporter les comptes", "Exportar cuentas", "تصدير الحسابات"),
        ["export_desc"] = A("选择账户并设置备份密码，API Token 不会以明文保存。", "Choose accounts and set a backup password. API Tokens are never saved in plain text.", "Выберите аккаунты и пароль. Токены не сохраняются открытым текстом.", "Scegli gli account e una password. I token non sono mai in chiaro.", "Choisissez les comptes et un mot de passe. Les jetons ne sont jamais en clair.", "Elige cuentas y contraseña. Los tokens nunca se guardan sin cifrar.", "اختر الحسابات وكلمة مرور. لا تُحفظ رموز API كنص مكشوف."),
        ["select_all"] = A("全选", "Select all", "Выбрать все", "Seleziona tutto", "Tout sélectionner", "Seleccionar todo", "تحديد الكل"),
        ["backup_password"] = A("备份密码（至少8位）", "Backup password (at least 8 characters)", "Пароль копии (минимум 8 знаков)", "Password backup (almeno 8 caratteri)", "Mot de passe (8 caractères minimum)", "Contraseña (mínimo 8 caracteres)", "كلمة مرور النسخة (8 أحرف على الأقل)"),
        ["confirm_password"] = A("再次输入备份密码", "Confirm backup password", "Повторите пароль", "Conferma password", "Confirmer le mot de passe", "Confirmar contraseña", "تأكيد كلمة المرور"),
        ["export_button"] = A("导出加密文件", "Export encrypted file", "Экспортировать", "Esporta file cifrato", "Exporter le fichier chiffré", "Exportar archivo cifrado", "تصدير ملف مشفر"),
        ["import_title"] = A("导入账户", "Import accounts", "Импорт аккаунтов", "Importa account", "Importer des comptes", "Importar cuentas", "استيراد الحسابات"),
        ["import_desc"] = A("选择 .cfqm 文件，输入导出时设置的密码。", "Choose a .cfqm file and enter its backup password.", "Выберите файл .cfqm и введите пароль.", "Scegli un file .cfqm e inserisci la password.", "Choisissez un fichier .cfqm et saisissez son mot de passe.", "Elige un archivo .cfqm e introduce su contraseña.", "اختر ملف .cfqm وأدخل كلمة مروره."),
        ["duplicate_mode"] = A("重复账户处理", "When an account already exists", "Если аккаунт уже существует", "Se l'account esiste già", "Si le compte existe déjà", "Si la cuenta ya existe", "عند وجود الحساب مسبقًا"),
        ["duplicate_skip"] = A("跳过", "Skip", "Пропустить", "Ignora", "Ignorer", "Omitir", "تخطي"),
        ["duplicate_replace"] = A("替换", "Replace", "Заменить", "Sostituisci", "Remplacer", "Reemplazar", "استبدال"),
        ["duplicate_keep"] = A("同时保留", "Keep both", "Оставить оба", "Mantieni entrambi", "Conserver les deux", "Conservar ambas", "الاحتفاظ بكليهما"),
        ["import_button"] = A("选择文件并导入", "Choose file and import", "Выбрать и импортировать", "Scegli e importa", "Choisir et importer", "Elegir e importar", "اختيار الملف واستيراده"),
        ["settings_title"] = A("Windows 设置", "Windows settings", "Настройки Windows", "Impostazioni Windows", "Paramètres Windows", "Ajustes de Windows", "إعدادات Windows"),
        ["settings_desc"] = A("安全、语言以及后台运行方式", "Security, language, and background behavior", "Безопасность, язык и фоновая работа", "Sicurezza, lingua e attività in background", "Sécurité, langue et fonctionnement en arrière-plan", "Seguridad, idioma y funcionamiento en segundo plano", "الأمان واللغة والعمل في الخلفية"),
        ["security"] = A("安全", "SECURITY", "БЕЗОПАСНОСТЬ", "SICUREZZA", "SÉCURITÉ", "SEGURIDAD", "الأمان"),
        ["app_lock"] = A("应用锁", "App lock", "Блокировка приложения", "Blocco app", "Verrouillage", "Bloqueo de aplicación", "قفل التطبيق"),
        ["app_lock_desc"] = A("重新打开或离开一段时间后，使用 Windows Hello 或应用PIN验证", "Require Windows Hello or app PIN after reopening or being away", "Windows Hello или PIN после повторного открытия", "Richiedi Windows Hello o PIN alla riapertura", "Exiger Windows Hello ou le PIN après réouverture", "Exigir Windows Hello o PIN al volver", "طلب Windows Hello أو PIN عند العودة"),
        ["set_pin"] = A("设置/更改备用PIN", "Set/change fallback PIN", "Задать резервный PIN", "Imposta PIN di riserva", "Définir le PIN de secours", "Configurar PIN alternativo", "تعيين PIN احتياطي"),
        ["lock_after"] = A("离开多久后锁定", "Lock after being away", "Блокировать через", "Blocca dopo", "Verrouiller après", "Bloquear después de", "القفل بعد الغياب"),
        ["immediately"] = A("立即", "Immediately", "Сразу", "Subito", "Immédiatement", "Inmediatamente", "فورًا"),
        ["minutes"] = A("{0} 分钟", "{0} minutes", "{0} мин", "{0} minuti", "{0} minutes", "{0} minutos", "{0} دقائق"),
        ["background"] = A("后台刷新", "BACKGROUND REFRESH", "ФОНОВОЕ ОБНОВЛЕНИЕ", "AGGIORNAMENTO IN BACKGROUND", "ACTUALISATION EN ARRIÈRE-PLAN", "ACTUALIZACIÓN EN SEGUNDO PLANO", "التحديث في الخلفية"),
        ["background_refresh"] = A("允许托盘后台刷新", "Refresh while running in the tray", "Обновлять в области уведомлений", "Aggiorna nell'area di notifica", "Actualiser dans la zone de notification", "Actualizar en la bandeja", "التحديث أثناء العمل في شريط النظام"),
        ["background_desc"] = A("关闭窗口后程序留在系统托盘；电脑休眠或完全退出时不会刷新。", "Keep the app in the system tray after closing. It cannot refresh while asleep or fully exited.", "После закрытия приложение остаётся в трее. Во сне и после выхода не обновляется.", "Dopo la chiusura resta nell'area di notifica. Non aggiorna durante la sospensione.", "Après fermeture, l'app reste dans la zone de notification. Pas d'actualisation en veille.", "Al cerrar, la aplicación queda en la bandeja. No actualiza durante la suspensión.", "يبقى التطبيق في شريط النظام بعد الإغلاق، ولا يُحدّث أثناء السكون."),
        ["refresh_interval"] = A("刷新频率", "Refresh frequency", "Частота обновления", "Frequenza aggiornamento", "Fréquence d'actualisation", "Frecuencia de actualización", "تكرار التحديث"),
        ["start_windows"] = A("登录Windows时自动启动", "Start when I sign in to Windows", "Запускать при входе в Windows", "Avvia all'accesso a Windows", "Démarrer à l'ouverture de session Windows", "Iniciar al acceder a Windows", "البدء عند تسجيل الدخول إلى Windows"),
        ["minimize_tray"] = A("关闭窗口时最小化到托盘", "Minimize to tray when closing", "Сворачивать в трей при закрытии", "Riduci nell'area di notifica", "Réduire dans la zone de notification", "Minimizar a la bandeja", "التصغير إلى شريط النظام عند الإغلاق"),
        ["language"] = A("语言", "LANGUAGE", "ЯЗЫК", "LINGUA", "LANGUE", "IDIOMA", "اللغة"),
        ["app_language"] = A("应用语言", "App language", "Язык приложения", "Lingua dell'app", "Langue de l'application", "Idioma de la aplicación", "لغة التطبيق"),
        ["language_desc"] = A("首次启动跟随Windows，可随时切换", "Follows Windows on first launch; you can change it anytime", "Сначала используется язык Windows", "Al primo avvio segue Windows", "Suit Windows au premier lancement", "Sigue Windows en el primer inicio", "يتبع لغة Windows عند التشغيل الأول"),
        ["privacy"] = A("隐私", "PRIVACY", "КОНФИДЕНЦИАЛЬНОСТЬ", "PRIVACY", "CONFIDENTIALITÉ", "PRIVACIDAD", "الخصوصية"),
        ["privacy_desc"] = A("无广告、无统计SDK。Token由Windows当前用户加密，仅发送至Cloudflare官方API。", "No ads or analytics SDKs. Tokens are encrypted for the current Windows user and sent only to Cloudflare's official API.", "Без рекламы и аналитики. Токены шифруются для текущего пользователя Windows.", "Nessuna pubblicità o analisi. I token sono cifrati per l'utente Windows corrente.", "Sans publicité ni analyse. Les jetons sont chiffrés pour l'utilisateur Windows actuel.", "Sin publicidad ni analíticas. Los tokens se cifran para el usuario actual de Windows.", "بدون إعلانات أو تحليلات. تُشفّر الرموز لمستخدم Windows الحالي."),
        ["help_title"] = A("零基础配置教程", "Beginner setup guide", "Руководство для начинающих", "Guida per principianti", "Guide pour débutants", "Guía para principiantes", "دليل الإعداد للمبتدئين"),
        ["help_desc"] = A("大约3分钟完成，只需创建只读Token", "Ready in about 3 minutes with a read-only token", "Около 3 минут с токеном только для чтения", "Circa 3 minuti con un token di sola lettura", "Environ 3 minutes avec un jeton en lecture seule", "Unos 3 minutos con un token de lectura", "حوالي 3 دقائق باستخدام رمز للقراءة فقط"),
        ["help_steps"] = A("1. 打开 dash.cloudflare.com 并登录。\n\n2. 进入 Workers & Pages，复制32位 Account ID。\n\n3. 打开个人资料 → API Tokens → Create Custom Token。\n\n4. 只授予 Account → Account Analytics → Read，并限制到该账户。\n\n5. Cloudflare只显示一次完整Token，复制到本应用。不要使用Global API Key。", "1. Sign in at dash.cloudflare.com.\n\n2. Open Workers & Pages and copy the 32-character Account ID.\n\n3. Open Profile → API Tokens → Create Custom Token.\n\n4. Grant only Account → Account Analytics → Read and limit it to that account.\n\n5. Cloudflare shows the full token once. Copy it into this app. Never use a Global API Key.", "1. Войдите на dash.cloudflare.com.\n\n2. Откройте Workers & Pages и скопируйте Account ID.\n\n3. Profile → API Tokens → Create Custom Token.\n\n4. Разрешите только Account Analytics → Read.\n\n5. Скопируйте показанный один раз токен. Не используйте Global API Key.", "1. Accedi a dash.cloudflare.com.\n\n2. Apri Workers & Pages e copia l'Account ID.\n\n3. Profile → API Tokens → Create Custom Token.\n\n4. Concedi solo Account Analytics → Read.\n\n5. Copia il token mostrato una sola volta. Non usare Global API Key.", "1. Connectez-vous à dash.cloudflare.com.\n\n2. Ouvrez Workers & Pages et copiez l'Account ID.\n\n3. Profile → API Tokens → Create Custom Token.\n\n4. Accordez uniquement Account Analytics → Read.\n\n5. Copiez le jeton affiché une seule fois. N'utilisez pas de Global API Key.", "1. Inicia sesión en dash.cloudflare.com.\n\n2. Abre Workers & Pages y copia el Account ID.\n\n3. Profile → API Tokens → Create Custom Token.\n\n4. Concede solo Account Analytics → Read.\n\n5. Copia el token que se muestra una sola vez. No uses Global API Key.", "1. سجّل الدخول إلى dash.cloudflare.com.\n\n2. افتح Workers & Pages وانسخ Account ID.\n\n3. Profile ← API Tokens ← Create Custom Token.\n\n4. امنح Account Analytics ← Read فقط.\n\n5. انسخ الرمز الذي يظهر مرة واحدة. لا تستخدم Global API Key."),
        ["save"] = A("保存并查询", "Save and query", "Сохранить", "Salva e interroga", "Enregistrer", "Guardar y consultar", "حفظ واستعلام"),
        ["cancel"] = A("取消", "Cancel", "Отмена", "Annulla", "Annuler", "Cancelar", "إلغاء"),
        ["account_name"] = A("账户备注", "Account label", "Название", "Etichetta account", "Nom du compte", "Nombre de cuenta", "اسم الحساب"),
        ["account_id"] = A("Account ID（32位）", "Account ID (32 characters)", "Account ID (32 символа)", "Account ID (32 caratteri)", "Account ID (32 caractères)", "Account ID (32 caracteres)", "Account ID ‏(32 حرفًا)"),
        ["api_token"] = A("API Token（只读权限）", "API Token (read-only)", "API Token (только чтение)", "API Token (sola lettura)", "API Token (lecture seule)", "API Token (solo lectura)", "API Token ‏(للقراءة فقط)"),
        ["new_token"] = A("新API Token（留空则不修改）", "New API Token (leave blank to keep current)", "Новый токен (оставьте пустым)", "Nuovo token (vuoto per mantenere)", "Nouveau jeton (vide pour conserver)", "Nuevo token (vacío para conservar)", "رمز جديد (اتركه فارغًا للإبقاء)"),
        ["validation_account"] = A("Account ID必须是32位十六进制字符。", "Account ID must contain 32 hexadecimal characters.", "Account ID должен содержать 32 шестнадцатеричных символа.", "L'Account ID deve avere 32 caratteri esadecimali.", "L'Account ID doit contenir 32 caractères hexadécimaux.", "El Account ID debe tener 32 caracteres hexadecimales.", "يجب أن يحتوي Account ID على 32 حرفًا سداسيًا."),
        ["validation_token"] = A("请输入有效的Cloudflare API Token。", "Enter a valid Cloudflare API Token.", "Введите действительный API Token.", "Inserisci un API Token valido.", "Saisissez un API Token valide.", "Introduce un API Token válido.", "أدخل API Token صالحًا."),
        ["validation_limit"] = A("每日额度必须是大于0的数字。", "Daily limit must be a number greater than zero.", "Дневной лимит должен быть больше нуля.", "Il limite deve essere maggiore di zero.", "La limite doit être supérieure à zéro.", "El límite debe ser mayor que cero.", "يجب أن يكون الحد اليومي أكبر من صفر."),
        ["confirm_delete"] = A("确定删除“{0}”及其加密Token吗？", "Delete “{0}” and its encrypted token?", "Удалить «{0}» и зашифрованный токен?", "Eliminare “{0}” e il token cifrato?", "Supprimer « {0} » et son jeton chiffré ?", "¿Eliminar “{0}” y su token cifrado?", "هل تريد حذف «{0}» ورمزه المشفر؟"),
        ["set_pin_title"] = A("设置备用PIN", "Set fallback PIN", "Задать резервный PIN", "Imposta PIN", "Définir le PIN", "Configurar PIN", "تعيين PIN"),
        ["pin_prompt"] = A("输入至少6位PIN", "Enter a PIN with at least 6 digits", "Введите PIN не короче 6 цифр", "Inserisci almeno 6 cifre", "Saisissez au moins 6 chiffres", "Introduce al menos 6 dígitos", "أدخل PIN من 6 أرقام على الأقل"),
        ["pin_confirm"] = A("再次输入PIN", "Confirm PIN", "Повторите PIN", "Conferma PIN", "Confirmer le PIN", "Confirmar PIN", "تأكيد PIN"),
        ["unlock_title"] = A("应用已锁定", "App locked", "Приложение заблокировано", "App bloccata", "Application verrouillée", "Aplicación bloqueada", "التطبيق مقفل"),
        ["unlock_desc"] = A("使用Windows Hello，或输入备用PIN。", "Use Windows Hello or enter your fallback PIN.", "Используйте Windows Hello или резервный PIN.", "Usa Windows Hello o il PIN di riserva.", "Utilisez Windows Hello ou le PIN de secours.", "Usa Windows Hello o el PIN alternativo.", "استخدم Windows Hello أو PIN الاحتياطي."),
        ["unlock"] = A("验证并进入", "Verify and enter", "Проверить и войти", "Verifica ed entra", "Vérifier et entrer", "Verificar y entrar", "التحقق والدخول"),
        ["hello_prompt"] = A("解锁CF额度监控", "Unlock CF Quota Monitor", "Разблокировать CF Quota Monitor", "Sblocca CF Quota Monitor", "Déverrouiller CF Quota Monitor", "Desbloquear CF Quota Monitor", "فتح CF Quota Monitor"),
        ["wrong_pin"] = A("PIN错误。", "Incorrect PIN.", "Неверный PIN.", "PIN errato.", "PIN incorrect.", "PIN incorrecto.", "PIN غير صحيح."),
        ["backup_select_account"] = A("请至少选择一个账户。", "Select at least one account.", "Выберите хотя бы один аккаунт.", "Seleziona almeno un account.", "Sélectionnez au moins un compte.", "Selecciona al menos una cuenta.", "حدد حسابًا واحدًا على الأقل."),
        ["backup_password_short"] = A("备份密码至少需要8位。", "Backup password must be at least 8 characters.", "Пароль должен быть не короче 8 знаков.", "La password deve avere almeno 8 caratteri.", "Le mot de passe doit contenir au moins 8 caractères.", "La contraseña debe tener al menos 8 caracteres.", "يجب ألا تقل كلمة المرور عن 8 أحرف."),
        ["password_mismatch"] = A("两次输入的密码不一致。", "The passwords do not match.", "Пароли не совпадают.", "Le password non corrispondono.", "Les mots de passe ne correspondent pas.", "Las contraseñas no coinciden.", "كلمتا المرور غير متطابقتين."),
        ["backup_invalid"] = A("备份文件无效或已损坏。", "The backup file is invalid or damaged.", "Файл недействителен или повреждён.", "Il backup non è valido o è danneggiato.", "La sauvegarde est invalide ou endommagée.", "La copia no es válida o está dañada.", "ملف النسخة غير صالح أو تالف."),
        ["backup_unsupported"] = A("不支持此备份文件版本。", "This backup version is not supported.", "Эта версия копии не поддерживается.", "Versione backup non supportata.", "Version de sauvegarde non prise en charge.", "Versión de copia no compatible.", "إصدار النسخة غير مدعوم."),
        ["backup_wrong_password"] = A("密码错误，或文件已被修改。", "Wrong password, or the file was modified.", "Неверный пароль или файл изменён.", "Password errata o file modificato.", "Mot de passe incorrect ou fichier modifié.", "Contraseña incorrecta o archivo modificado.", "كلمة المرور خاطئة أو تم تعديل الملف."),
        ["token_unreadable"] = A("有账户的加密Token无法读取，请重新编辑该账户。", "An encrypted token cannot be read. Edit that account and enter it again.", "Зашифрованный токен не читается. Введите его снова.", "Un token non è leggibile. Inseriscilo di nuovo.", "Un jeton est illisible. Saisissez-le à nouveau.", "Un token no se puede leer. Introdúcelo de nuevo.", "تعذرت قراءة رمز مشفر. أدخله من جديد."),
        ["export_success"] = A("加密备份已导出。请妥善保存密码。", "Encrypted backup exported. Keep its password safe.", "Зашифрованная копия экспортирована. Сохраните пароль.", "Backup cifrato esportato. Conserva la password.", "Sauvegarde chiffrée exportée. Conservez son mot de passe.", "Copia cifrada exportada. Guarda la contraseña.", "تم تصدير النسخة المشفرة. احتفظ بكلمة المرور."),
        ["import_confirm"] = A("文件包含 {0} 个账户：\n\n{1}\n\n确定导入吗？", "The file contains {0} accounts:\n\n{1}\n\nImport them?", "Файл содержит аккаунтов: {0}\n\n{1}\n\nИмпортировать?", "Il file contiene {0} account:\n\n{1}\n\nImportarli?", "Le fichier contient {0} comptes :\n\n{1}\n\nLes importer ?", "El archivo contiene {0} cuentas:\n\n{1}\n\n¿Importarlas?", "يحتوي الملف على {0} حسابات:\n\n{1}\n\nهل تريد استيرادها؟"),
        ["import_success"] = A("已导入 {0} 个账户，跳过 {1} 个。", "Imported {0} accounts; skipped {1}.", "Импортировано: {0}; пропущено: {1}.", "Importati {0}; ignorati {1}.", "{0} comptes importés ; {1} ignorés.", "Importadas {0}; omitidas {1}.", "تم استيراد {0} وتخطي {1}."),
        ["error_network"] = A("网络连接失败，请检查网络后重试。", "Network connection failed. Check your connection and try again.", "Ошибка сети. Проверьте подключение.", "Connessione non riuscita.", "Échec de la connexion réseau.", "Error de conexión de red.", "فشل اتصال الشبكة."),
        ["error_token_invalid"] = A("Token无效、已过期或没有访问权限。", "The token is invalid, expired, or lacks access.", "Токен недействителен или не имеет доступа.", "Token non valido o senza accesso.", "Jeton invalide ou sans accès.", "Token no válido o sin acceso.", "الرمز غير صالح أو لا يملك صلاحية."),
        ["error_permission"] = A("权限不足，请授予Account Analytics Read。", "Permission denied. Grant Account Analytics Read.", "Недостаточно прав: нужен Account Analytics Read.", "Autorizza Account Analytics Read.", "Accordez Account Analytics Read.", "Concede Account Analytics Read.", "امنح صلاحية Account Analytics Read."),
        ["error_rate_limit"] = A("Cloudflare查询过于频繁，请稍后重试。", "Too many Cloudflare queries. Try again later.", "Слишком много запросов. Повторите позже.", "Troppe richieste. Riprova più tardi.", "Trop de requêtes. Réessayez plus tard.", "Demasiadas consultas. Inténtalo después.", "طلبات كثيرة جدًا. حاول لاحقًا."),
        ["error_cloudflare"] = A("Cloudflare服务暂时不可用。", "Cloudflare is temporarily unavailable.", "Cloudflare временно недоступен.", "Cloudflare non è disponibile.", "Cloudflare est temporairement indisponible.", "Cloudflare no está disponible temporalmente.", "Cloudflare غير متاح مؤقتًا."),
        ["error_request"] = A("Cloudflare请求失败。", "Cloudflare request failed.", "Запрос Cloudflare не выполнен.", "Richiesta Cloudflare non riuscita.", "Échec de la requête Cloudflare.", "Falló la solicitud a Cloudflare.", "فشل طلب Cloudflare."),
        ["error_query"] = A("Cloudflare用量查询失败。", "Cloudflare usage query failed.", "Запрос статистики не выполнен.", "Query di utilizzo non riuscita.", "Échec de la requête d'utilisation.", "Falló la consulta de uso.", "فشل استعلام الاستخدام."),
        ["error_account_access"] = A("Token无法访问此Account ID。", "The token cannot access this Account ID.", "Токен не имеет доступа к Account ID.", "Il token non accede a questo Account ID.", "Le jeton n'accède pas à cet Account ID.", "El token no accede a este Account ID.", "لا يمكن للرمز الوصول إلى Account ID."),
        ["error_response"] = A("Cloudflare返回了无法识别的数据。", "Cloudflare returned an unexpected response.", "Cloudflare вернул неожиданный ответ.", "Risposta Cloudflare imprevista.", "Réponse Cloudflare inattendue.", "Respuesta inesperada de Cloudflare.", "أعاد Cloudflare استجابة غير متوقعة."),
        ["show"] = A("显示", "Show", "Показать", "Mostra", "Afficher", "Mostrar", "إظهار"),
        ["exit"] = A("退出", "Exit", "Выход", "Esci", "Quitter", "Salir", "خروج"),
        ["app_running_tray"] = A("CF额度监控仍在系统托盘运行。", "CF Quota Monitor is still running in the system tray.", "CF Quota Monitor продолжает работу в трее.", "CF Quota Monitor è ancora attivo nell'area di notifica.", "CF Quota Monitor fonctionne toujours dans la zone de notification.", "CF Quota Monitor sigue en la bandeja.", "لا يزال CF Quota Monitor يعمل في شريط النظام."),
        ["ok"] = A("确定", "OK", "ОК", "OK", "OK", "Aceptar", "موافق")
    };

    public string CurrentTag { get; private set; }
    public bool IsRightToLeft => CurrentTag == "ar";

    public LocalizationService(string configuredTag)
    {
        CurrentTag = Normalize(string.IsNullOrWhiteSpace(configuredTag)
            ? CultureInfo.CurrentUICulture.TwoLetterISOLanguageName : configuredTag);
    }

    public void SetLanguage(string tag) => CurrentTag = Normalize(string.IsNullOrWhiteSpace(tag)
        ? CultureInfo.CurrentUICulture.TwoLetterISOLanguageName : tag);

    public string T(string key, params object[] args)
    {
        if (!_strings.TryGetValue(key, out var values)) return key;
        var index = CurrentTag switch { "zh" => 0, "ru" => 2, "it" => 3, "fr" => 4, "es" => 5, "ar" => 6, _ => 1 };
        var text = values[index];
        return args.Length == 0 ? text : string.Format(CultureInfo.CurrentCulture, text, args);
    }

    public System.Windows.FlowDirection FlowDirection => IsRightToLeft
        ? System.Windows.FlowDirection.RightToLeft : System.Windows.FlowDirection.LeftToRight;

    private static string Normalize(string tag) => tag.ToLowerInvariant() switch
    {
        "zh" => "zh", "ru" => "ru", "it" => "it", "fr" => "fr", "es" => "es", "ar" => "ar", _ => "en"
    };
}
